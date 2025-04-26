package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;
import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final TypeUtils types;
    private final OptUtils ollirTypes;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        setDefaultValue(() -> OllirExprResult.EMPTY);
        buildVisitor();
    }

    @Override
    protected void buildVisitor() {
        // Integer literal (grammar labels it "Int")
        addVisit("Int", this::visitInteger);

        // Variable references
        addVisit("Id", this::visitVarRef);

        // Binary operators
        addVisit("AddSub", this::visitBinExpr);
        addVisit("MulDiv", this::visitBinExpr);
        addVisit("Compare", this::visitBinExpr);
        addVisit("And",    this::visitBinExpr);
        addVisit("Or",     this::visitBinExpr);


        addVisit("NewArray", this::visitNewArray);
        addVisit("ArrayLiteral", this::visitArrayLiteral);
        // Fallback
        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String value = node.get("name");                      // grammar stores lexeme in "name"
        String code  = value + ollirIntType;                  // e.g. "1.i32"
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        String op = node.get("op");
        Type resultType = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(resultType).substring(1);

        String tmp = ollirTypes.nextTemp();
        String instr = tmp + "." + ollirType
                + " :=." + ollirType
                + " " + lhs.getCode()
                + " " + op + "." + ollirType
                + " " + rhs.getCode()
                + ";\n";

        String computation = lhs.getComputation()
                + rhs.getComputation()
                + instr;

        return new OllirExprResult(tmp + "." + ollirType, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        String name = node.get("name");
        Type   t    = types.getExprType(node);
        String suffix = ollirTypes.toOllirType(t);
        return new OllirExprResult(name + suffix);
    }

    private OllirExprResult visitArrayLiteral(JmmNode node, Void unused) {
        int size = node.getNumChildren();
        String temp = ollirTypes.nextTemp();
        StringBuilder code = new StringBuilder();
        code.append(temp).append(".array.i32 :=.array.i32 new(array, ")
                .append(size).append(".i32)").append(".array.i32;\n");
        for (int i = 0; i < size; i++) {
            var elemResult = visit(node.getChild(i));
            code.append(elemResult.getComputation());
            code.append(temp).append("[").append(i).append("].i32 :=.i32 ")
                    .append(elemResult.getCode()).append(";\n");
        }
        return new OllirExprResult(temp + ".array.i32", code.toString());
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        var sizeExpr = visit(node.getChild(0));
        Type newArrayType = new Type("int", true);
        String suffix = ollirTypes.toOllirType(newArrayType);
        String temp = ollirTypes.nextTemp();
        StringBuilder code = new StringBuilder();
        code.append(sizeExpr.getComputation());
        code.append(temp).append(suffix)
                .append(" :=").append(suffix)
                .append(" new(array, ").append(sizeExpr.getCode())
                .append(").").append(suffix.substring(1))
                .append(";\n");
        return new OllirExprResult(temp + suffix, code.toString());
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) {
            visit(child);
        }
        return OllirExprResult.EMPTY;
    }
}