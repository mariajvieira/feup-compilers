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
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("AddSub", this::visitArithmeticOp);
        addVisit("MulDiv", this::visitArithmeticOp);
        addVisit("Compare", this::visitCompare);
        addVisit("And", this::visitLogicalOp);
        addVisit("Or", this::visitLogicalOp);
        addVisit("Not", this::visitNot);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("WhileStmt", this::visitWhileStmt);
        addVisit("ReturnStmt", this::visitReturnStmt);
        addVisit("MethodCall", this::visitMethodCall);
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
                            (valueType.isArray() ? "[]" : "") + "' to variable of type '" +
                            targetType.getName() + (targetType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitArithmeticOp(JmmNode opNode, SymbolTable table) {
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
                            leftType.getName() + (leftType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }

        if (!isIntType(rightType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    opNode.getLine(),
                    opNode.getColumn(),
                    "Right operand of arithmetic operation must be of type 'int', but got '" +
                            rightType.getName() + (rightType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitCompare(JmmNode compareNode, SymbolTable table) {
        JmmNode left = compareNode.getChildren().get(0);
        JmmNode right = compareNode.getChildren().get(1);

        Type leftType = typeUtils.getExprType(left);
        Type rightType = typeUtils.getExprType(right);

        // For comparison operations '<', '>', '<=', '>=' operands must be of type int
        if (compareNode.get("op").matches("<|>|<=|>=")) {
            if (!isIntType(leftType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        compareNode.getLine(),
                        compareNode.getColumn(),
                        "Left operand of comparison operation must be of type 'int', but got '" +
                                leftType.getName() + (leftType.isArray() ? "[]" : "") + "'",
                        null
                ));
            }

            if (!isIntType(rightType)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        compareNode.getLine(),
                        compareNode.getColumn(),
                        "Right operand of comparison operation must be of type 'int', but got '" +
                                rightType.getName() + (rightType.isArray() ? "[]" : "") + "'",
                        null
                ));
            }
        }
        // For equality operations '==', '!=' operands must have compatible types
        else {
            if (!isTypeCompatible(leftType, rightType, table)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        compareNode.getLine(),
                        compareNode.getColumn(),
                        "Incompatible types in equality operation: '" +
                                leftType.getName() + (leftType.isArray() ? "[]" : "") + "' and '" +
                                rightType.getName() + (rightType.isArray() ? "[]" : "") + "'",
                        null
                ));
            }
        }

        return null;
    }

    private Void visitLogicalOp(JmmNode logicalNode, SymbolTable table) {
        JmmNode left = logicalNode.getChildren().get(0);
        JmmNode right = logicalNode.getChildren().get(1);

        Type leftType = typeUtils.getExprType(left);
        Type rightType = typeUtils.getExprType(right);

        if (!isBooleanType(leftType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    logicalNode.getLine(),
                    logicalNode.getColumn(),
                    "Left operand of logical operation must be of type 'boolean', but got '" +
                            leftType.getName() + (leftType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }

        if (!isBooleanType(rightType)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    logicalNode.getLine(),
                    logicalNode.getColumn(),
                    "Right operand of logical operation must be of type 'boolean', but got '" +
                            rightType.getName() + (rightType.isArray() ? "[]" : "") + "'",
                    null
            ));
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
                            exprType.getName() + (exprType.isArray() ? "[]" : "") + "'",
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
                            indexType.getName() + (indexType.isArray() ? "[]" : "") + "'",
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
                            conditionType.getName() + (conditionType.isArray() ? "[]" : "") + "'",
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
                            conditionType.getName() + (conditionType.isArray() ? "[]" : "") + "'",
                    null
            ));
        }

        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        if (!returnStmt.getChildren().isEmpty()) {
            JmmNode returnExpr = returnStmt.getChildren().get(0);
            Type returnExprType = typeUtils.getExprType(returnExpr);
            Type methodReturnType = table.getReturnType(currentMethod);

            if (!isTypeCompatible(methodReturnType, returnExprType, table)) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        returnStmt.getLine(),
                        returnStmt.getColumn(),
                        "Incompatible return type: expected '" +
                                methodReturnType.getName() + (methodReturnType.isArray() ? "[]" : "") +
                                "', but got '" + returnExprType.getName() +
                                (returnExprType.isArray() ? "[]" : "") + "'",
                        null
                ));
            }
        }

        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        JmmNode caller = methodCall.getChildren().get(0);
        String methodName = methodCall.get("name");
        List<JmmNode> args = methodCall.getChildren().subList(1, methodCall.getChildren().size());

        Type callerType = typeUtils.getExprType(caller);

        // If the caller is of type this, check if the method exists in the current class
        if (callerType.getName().equals(table.getClassName())) {
            if (!table.getMethods().contains(methodName)) {
                // If the class extends another class, we assume the method is defined in the superclass
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
                // Check argument types
                List<Symbol> parameters = table.getParameters(methodName);
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
                                    "Incompatible argument type for parameter " + (i + 1) +
                                            ": expected '" + paramType.getName() +
                                            (paramType.isArray() ? "[]" : "") + "', but got '" +
                                            argType.getName() + (argType.isArray() ? "[]" : "") + "'",
                                    null
                            ));
                        }
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
        // Same type
        if (targetType.getName().equals(valueType.getName()) && targetType.isArray() == valueType.isArray()) {
            return true;
        }

        // Assignment to object types: check if value type is a subclass of target type
        if (!targetType.isArray() && !valueType.isArray()) {
            // Check if value type extends target type
            if (valueType.getName().equals(table.getClassName()) &&
                    targetType.getName().equals(table.getSuper())) {
                return true;
            }

            // Check if target type is an imported class (assuming compatibility)
            if (table.getImports().contains(targetType.getName())) {
                return true;
            }
        }

        return false;
    }
}