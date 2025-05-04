package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final TypeUtils types;
    private final OptUtils ollirTypes;
    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table      = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        setDefaultValue(() -> OllirExprResult.EMPTY);
        buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Int", this::visitInteger);

        addVisit("Id", this::visitVarRef);

        // Binary operators
        addVisit("AddSub", this::visitBinExpr);
        addVisit("MulDiv", this::visitBinExpr);
        addVisit("Compare", this::visitBinExpr);
        addVisit("And",    this::visitBinExpr);
        addVisit("Or",     this::visitBinExpr);

        addVisit("Length", this::visitLength);
        addVisit("Parenthesis", this::visitParenthesis);


        addVisit("NewArray", this::visitNewArray);
        addVisit("ArrayLiteral", this::visitArrayLiteral);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("NewObject", this::visitNewObject);
        addVisit("MethodCall", this::visitMethodCall);

        // Boolean literal
        addVisit("Boolean", this::visitBoolean);
        addVisit("True",    this::visitBoolean);
        addVisit("False",   this::visitBoolean);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        String className = node.get("name");
        String temp      = ollirTypes.nextTemp();
        String fullTemp  = temp + "." + className;
        StringBuilder code = new StringBuilder()
                .append(fullTemp)
                .append(" :=.").append(className)
                .append(" new(").append(className).append(").")
                .append(className).append(";\n")
                .append("invokespecial(")
                .append(fullTemp)
                .append(", \"<init>\").V;\n");
        return new OllirExprResult(fullTemp, code.toString());
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        var callerRes = visit(node.getChild(0));
        StringBuilder code = new StringBuilder(callerRes.getComputation());
        List<String> argCodes = new ArrayList<>();
        for (int i = 1; i < node.getNumChildren(); i++) {
            var argRes = visit(node.getChild(i));
            code.append(argRes.getComputation());
            argCodes.add(argRes.getCode());
        }
        String methodName = node.get("methodName");
        Type   retType    = types.getExprType(node);
        String ollirType  = ollirTypes.toOllirType(retType).substring(1);
        String tmp        = ollirTypes.nextTemp();
        String fullTmp    = tmp + "." + ollirType;

        code.append(fullTmp)
                .append(" :=.").append(ollirType)
                .append(" invokevirtual(")
                .append(callerRes.getCode())
                .append(", \"").append(methodName).append("\"");

        for (var ac : argCodes) {
            code.append(", ").append(ac);
        }
        code.append(").").append(ollirType).append(";\n");
        return new OllirExprResult(fullTmp, code.toString());
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        var arrayRes = visit(node.getChild(0));
        var idxRes   = visit(node.getChild(1));

        String temp = ollirTypes.nextTemp();
        StringBuilder code = new StringBuilder()
                .append(arrayRes.getComputation())
                .append(idxRes.getComputation())
                .append(temp).append(".i32 :=.i32 ")
                .append(arrayRes.getCode()).append("[")
                .append(idxRes.getCode()).append("].i32;\n");

        return new OllirExprResult(temp + ".i32", code.toString());
    }

    private OllirExprResult visitLength(JmmNode node, Void unused) {
        var arrayExpr = visit(node.getChild(0));
        StringBuilder code = new StringBuilder();
        code.append(arrayExpr.getComputation());
        String temp = ollirTypes.nextTemp();
        code.append(temp).append(".i32 :=.i32 arraylength(")
                .append(arrayExpr.getCode()).append(").i32;\n");
        return new OllirExprResult(temp + ".i32", code.toString());
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String value = node.get("name");
        String code  = value + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        JmmNode leftNode  = node.getChild(0);
        if (leftNode.getKind().equals("Parenthesis")) {
            leftNode = leftNode.getChild(0);
        }
        JmmNode rightNode = node.getChild(1);
        if (rightNode.getKind().equals("Parenthesis")) {
            rightNode = rightNode.getChild(0);
        }

        var lhs = visit(leftNode);
        var rhs = visit(rightNode);

        String kind = node.getKind();
        if (kind.equals("And")) {
            String temp    = ollirTypes.nextTemp("andTmp");
            String thenLbl = "then"   + ollirTypes.nextTemp("");
            String endLbl  = "endif"  + ollirTypes.nextTemp("");

            StringBuilder code = new StringBuilder()
                    .append(lhs.getComputation())
                    .append("if (").append(lhs.getCode()).append(") goto ").append(thenLbl).append(";\n")
                    .append(temp).append(".bool :=.bool 0.bool;\n")
                    .append("goto ").append(endLbl).append(";\n")
                    .append(thenLbl).append(":\n")
                    .append(rhs.getComputation())
                    .append(temp).append(".bool :=.bool ").append(rhs.getCode()).append(";\n")
                    .append(endLbl).append(":\n");

            return new OllirExprResult(temp + ".bool", code.toString());
        }
        if (kind.equals("Or")) {
            String temp    = ollirTypes.nextTemp("orTmp");
            String thenLbl = "then"  + ollirTypes.nextTemp("");
            String endLbl  = "endif" + ollirTypes.nextTemp("");

            StringBuilder code = new StringBuilder()
                    .append(lhs.getComputation())
                    .append("if (").append(lhs.getCode()).append(") goto ").append(endLbl).append(";\n")
                    .append(rhs.getComputation())
                    .append("if (").append(rhs.getCode()).append(") goto ").append(thenLbl).append(";\n")
                    .append(temp).append(".bool :=.bool 0.bool;\n")
                    .append("goto ").append(endLbl).append(";\n")
                    .append(thenLbl).append(":\n")
                    .append(temp).append(".bool :=.bool 1.bool;\n")
                    .append(endLbl).append(":\n");

            return new OllirExprResult(temp + ".bool", code.toString());
        }

        String rawOp;
        switch (kind) {
            case "AddSub" -> rawOp = node.get("op");    // "+" or "-"
            case "MulDiv" -> rawOp = node.get("op");    // "*" or "/"
            case "Compare"-> rawOp = node.get("op");    // "<", ">", "==", etc.
            default        -> throw new RuntimeException("Unknown binop " + kind);
        }

        Type resultType    = types.getExprType(node);
        String ollirType   = ollirTypes.toOllirType(resultType).substring(1);
        String tmp         = ollirTypes.nextTemp();
        String instr       = tmp + "." + ollirType
                + " :=." + ollirType + " "
                + lhs.getCode() + " " + rawOp + "." + ollirType + " "
                + rhs.getCode() + END_STMT;

        String computation = lhs.getComputation() + rhs.getComputation() + instr;
        return new OllirExprResult(tmp + "." + ollirType, computation);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        String lit = node.get("name").equals("true") ? "1" : "0";
        String code = lit + ".bool";
        return new OllirExprResult(code);
     }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String name   = node.get("name");
        String suffix = ollirTypes.toOllirType(types.getExprType(node));

        var methodNameOpt = node.getAncestor("MethodDecl").map(n -> n.get("name"));
        String methodName = methodNameOpt.orElse("");

        boolean isParam = table.getParameters(methodName)
                .stream().anyMatch(p -> p.getName().equals(name));
        boolean isLocal = table.getLocalVariables(methodName)
                .stream().anyMatch(l -> l.getName().equals(name));
        boolean isField = !isParam && !isLocal
                && table.getFields().stream()
                .anyMatch(f -> f.getName().equals(name));

        if (isField) {
            String tmp = ollirTypes.nextTemp();
            StringBuilder code = new StringBuilder();
            code.append(tmp).append(suffix)
                    .append(" :=").append(suffix)
                    .append(" getfield(this, ")
                    .append(name).append(suffix).append(").")
                    .append(suffix.substring(1))
                    .append(END_STMT);
            return new OllirExprResult(tmp + suffix, code.toString());
        }

        return new OllirExprResult(name + suffix);
    }

    private OllirExprResult visitArrayLiteral(JmmNode node, Void unused) {
        int size = node.getNumChildren();
        String temp = ollirTypes.nextTemp();
        StringBuilder code = new StringBuilder();
        code.append(temp).append(".array.i32 :=.array.i32 new(array, ")
                .append(size).append(".i32)").append(".array.i32;\n");
        for (int i = 0; i < size; i++) {
            var elemResult = visit(node.getChild(i));
            code.append(elemResult.getComputation());
            code.append(temp).append("[").append(i).append("].i32 :=.i32 ")
                    .append(elemResult.getCode()).append(";\n");
        }
        return new OllirExprResult(temp + ".array.i32", code.toString());
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        var sizeExpr = visit(node.getChild(0));
        Type newArrayType = new Type("int", true);
        String suffix = ollirTypes.toOllirType(newArrayType);
        String temp = ollirTypes.nextTemp();
        StringBuilder code = new StringBuilder();
        code.append(sizeExpr.getComputation());
        code.append(temp).append(suffix)
                .append(" :=").append(suffix)
                .append(" new(array, ").append(sizeExpr.getCode())
                .append(").").append(suffix.substring(1))
                .append(";\n");
        return new OllirExprResult(temp + suffix, code.toString());
    }


    private OllirExprResult visitParenthesis(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return OllirExprResult.EMPTY;
    }
}