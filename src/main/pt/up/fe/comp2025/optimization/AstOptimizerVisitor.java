package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AstOptimizerVisitor extends PreorderJmmVisitor<Void, Void> {

    private final Map<String, String> constants = new HashMap<>();
    private boolean optimized = false;

    public void resetOptimized() {
        optimized = false;
    }
    public boolean hasOptimized() {
        return optimized;
    }

    public AstOptimizerVisitor() {
        setDefaultValue(null);
        buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Or",        this::foldBoolBinOp);
        addVisit("Id",        this::propagateId);
        addVisit("AddSub",    this::foldConstBinOp);
        addVisit("MulDiv",    this::foldConstBinOp);
        addVisit("And",       this::foldBoolBinOp);

        //addVisit("Compare",   this::foldConstCompare);
        addVisit(Kind.ASSIGN_STMT.getNodeName(), this::propagateAssign);
        addVisit(Kind.PROGRAM.getNodeName(),     this::visitProgram);
        addVisit(Kind.CLASS_DECL.getNodeName(),  this::visitClassDecl);
        addVisit(Kind.METHOD_DECL.getNodeName(), this::visitMethodDecl);
        addVisit(Kind.VAR_DECL.getNodeName(),    this::visitVarDecl);
        addVisit(Kind.PARAM.getNodeName(),       this::visitParam);
        addVisit(Kind.TYPE.getNodeName(),        this::visitType);
        addVisit(Kind.RETURN_STMT.getNodeName(), this::visitReturnStmt);
        addVisit(Kind.BINARY_EXPR.getNodeName(), this::visitBinaryExpr);

        setDefaultVisit((node, dummy) -> {
            for (var child : List.copyOf(node.getChildren()))
                visit(child);
            return null;
        });
    }

    private Void foldConstBinOp(JmmNode node, Void unused) {
        // first optimize children
        node.getChildren().forEach(this::visit);

        var left = node.getChildren().get(0);
        var right = node.getChildren().get(1);
        if (left.getKind().equals("Int") && right.getKind().equals("Int")) {
            int a = Integer.parseInt(left.get("name"));
            int b = Integer.parseInt(right.get("name"));
            var op = node.get("op");
            int res = switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> a / b;
                default -> throw new RuntimeException("Unknown op: " + op);
            };
            replaceWithLiteral(node, "Int", Integer.toString(res));
        }
        return null;
    }

    private Void foldConstCompare(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);

        var left = node.getChildren().get(0);
        var right = node.getChildren().get(1);
        if (left.getKind().equals("Int") && right.getKind().equals("Int")) {
            int a = Integer.parseInt(left.get("name"));
            int b = Integer.parseInt(right.get("name"));
            var op = node.get("op");
            boolean res = switch (op) {
                case "<"  -> a < b;
                case ">"  -> a > b;
                case "<=" -> a <= b;
                case ">=" -> a >= b;
                case "==" -> a == b;
                case "!=" -> a != b;
                default   -> throw new RuntimeException("Unknown cmp: " + op);
            };
            replaceWithLiteral(node, "Boolean", Boolean.toString(res));
        }
        return null;
    }
    private Void visitProgram(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitClassDecl(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitMethodDecl(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitVarDecl(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitParam(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitType(JmmNode node, Void unused) {
        return null;
    }

    private Void visitReturnStmt(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void visitBinaryExpr(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);
        return null;
    }

    private Void propagateAssign(JmmNode node, Void unused) {
        var target = node.getChildren().get(0);
        var value  = node.getChildren().get(1);

        if (!target.getKind().equals("Id")) {
            visit(value);
            return null;
        }

        String targetName = target.get("name");
        boolean inLoop    = node.getAncestor("WhileStmt").isPresent();

        if (!inLoop && value.getKind().equals("Int")) {
            constants.put(targetName, value.get("name"));
        } else {
            constants.remove(targetName);
        }

        visit(value);
        return null;
    }

    private Void propagateId(JmmNode node, Void unused) {
        if (node.getParent() != null && node.getParent().getKind().equals(Kind.ASSIGN_STMT.getNodeName())) {
            if (node.equals(node.getParent().getChild(0))) {
                return null;
            }
        }
        String name = node.get("name");
        if (constants.containsKey(name)) {
            String litValue = constants.get(name);
            var lit = node.copy(Collections.singletonList("Int"));
            lit.put("name", litValue);
            node.replace(lit);
        }
        return null;
    }


    private Void foldBoolBinOp(JmmNode node, Void unused) {
        node.getChildren().forEach(this::visit);

        var left = node.getChildren().get(0);
        var right = node.getChildren().get(1);
        if (left.getKind().equals("Boolean") && right.getKind().equals("Boolean")) {
            boolean b1 = left.get("name").equals("true");
            boolean b2 = right.get("name").equals("true");
            boolean res = node.getKind().equals("And") ? (b1 && b2) : (b1 || b2);
            replaceWithLiteral(node, "Boolean", Boolean.toString(res));
        }
        return null;
    }

    private void replaceWithLiteral(JmmNode node, String newKind, String litValue) {
        var lit = node.copy(Collections.singletonList(newKind));
        lit.put("name", litValue);
        node.replace(lit);
        optimized=true;
    }

}