package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit("Id",                           this::visitVarRefExpr);
        addVisit("importDecl",                   (n, t) -> null);
        addVisit(Kind.VAR_DECL.getNodeName(),    (n, t) -> null);
        addVisit(Kind.PARAM.getNodeName(),       (n, t) -> null);

        setDefaultVisit((node, table) -> {
            for (var child : node.getChildren())
                visit(child, table);
            return null;
        });
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String name = varRefExpr.get("name");

        // Skip literals and 'this'
        if (name.equals("true") || name.equals("false") || name.equals("this"))
            return null;

        // Skip imported types (e.g., io)
        boolean isImport = table.getImports().stream()
                .anyMatch(imp -> imp.equals(name) || imp.endsWith("." + name));
        if (isImport)
            return null;

        // Skip fields
        if (table.getFields().stream().anyMatch(f -> f.getName().equals(name)))
            return null;

        // Skip parameters or locals in current method
        if (currentMethod != null) {
            boolean isParam = table.getParameters(currentMethod)
                    .stream().anyMatch(p -> p.getName().equals(name));
            boolean isLocal = table.getLocalVariables(currentMethod)
                    .stream().anyMatch(v -> v.getName().equals(name));
            if (isParam || isLocal)
                return null;
        }

        addReport(Report.newError(
                Stage.SEMANTIC,
                varRefExpr.getLine(),  varRefExpr.getColumn(),
                "Variable " + name + " not declared",
                null
        ));
        return null;
    }


}
