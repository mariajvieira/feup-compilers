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
            case "AddSub", "MulDiv", "Compare" -> newIntType(); // Operações aritméticas e comparações retornam int
            case "And", "Or", "Not" -> new Type("boolean", false); // Operadores booleanos retornam boolean
            case "Int" -> newIntType(); // Literal inteiro
            case "True", "False" -> new Type("boolean", false); // Literais booleanos
            case "This" -> new Type(table.getClassName(), false); // Palavra-chave this, tipo da própria classe
            case "NewArray" -> new Type("int", true); // Arrays de inteiros
            case "NewObject" -> new Type(expr.get("name"), false); // Instância de objeto, o nome da classe
            case "ArrayAccess" -> newIntType(); // Acesso a arrays, devolve o tipo base (int)
            case "ArrayLiteral" -> new Type("int", true); // Literal array (int[])
            case "Id" -> {
                String varName = expr.get("name");
                Type type = getTypeFromSymbolTable(varName);
                yield type != null ? type : new Type("unknown", false); // Busca o tipo na SymbolTable
            }
            case "MethodCall" -> {
                // Aqui poderias procurar a função na tabela de símbolos, mas para já devolve 'unknown'
                yield new Type("unknown", false);
            }
            case "Parenthesis" -> getExprType(expr.getJmmChild(0)); // Tipo do que está dentro dos parêntesis
            default -> new Type("unknown", false); // Por defeito, tipo desconhecido
        };
    }



    private Type getTypeFromSymbolTable(String varName, String methodSignature) {
        // Procurar na lista de variáveis locais do método
        for (var local : table.getLocalVariables(methodSignature)) {
            if (local.getName().equals(varName)) return local.getType();
        }
        // Procurar na lista de parâmetros do método
        for (var param : table.getParameters(methodSignature)) {
            if (param.getName().equals(varName)) return param.getType();
        }
        // Procurar na lista de campos (fields) da classe
        for (var field : table.getFields()) {
            if (field.getName().equals(varName)) return field.getType();
        }
        return null; // Se não encontrar, devolve null
    }
}