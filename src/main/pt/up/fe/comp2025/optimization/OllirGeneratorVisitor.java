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
        // Basic node types
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);

        // Statement types
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("ReturnStmt", this::visitReturn);
        addVisit("RetStmt", this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        // Expression types
        addVisit("MethodCall", this::visitMethodCall);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        //addVisit(ASSIGN_ARRAY_STMT, this::visitArrayAssign);

        // Other
        addVisit("ImportDecl", this::visitImportDecl);

        // Default visitor for unhandled nodes
        setDefaultVisit(this::defaultVisit);
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        System.out.println("ExprStmt node: " + node.toTree());

        if (node.getNumChildren() > 0) {
            JmmNode child = node.getChild(0);
            System.out.println("ExprStmt child kind: " + child.getKind());

            if (child.getKind().equals("MethodCall")) {
                return visitMethodCall(child, unused);
            }

            return visit(child);
        }
        return "";
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


    // File: src/main/pt/up/fe/comp2025/optimization/OllirGeneratorVisitor.java
    private String visitAssignStmt(JmmNode node, Void unused) {
        // left-hand side and right-hand side
        var leftNode  = node.getChild(0);
        var valueNode = node.getChild(1);

        // Array‐element assignment
        if (leftNode.getKind().equals("ArrayAccess")) {
            // inside the ArrayAccess node: [ arrayExpr, indexExpr ]
            var arrayExpr = leftNode.getChild(0);
            var idxExpr   = leftNode.getChild(1);

            var idxRes = exprVisitor.visit(idxExpr);
            var valRes = exprVisitor.visit(valueNode);

            String arrName = arrayExpr.get("name"); // bare identifier
            StringBuilder code = new StringBuilder();
            code
                    .append(idxRes.getComputation())
                    .append(valRes.getComputation())
                    .append(arrName)
                    .append("[")
                    .append(idxRes.getCode())
                    .append("].i32 :=.i32 ")
                    .append(valRes.getCode())
                    .append(";\n");

            return code.toString();
        }

        // Regular variable assignment (unchanged)
        StringBuilder code = new StringBuilder();
        String varName    = leftNode.get("name");
        Type   thisType   = types.getExprType(leftNode);
        String typeString = ollirTypes.toOllirType(thisType);
        var rhsResult     = exprVisitor.visit(valueNode);

        code
                .append(rhsResult.getComputation())
                .append(varName)
                .append(typeString)
                .append(" :=.")
                .append(typeString.substring(1))
                .append(" ")
                .append(rhsResult.getCode())
                .append(";\n");

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        JmmNode cur = node;
        while (true) {
            var kind = cur.getKind();
            if (kind.equals(Kind.RET_STMT.getNodeName()) || kind.equals("RetStmt")) {
                if (cur.getNumChildren() == 0) {
                    break;
                } else {
                    cur = cur.getChildren().get(0);
                }
            } else {
                break;
            }
        }

        StringBuilder code = new StringBuilder();
        if (cur.getNumChildren() > 0 || !cur.getKind().equals(node.getKind())) {
            var exprResult = exprVisitor.visit(cur);
            code.append(exprResult.getComputation());
            String operand = exprResult.getCode();
            Type exprType = types.getExprType(cur);
            String suffix = ollirTypes.toOllirType(exprType);

            var optName = cur.getOptional("name");
            String varName = optName.orElse(null);
            String methodName = node.getAncestor(METHOD_DECL).map(n -> n.get("name")).orElse("");
            boolean isLocalOrParam = varName != null && (
                    table.getLocalVariables(methodName).stream().anyMatch(v -> v.getName().equals(varName))
                            || table.getParameters(methodName).stream().anyMatch(v -> v.getName().equals(varName))
            );
            if (varName != null && !isLocalOrParam &&
                    table.getFields().stream().anyMatch(f -> f.getName().equals(varName))) {
                String temp = ollirTypes.nextTemp();
                code.append(temp).append(suffix)
                        .append(" :=.").append(suffix.substring(1))
                        .append(" getfield(this, ").append(varName).append(suffix)
                        .append(").").append(suffix.substring(1)).append(";\n");
                code.append("ret").append(suffix).append(" ").append(temp).append(suffix).append(";\n");
            } else {
                code.append("ret").append(suffix).append(" ").append(operand).append(";\n");
            }
        } else {
            code.append("ret.V;\n");
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
        StringBuilder code = new StringBuilder();

        // Method signature
        code.append(".method ");
        if (node.getBoolean("isPublic", false)) code.append("public ");
        if (node.getBoolean("isStatic", false)) code.append("static ");
        code.append(node.get("name")).append("(");
        node.getChildren().stream()
                .filter(c -> c.getKind().equals("ParamList"))
                .findFirst()
                .ifPresent(p -> code.append(
                        p.getChildren().stream()
                                .map(this::visit)
                                .collect(Collectors.joining(", "))
                ));
        code.append(")");
        var retTypeNode = node.getChildren().stream()
                .filter(c -> c.getKind().equals("Type"))
                .findFirst().orElse(node.getChild(0));
        code.append(ollirTypes.toOllirType(TypeUtils.convertType(retTypeNode)))
                .append(" {\n");

        // Single pass over body
        for (var child : node.getChildren()) {
            String stmtKind = child.getKind();
            String childCode = "";
            switch (stmtKind) {
                case "VarDecl"      -> childCode = visit(child, unused);
                case "AssignStmt"   -> childCode = visitAssignStmt(child, unused);
                case "ReturnStmt", "RetStmt"    -> childCode = visitReturn(child, unused);
                case "WhileStmt"    -> childCode = visitWhileStmt(child, unused);
                case "IfStmt"       -> childCode = visitIfStmt(child, unused);
                case "MethodCall"   -> childCode = visitMethodCall(child, unused);
                case "ExprStmt"     -> childCode = visitExprStmt(child, unused);
                default -> {
                    continue;
                }
            }
            if (!childCode.isBlank()) {
                code.append("   ").append(childCode);
                if (!childCode.endsWith("\n")) code.append("\n");
            }
        }
        boolean isVoid = node.getChildren().stream()
                .filter(c -> c.getKind().equals("Type"))
                .map(TypeUtils::convertType)
                .anyMatch(t -> t.getName().equals("void"));

        boolean hasReturn = node.getChildren().stream()
                .anyMatch(c -> c.getKind().equals("ReturnStmt") || c.getKind().equals("RetStmt"));

        if (isVoid && !hasReturn) {
            code.append("   ret.V;\n");
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

        for (var field : table.getFields()) {
            code.append(".field public ").append(field.getName()).append(ollirTypes.toOllirType(field.getType())).append(";\n");
        }

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
        for (var child : node.getChildren()) {
            if (child.getKind().equals("ImportDecl")) {
                code.append(visit(child));
            }
        }
        for (var child : node.getChildren()) {
            if (child.getKind().equals("ClassDecl")) {
                code.append(visit(child));
            }
        }

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
        System.out.println("Visiting node kind: " + node.getKind());
        for (var child : node.getChildren()) {
            visit(child);
        }
        return "";
    }


    private String visitArrayAccess(JmmNode node, Void unused) {
        var arrayExpr = exprVisitor.visit(node.getChild(0));
        var indexExpr = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();
        code.append(arrayExpr.getComputation());
        code.append(indexExpr.getComputation());

        String temp = ollirTypes.nextTemp();

        code.append(temp).append(".i32 :=.i32 ")
                .append(arrayExpr.getCode()).append("[")
                .append(indexExpr.getCode()).append("].i32;\n");

        return new OllirExprResult(temp + ".i32", code.toString()).getComputation();
    }


    private String visitArrayAssign(JmmNode node, Void unused) {
        var arrNode = node.getChild(0);
        var idxNode = node.getChild(1);
        var valNode = node.getChild(2);

        var idxRes = exprVisitor.visit(idxNode);
        var valRes = exprVisitor.visit(valNode);

        String arrName = arrNode.get("name");           // just the identifier
        StringBuilder code = new StringBuilder();
        code.append(idxRes.getComputation())
                .append(valRes.getComputation());

        // emit: a[<idx>].i32 :=.i32 <value>;
        code.append(arrName)
                .append("[")
                .append(idxRes.getCode())
                .append("].i32 :=.i32 ")
                .append(valRes.getCode())
                .append(";\n");

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
        var callerNode = node.getChild(0);
        var callerRes  = exprVisitor.visit(callerNode);
        StringBuilder code = new StringBuilder(callerRes.getComputation());
        StringBuilder args = new StringBuilder();
        for (int i = 1; i < node.getNumChildren(); i++) {
            var argRes = exprVisitor.visit(node.getChild(i));
            code.append(argRes.getComputation());
            if (i > 1) args.append(", ");
            args.append(argRes.getCode());
        }

        String callerName = callerNode.get("name");
        String methodName = node.get("methodName");

        // Special case for io.println -> invokestatic
        if (callerName.equals("io") && methodName.equals("println")) {
            code.append("invokestatic(io")
                    .append(", \"println\"")
                    .append(args.length() > 0 ? ", " + args : "")
                    .append(").V;\n");
            return code.toString();
        }

        // existing fallback to invokevirtual
        Type returnType = types.getExprType(node);
        String typeStr  = ollirTypes.toOllirType(returnType);
        code.append("invokevirtual(")
                .append(callerRes.getCode())
                .append(", \"").append(methodName).append("\"")
                .append(args.length() > 0 ? ", " + args : "")
                .append(")").append(typeStr).append(";\n");
        return code.toString();
    }
}
