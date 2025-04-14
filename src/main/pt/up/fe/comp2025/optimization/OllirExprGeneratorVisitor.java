package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
    }


    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);

//        addVisit(PROGRAM, this::visitProgram);
//        addVisit(CLASS_DECL, this::visitClass);
//        addVisit(METHOD_DECL, this::visitMethodDecl);
//        addVisit(PARAM, this::visitParam);
//        addVisit(RETURN_STMT, this::visitReturn);
//        addVisit(ASSIGN_STMT, this::visitAssignStmt);
//        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = types.getExprType(node);
        String resOllirType = ollirTypes.toOllirType(resType);
        String code = ollirTypes.nextTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = types.getExprType(node);
        computation.append(node.get("op")).append(ollirTypes.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }


    /// NOVAS FUNÇOES
    private OllirExprResult visitArrayCreation(JmmNode node, Void unused) {
        var sizeExpr = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder();
        computation.append(sizeExpr.getComputation());

        String newArrTemp = ollirTypes.nextTemp();
        String newArrType = ".array.i32";

        computation.append(newArrTemp).append(newArrType)
                .append(" :=.array.i32 new(array, ")
                .append(sizeExpr.getCode()).append(");\n");

        return new OllirExprResult(newArrTemp + newArrType, computation);
    }


    private String visitArrayAccess(JmmNode node, Void unused) {
        // Example: tmp0.i32 :=.i32 arr[ index ].i32
        var arrNode = node.getChild(0);
        var idxNode = node.getChild(1);

        var arrExpr = this.visit(arrNode);
        var idxExpr = this.visit(idxNode);

        StringBuilder code = new StringBuilder();
        code.append(arrExpr.getComputation()).append(idxExpr.getComputation());

        String tmpVar = ollirTypes.nextTemp();
        String arrType = ".i32"; // Adjust if needed (e.g., .array.i32 or .bool)

        code.append(tmpVar).append(arrType).append(" :=.i32 ")
                .append(arrExpr.getCode()).append("[")
                .append(idxExpr.getCode()).append("]").append(arrType)
                .append(";\n");

        return code.toString();
    }

    private String visitArrayAssign(JmmNode node, Void unused) {
        // Example: arr[ index ] :=.i32 value
        var arrNode = node.getChild(0);
        var idxNode = node.getChild(1);
        var valNode = node.getChild(2);

        var arrExpr = this.visit(arrNode);
        var idxExpr = this.visit(idxNode);
        var valExpr = this.visit(valNode);

        StringBuilder code = new StringBuilder();
        code.append(arrExpr.getComputation())
                .append(idxExpr.getComputation())
                .append(valExpr.getComputation());

        code.append(arrExpr.getCode()).append("[")
                .append(idxExpr.getCode()).append("]").append(".i32")
                .append(" :=.i32 ")
                .append(valExpr.getCode()).append(";\n");

        return code.toString();
    }


    private String visitIfStmt(JmmNode node, Void unused) {
        // Example if structure with labels
        String labelElse = "LabelElse" + ollirTypes.nextTemp("");
        String labelEnd = "LabelEnd" + ollirTypes.nextTemp("");

        var condExpr = this.visit(node.getChild(0));
        var thenStmt = node.getChild(1);
        var elseStmt = node.getNumChildren() > 2 ? node.getChild(2) : null;

        StringBuilder code = new StringBuilder();
        code.append(condExpr.getComputation());

        code.append("if (").append(condExpr.getCode())
                .append(") goto ").append(labelElse).append(";\n");

        // Then block
        code.append(visit(thenStmt));

        // Goto end
        code.append("goto ").append(labelEnd).append(";\n");

        // Else block
        code.append(labelElse).append(":\n");
        if (elseStmt != null) {
            code.append(visit(elseStmt));
        }

        // End label
        code.append(labelEnd).append(":\n");

        return code.toString();
    }


    private String visitWhileStmt(JmmNode node, Void unused) {
        // Example while structure
        String labelCond = "LabelCond" + ollirTypes.nextTemp("");
        String labelBody = "LabelBody" + ollirTypes.nextTemp("");

        var condExpr = this.visit(node.getChild(0));
        var bodyStmt = node.getChild(1);

        StringBuilder code = new StringBuilder();
        code.append(labelCond).append(":\n")
                .append(condExpr.getComputation())
                .append("if (").append(condExpr.getCode())
                .append(") goto ").append(labelBody).append(";\n")
                .append("goto LabelEnd").append(labelCond).append(";\n")
                .append(labelBody).append(":\n")
                .append(visit(bodyStmt))
                .append("goto ").append(labelCond).append(";\n")
                .append("LabelEnd").append(labelCond).append(":\n");

        return code.toString();
    }

    private String visitMethodCall(JmmNode node, Void unused) {
        // Optionally handle varargs here by converting them to an array
        // Example: tmp1.i32 :=.i32 invokevirtual(this, "foo", argTemp.array.i32).i32;
        String methodName = node.get("methodName");
        var argNodes = node.getChildren();
        StringBuilder code = new StringBuilder();

        // Generate code for arguments
        for (var argNode : argNodes) {
            var argExpr = this.visit(argNode);
            code.append(argExpr.getComputation());
        }
        String tmpVar = ollirTypes.nextTemp();
        code.append(tmpVar).append(".i32 :=.i32 invokevirtual(")
                .append("this, \"").append(methodName).append("\",");

        // Example showing just one array arg or multiple
        // Adapt as needed
        code.append(" tmpArr.array.i32").append(").i32;\n");

        return code.toString();
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
