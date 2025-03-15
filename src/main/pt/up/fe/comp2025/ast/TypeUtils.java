package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

public class TypeUtils {

    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type convertType(JmmNode typeNode) {
        var name = typeNode.get("name");
        var isArray = typeNode.hasAttribute("isArray") && Boolean.parseBoolean(typeNode.get("isArray"));
        return new Type(name, isArray);
    }

    public Type getExprType(JmmNode expr) {
        if (expr.getKind().equals("BinaryOp")) {
            return newIntType();
        }
        if (expr.getKind().equals("BooleanOp")) {
            return new Type("boolean", false);
        }
        return new Type("unknown", false);
    }
}
