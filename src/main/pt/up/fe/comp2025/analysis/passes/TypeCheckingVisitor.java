package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;

public class TypeCheckingVisitor extends AnalysisVisitor {

    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL,     this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT,     this::visitAssignStmt);
        addVisit(Kind.ADD_SUB,         this::visitArithmeticOp);
        addVisit(Kind.MUL_DIV,         this::visitArithmeticOp);
        addVisit(Kind.COMPARE,         this::visitCompare);
        addVisit(Kind.EQUAL_DIFF,      this::visitEqualDiff);
        addVisit(Kind.AND,             this::visitLogicalOp);
        addVisit(Kind.OR,              this::visitLogicalOp);
        addVisit(Kind.NOT,             this::visitNot);
        addVisit(Kind.ARRAY_ACCESS,    this::visitArrayAccess);
        addVisit(Kind.IF_STMT,         this::visitIfStmt);
        addVisit(Kind.WHILE_STMT,      this::visitWhileStmt);
        addVisit(Kind.RET_STMT,        this::visitReturnStmt);
        addVisit(Kind.METHOD_CALL,     this::visitMethodCall);
        addVisit(Kind.ARRAY_LITERAL,   this::visitArrayLiteral);
        addVisit(Kind.LENGTH,          this::visitLength);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        typeUtils = new TypeUtils(table);
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode target = assignStmt.getChildren().get(0);
        JmmNode value = assignStmt.getChildren().get(1);

        Type targetType = typeUtils.getExprType(target);
        Type valueType = typeUtils.getExprType(value);

        if (!isTypeCompatible(targetType, valueType, table)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    assignStmt.getLine(),
                    assignStmt.getColumn(),
                    "Cannot assign value of type '" + valueType.getName() +
                            (valueType.isArray() ? "" : "") + "' to variable of type '" +
                            targetType.getName() + (targetType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }

    // File: src/main/pt/up/fe/comp2025/analysis/passes/TypeCheckingVisitor.java
    private Void visitArithmeticOp(JmmNode opNode, SymbolTable table) {
        // Skip unary (no operator) cases
        if (opNode.getNumChildren() < 2) {
            return null;
        }

        JmmNode left = opNode.getChildren().get(0);
        JmmNode right = opNode.getChildren().get(1);

        Type leftType = typeUtils.getExprType(left);
        Type rightType = typeUtils.getExprType(right);

        if (!isIntType(leftType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    opNode.getLine(),
                    opNode.getColumn(),
                    "Left operand of arithmetic operation must be of type 'int', but got '" +
                            leftType.getName() + (leftType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        if (!isIntType(rightType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    opNode.getLine(),
                    opNode.getColumn(),
                    "Right operand of arithmetic operation must be of type 'int', but got '" +
                            rightType.getName() + (rightType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }


    private Void visitCompare(JmmNode compareNode, SymbolTable table) {
        // No relational operator => nothing to check
        if (compareNode.getNumChildren() < 2) {
            return null;
        }

        // For binary or chained comparisons, check each operand pair
        JmmNode left = compareNode.getChildren().get(0);
        for (int i = 1; i < compareNode.getNumChildren(); i++) {
            JmmNode right = compareNode.getChildren().get(i);

            var leftType  = typeUtils.getExprType(left);
            var rightType = typeUtils.getExprType(right);

            if (!isIntType(leftType) || leftType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        left.getLine(), left.getColumn(),
                        "Comparison operator requires integer operands",
                        null));
            }
            if (!isIntType(rightType) || rightType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        right.getLine(), right.getColumn(),
                        "Comparison operator requires integer operands",
                        null));
            }

            left = right;
        }
        return null;
    }


    private Void visitLogicalOp(JmmNode logicalNode, SymbolTable table) {
        // No operator => nothing to check
        if (logicalNode.getNumChildren() < 2) {
            return null;
        }

        // First operand
        JmmNode left = logicalNode.getChildren().get(0);
        for (int i = 1; i < logicalNode.getNumChildren(); i++) {
            JmmNode right = logicalNode.getChildren().get(i);

            Type leftType = typeUtils.getExprType(left);
            Type rightType = typeUtils.getExprType(right);

            if (!isBooleanType(leftType)) {
                addReport(Report.newError(Stage.SEMANTIC,
                        left.getLine(), left.getColumn(),
                        "Logical operator requires boolean operands", null));
            }
            if (!isBooleanType(rightType)) {
                addReport(Report.newError(Stage.SEMANTIC,
                        right.getLine(), right.getColumn(),
                        "Logical operator requires boolean operands", null));
            }

            // For chained ops, next left is current right
            left = right;
        }
        return null;
    }


    private Void visitNot(JmmNode notNode, SymbolTable table) {
        JmmNode expr = notNode.getChildren().get(0);
        Type exprType = typeUtils.getExprType(expr);

        if (!isBooleanType(exprType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    notNode.getLine(),
                    notNode.getColumn(),
                    "Operand of logical NOT operation must be of type 'boolean', but got '" +
                            exprType.getName() + (exprType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        JmmNode array = arrayAccess.getChildren().get(0);
        JmmNode index = arrayAccess.getChildren().get(1);

        Type arrayType = typeUtils.getExprType(array);
        Type indexType = typeUtils.getExprType(index);

        if (!arrayType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Array access requires an array type, but got '" + arrayType.getName() + "'",
                    null
            ));
        }

        if (!isIntType(indexType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccess.getLine(),
                    arrayAccess.getColumn(),
                    "Array index must be of type 'int', but got '" +
                            indexType.getName() + (indexType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table) {
        JmmNode condition = ifStmt.getChildren().get(0);
        Type conditionType = typeUtils.getExprType(condition);

        if (!isBooleanType(conditionType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    ifStmt.getLine(),
                    ifStmt.getColumn(),
                    "If condition must be of type 'boolean', but got '" +
                            conditionType.getName() + (conditionType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        JmmNode condition = whileStmt.getChildren().get(0);
        Type conditionType = typeUtils.getExprType(condition);

        if (!isBooleanType(conditionType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    whileStmt.getLine(),
                    whileStmt.getColumn(),
                    "While condition must be of type 'boolean', but got '" +
                            conditionType.getName() + (conditionType.isArray() ? "" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        if (!returnStmt.getChildren().isEmpty()) {
            JmmNode returnExpr = returnStmt.getChildren().get(0);
            Type methodReturnType = table.getReturnType(currentMethod);
            Type returnExprType = typeUtils.getExprType(returnExpr);


            if (!isTypeCompatible(methodReturnType, returnExprType, table)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        returnStmt.getLine(),
                        returnStmt.getColumn(),
                        "Incompatible return type: expected '" +
                                methodReturnType.getName() + (methodReturnType.isArray() ? "" : "") +
                                "', but got '" + returnExprType.getName() +
                                (returnExprType.isArray() ? "" : "") + "'",
                        null
                ));
            }
        }

        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        JmmNode caller = methodCall.getChildren().get(0);
        String methodName = methodCall.get("methodName");
        List<JmmNode> args = methodCall.getChildren().subList(1, methodCall.getChildren().size());

        Type callerType = typeUtils.getExprType(caller);

        if (callerType.getName().equals(table.getClassName())) {
            if (!table.getMethods().contains(methodName)) {
                if (table.getSuper() == null) {
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            methodCall.getLine(),
                            methodCall.getColumn(),
                            "Method '" + methodName + "' not defined in class '" + table.getClassName() + "'",
                            null
                    ));
                }
            } else {
                List<Symbol> parameters = table.getParameters(methodName);

                boolean hasVarargs = !parameters.isEmpty() &&
                        parameters.get(parameters.size() - 1).getType().isArray();

                if (hasVarargs) {
                    if (args.size() < parameters.size() - 1) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                methodCall.getLine(),
                                methodCall.getColumn(),
                                "Method '" + methodName + "' called with too few arguments",
                                null
                        ));
                    } else {
                        for (int i = 0; i < parameters.size() - 1; i++) {
                            Type argType = typeUtils.getExprType(args.get(i));
                            Type paramType = parameters.get(i).getType();

                            if (!isTypeCompatible(paramType, argType, table)) {
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        args.get(i).getLine(),
                                        args.get(i).getColumn(),
                                        "Incompatible argument type for parameter " + (i + 1),
                                        null
                                ));
                            }
                        }

                        Type varargType = new Type(parameters.get(parameters.size() - 1).getType().getName(), false);
                        for (int i = parameters.size() - 1; i < args.size(); i++) {
                            Type argType = typeUtils.getExprType(args.get(i));
                            if (!isTypeCompatible(varargType, argType, table)) {
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        args.get(i).getLine(),
                                        args.get(i).getColumn(),
                                        "Incompatible argument type for varargs parameter",
                                        null
                                ));
                            }
                        }
                    }
                } else {
                    if (args.size() != parameters.size()) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                methodCall.getLine(),
                                methodCall.getColumn(),
                                "Method '" + methodName + "' called with incorrect number of arguments: expected " +
                                        parameters.size() + ", got " + args.size(),
                                null
                        ));
                    } else {
                        for (int i = 0; i < args.size(); i++) {
                            Type argType = typeUtils.getExprType(args.get(i));
                            Type paramType = parameters.get(i).getType();

                            if (!isTypeCompatible(paramType, argType, table)) {
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        args.get(i).getLine(),
                                        args.get(i).getColumn(),
                                        "Incompatible argument type for parameter " + (i + 1),
                                        null
                                ));
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private Void visitArrayLiteral(JmmNode arrayLiteral, SymbolTable table) {
        if (!arrayLiteral.getChildren().isEmpty()) {
            JmmNode arrayInitNode = arrayLiteral.getChildren().get(0);
            if (arrayInitNode != null && arrayInitNode.getChildren().size() > 0) {
                Type firstElementType = typeUtils.getExprType(arrayInitNode.getChildren().get(0));
                for (int i = 1; i < arrayInitNode.getChildren().size(); i++) {
                    Type currentElementType = typeUtils.getExprType(arrayInitNode.getChildren().get(i));
                    if (!isTypeCompatible(firstElementType, currentElementType, table)) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                arrayInitNode.getChildren().get(i).getLine(),
                                arrayInitNode.getChildren().get(i).getColumn(),
                                "Incompatible type in array initializer: expected '" +
                                        firstElementType.getName() + (firstElementType.isArray() ? "" : "") +
                                        "', but got '" + currentElementType.getName() +
                                        (currentElementType.isArray() ? "" : "") + "'",
                                null
                        ));
                    }
                }
            }
        }
        return null;
    }


    private boolean isIntType(Type type) {
        return "int".equals(type.getName()) && !type.isArray();
    }

    private boolean isBooleanType(Type type) {
        return "boolean".equals(type.getName()) && !type.isArray();
    }

    private boolean isTypeCompatible(Type targetType, Type valueType, SymbolTable table) {
        // Se algum dos tipos for unknown, considera compatível se houver herança ou importação
        if (valueType.getName().equals("unknown")
                && targetType.getName().equals(table.getClassName())) {
            return true;
        }

        if (valueType.getName().equals("unknown")) {
            if (isImported(targetType.getName(), table)
                    || (table.getSuper() != null && targetType.getName().equals(table.getSuper()))) {
                return true;
            }
        }

        // Mesmos tipos e dimensões de array
        if (targetType.getName().equals(valueType.getName()) &&
                targetType.isArray() == valueType.isArray()) {
            return true;
        }

        // Para tipos não-array
        if (!targetType.isArray() && !valueType.isArray()) {
            // Se ambos os tipos são importados
            if (isImported(targetType.getName(), table) &&
                    isImported(valueType.getName(), table)) {
                return true;
            }

            // Verifica herança
            if (valueType.getName().equals(table.getClassName()) &&
                    targetType.getName().equals(table.getSuper())) {
                return true;
            }
        }

        return false;
    }

    private boolean isImported(String typeName, SymbolTable table) {
        if (typeName == null || typeName.equals("unknown")) {
            return false;
        }
        return table.getImports().stream()
                .anyMatch(imp -> imp.equals(typeName) || imp.endsWith("." + typeName));
    }


    private Void visitEqualDiff(JmmNode node, SymbolTable table) {
        var children = node.getChildren();
        Type leftType = typeUtils.getExprType(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            Type rightType = typeUtils.getExprType(children.get(i));
            if (!leftType.getName().equals(rightType.getName())
                    || leftType.isArray() != rightType.isArray()) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        children.get(i).getLine(),
                        children.get(i).getColumn(),
                        "Operands of '" + node.getKind() + "' must have same type",
                        null
                ));
            }
            leftType = rightType;
        }
        return null;
    }


    private Void visitLength(JmmNode node, SymbolTable table) {
        JmmNode arrayExpr = node.getChildren().get(0);
        Type arrayType = typeUtils.getExprType(arrayExpr);
        if (!arrayType.isArray()) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    "'.length' can only be applied to arrays, but got '" +
                            arrayType.getName() + (arrayType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }
        return null;
    }



}