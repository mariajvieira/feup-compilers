package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

public class JmmSymbolTable extends AJmmSymbolTable {

    private final String className;
    private final String superClass;
    private final List<String> imports;
    private final List<Symbol> fields;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;

    public JmmSymbolTable(String className,
                          String superClass,
                          List<String> imports,
                          List<Symbol> fields,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals) {
        this.className = className;
        this.superClass = superClass;
        this.imports = imports;
        this.fields = fields;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.getOrDefault(methodSignature, TypeUtils.newIntType());
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.getOrDefault(methodSignature, Collections.emptyList());
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.getOrDefault(methodSignature, Collections.emptyList());
    }

    @Override
    public String toString() {
        return "Class: " + className + "\n" +
                "Super: " + superClass + "\n" +
                "Imports: " + imports + "\n" +
                "Fields: " + fields + "\n" +
                "Methods: " + methods + "\n";
    }
}
