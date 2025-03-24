package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

import pt.up.fe.comp2025.ast.Kind;


/**
 * Utility methods regarding types.
 */
public class TypeUtils {

    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    public static Type newStringType() {
        return new Type("String", false);
    }

    public static Type newVoidType() {
        return new Type("void", false);
    }

    public static Type convertType(JmmNode typeNode) {
        var name = typeNode.get("name");
        boolean isArray = Boolean.parseBoolean(typeNode.getOptional("isArray").orElse("false"));
        return new Type(name, isArray);
    }


    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public Type getExprType(JmmNode expr) {
        return switch (expr.getKind()) {
            case "AddSub", "MulDiv", "Compare" -> newIntType();
            case "And", "Or", "Not" -> newBooleanType();
            case "Int" -> newIntType();
            case "True", "False" -> newBooleanType();
            case "This" -> new Type(table.getClassName(), false);
            case "NewArray" -> new Type("int", true);
            case "NewObject" -> new Type(expr.get("name"), false);
            case "ArrayAccess" -> newIntType();
            case "ArrayLiteral" -> new Type("int", true);
            case "MethodCall" -> {
                var methodName = expr.get("name");
                var returnType = table.getReturnType(methodName);
                yield returnType != null ? returnType : new Type("unknown", false);
            }
            case "Id" -> {
                String varName = expr.get("name");
                String methodSignature = expr.getAncestor(Kind.METHOD_DECL).map(node -> node.get("name")).orElse(null);
                Type type = getTypeFromSymbolTable(varName, methodSignature);
                yield type != null ? type : new Type("unknown", false);
            }
            case "Parenthesis" -> getExprType(expr.getChildren().get(0));
            default -> new Type("unknown", false);
        };
    }


    private Type getTypeFromSymbolTable(String varName, String methodSignature) {
        for (var local : table.getLocalVariables(methodSignature)) {
            if (local.getName().equals(varName)) return local.getType();
        }
        for (var param : table.getParameters(methodSignature)) {
            if (param.getName().equals(varName)) return param.getType();
        }
        for (var field : table.getFields()) {
            if (field.getName().equals(varName)) return field.getType();
        }
        return null; // Se não encontrar, devolve null
    }
}