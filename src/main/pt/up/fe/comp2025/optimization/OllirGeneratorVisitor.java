package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);

        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(ASSIGN_ARRAY_STMT, this::visitArrayAssign);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(METHOD_CALL, this::visitMethodCall);

//        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        var varCode = left.get("name") + typeString;


        code.append(varCode);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // TODO: Hardcoded for int type, needs to be expanded
        Type retType = TypeUtils.newIntType();


        StringBuilder code = new StringBuilder();


        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;


        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = node.getBoolean("isPublic", false);

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        // TODO: Hardcoded for a single parameter, needs to be expanded
        var paramsCode = visit(node.getChild(1));
        code.append("(" + paramsCode + ")");

        // type
        // TODO: Hardcoded for int, needs to be expanded
        var retType = ".i32";
        code.append(retType);
        code.append(L_BRACKET);


        // rest of its children stmts
        var stmtsCode = node.getChildren(STMT).stream()
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));

        code.append(stmtsCode);
        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());
        
        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }



    // ----------------------- NOVAS FUNCOES ------------------------------ //
    private String visitArrayAccess(JmmNode node, Void unused) {
        var arrNode = node.getChild(0);
        var idxNode = node.getChild(1);

        var arrExpr = exprVisitor.visit(arrNode);
        var idxExpr = exprVisitor.visit(idxNode);

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
        var arrNode = node.getChild(0);
        var idxNode = node.getChild(1);
        var valNode = node.getChild(2);

        var arrExpr = exprVisitor.visit(arrNode);
        var idxExpr = exprVisitor.visit(idxNode);
        var valExpr = exprVisitor.visit(valNode);

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
        String labelElse = "LabelElse" + ollirTypes.nextTemp("");
        String labelEnd = "LabelEnd" + ollirTypes.nextTemp("");

        var condExpr = exprVisitor.visit(node.getChild(0));
        var thenStmt = node.getChild(1);
        var elseStmt = node.getNumChildren() > 2 ? node.getChild(2) : null;

        StringBuilder code = new StringBuilder();
        code.append(condExpr.getComputation());

        code.append("if (").append(condExpr.getCode())
                .append(") goto ").append(labelElse).append(";\n");

        code.append(visit(thenStmt));

        code.append("goto ").append(labelEnd).append(";\n");

        code.append(labelElse).append(":\n");
        if (elseStmt != null) {
            code.append(visit(elseStmt));
        }

        code.append(labelEnd).append(":\n");

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        String labelCond = "LabelCond" + ollirTypes.nextTemp("");
        String labelBody = "LabelBody" + ollirTypes.nextTemp("");

        var condExpr = exprVisitor.visit(node.getChild(0));
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
        String methodName = node.get("methodName");
        var argNodes = node.getChildren();
        StringBuilder code = new StringBuilder();

        for (var argNode : argNodes) {
            var argExpr = exprVisitor.visit(argNode);
            code.append(argExpr.getComputation());
        }

        String tmpVar = ollirTypes.nextTemp();
        code.append(tmpVar).append(".i32 :=.i32 invokevirtual(")
                .append("this, \"").append(methodName).append("\",");

        code.append(" tmpArr.array.i32").append(").i32;\n");

        return code.toString();
    }


}
