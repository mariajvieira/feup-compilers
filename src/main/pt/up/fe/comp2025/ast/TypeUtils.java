package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;


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
        var isArray = typeNode.hasAttribute("isArray") && Boolean.parseBoolean(typeNode.get("isArray"));
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
            case "And", "Or", "Not" -> new Type("boolean", false);
            case "Int" -> newIntType();
            case "True", "False" -> new Type("boolean", false);
            case "This" -> new Type(table.getClassName(), false);
            case "NewArray" -> new Type("int", true);
            case "NewObject" -> new Type(expr.get("name"), false);
            case "ArrayAccess" -> newIntType();
            case "ArrayLiteral" -> new Type("int", true);
           // case "Id" -> {
             //   String varName = expr.get("name");
                //Type type = getTypeFromSymbolTable(varName);
                //yield type != null ? type : new Type("unknown", false);
            //}
            case "MethodCall" -> { // por fazer
                yield new Type("unknown", false);
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