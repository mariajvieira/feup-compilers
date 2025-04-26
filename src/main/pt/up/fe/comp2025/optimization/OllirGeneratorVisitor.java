package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;
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
        addVisit("MethodDecl", this::visitMethodDecl);
        addVisit("ImportDecl", this::visitImportDecl);
        setDefaultVisit(this::defaultVisit);
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit("ReturnStmt", this::visitReturn);
        addVisit("RetStmt", this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        var qualifiedNameNode = node.getChildren().get(0);

        var qualifiedName = "";
        if (qualifiedNameNode.getKind().equals("qualifiedName")) {
            qualifiedName = qualifiedNameNode.getChildren().stream()
                    .map(child -> child.get("name"))
                    .collect(Collectors.joining("."));
        } else {
            qualifiedName = qualifiedNameNode.get("name");
        }

        qualifiedName = qualifiedName.replaceAll("\\[|\\]", "");

        return "import " + qualifiedName + ";\n";
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // Get the target variable
        var left = node.getChild(0);
        String varName = left.get("name");
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);

        // Process the value expression
        var value = node.getChild(1);
        var rhs = exprVisitor.visit(value);

        // Append any computation from the expression first
        code.append(rhs.getComputation());

        // Generate the assignment statement
        code.append(varName).append(typeString);
        code.append(SPACE);
        code.append(ASSIGN).append(typeString);
        code.append(SPACE);
        code.append(rhs.getCode());
        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // 1. Unwrap nested return‑wrapper nodes
        JmmNode cur = node;
        while (true) {
            var kind = cur.getKind();
            if ((kind.equals(Kind.RETURN_STMT.getNodeName())
                    || kind.equals("ReturnStmt")
                    || kind.equals("RetStmt"))
                    && cur.getNumChildren() > 0) {
                cur = cur.getChild(0);
            } else {
                break;
            }
        }

        StringBuilder code = new StringBuilder();
        // 2. If unwrapped node is an expression, visit it
        if (cur.getNumChildren() > 0 || !cur.getKind().equals(node.getKind())) {
            var exprResult = exprVisitor.visit(cur);
            code.append(exprResult.getComputation());

            String operand = exprResult.getCode();
            if (operand.isEmpty() && cur.hasAttribute("name")) {
                operand = cur.get("name");
            }
            String suffix = ollirTypes.toOllirType(types.getExprType(cur));
            code.append("ret").append(suffix)
                    .append(" ").append(operand)
                    .append(";").append("\n");
        } else {
            // void return
            code.append("ret.V;").append("\n");
        }

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

        if (node.getBoolean("isPublic", false)) code.append("public ");
        if (node.getBoolean("isStatic", false)) code.append("static ");

        code.append(node.get("name")).append("(");

        // parameters
        node.getChildren().stream()
                .filter(c -> c.getKind().equals("ParamList"))
                .findFirst()
                .ifPresent(p -> {
                    String params = p.getChildren().stream()
                            .map(this::visit)
                            .collect(Collectors.joining(", "));
                    code.append(params);
                });
        code.append(")");

        // return type
        Type retType = types.convertType(
                node.getChildren().stream()
                        .filter(c -> c.getKind().equals("Type"))
                        .findFirst().orElse(node.getChild(0))
        );
        code.append(ollirTypes.toOllirType(retType)).append(" {\n");

        // statements
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("AssignStmt")) {
                code.append("   ").append(visit(child, unused));
            }
        }
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ReturnStmt") || child.getKind().equals("RetStmt")) {
                code.append("   ").append(visitReturn(child, unused));
            }
        }

        code.append("}\n\n");
        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        if (table.getSuper() != null) {
            code.append(" extends ").append(table.getSuper());
        }

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
        var arrayExpr = exprVisitor.visit(node.getChild(0));
        var indexExpr = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();
        code.append(arrayExpr.getComputation());
        code.append(indexExpr.getComputation());

        String temp = ollirTypes.nextTemp();

        // Generate proper array access
        code.append(temp).append(".i32 :=.i32 ")
                .append(arrayExpr.getCode()).append("[")
                .append(indexExpr.getCode()).append("].i32;\n");

        return new OllirExprResult(temp + ".i32", code.toString()).getComputation();
    }

    private String visitArrayInit(JmmNode node, Void unused) {
        int size = node.getNumChildren();
        String temp = ollirTypes.nextTemp();

        StringBuilder code = new StringBuilder();

        // Allocate array
        code.append(temp).append(".array.i32 :=.array.i32 ")
                .append("new(array, ").append(size).append(").array.i32;\n");

        // Initialize elements
        for (int i = 0; i < size; i++) {
            var elemExpr = exprVisitor.visit(node.getChild(i));
            code.append(elemExpr.getComputation());
            code.append(temp).append("[").append(i).append("].i32 :=.i32 ")
                    .append(elemExpr.getCode()).append(";\n");
        }

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
        var condExpr = exprVisitor.visit(node.getChild(0));
        String labelThen = "then" + ollirTypes.nextTemp("");
        String labelElse = "else" + ollirTypes.nextTemp("");
        String labelEnd = "endif" + ollirTypes.nextTemp("");

        StringBuilder code = new StringBuilder();
        code.append(condExpr.getComputation());

        // Generate proper conditional branch
        code.append("if (").append(condExpr.getCode())
                .append(") goto ").append(labelThen).append(";\n")
                .append("goto ").append(labelElse).append(";\n");

        // Then branch
        code.append(labelThen).append(":\n")
                .append(visit(node.getChild(1)))
                .append("goto ").append(labelEnd).append(";\n");

        // Else branch
        code.append(labelElse).append(":\n");
        if (node.getNumChildren() > 2) {
            code.append(visit(node.getChild(2)));
        }

        code.append(labelEnd).append(":\n");
        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        String labelCond = "cond" + ollirTypes.nextTemp("");
        String labelBody = "body" + ollirTypes.nextTemp("");
        String labelEnd = "endwhile" + ollirTypes.nextTemp("");

        var condExpr = exprVisitor.visit(node.getChild(0));

        StringBuilder code = new StringBuilder();

        // Loop condition
        code.append(labelCond).append(":\n")
                .append(condExpr.getComputation());

        // Conditional branch
        code.append("if (").append(condExpr.getCode())
                .append(") goto ").append(labelBody).append(";\n")
                .append("goto ").append(labelEnd).append(";\n");

        // Loop body
        code.append(labelBody).append(":\n")
                .append(visit(node.getChild(1)))
                .append("goto ").append(labelCond).append(";\n");

        // End of loop
        code.append(labelEnd).append(":\n");

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
