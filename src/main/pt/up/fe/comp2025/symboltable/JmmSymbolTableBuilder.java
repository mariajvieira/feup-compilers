package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;


public class JmmSymbolTableBuilder {
    private List<Report> reports;
    private String currentMethod;

    public List<Report> getReports() {
        return reports;
    }

    public JmmSymbolTable build(JmmNode root) {
        reports = new ArrayList<>();

        var classDecl = root.getChildren().stream()
                .filter(node -> Kind.CLASS_DECL.check(node))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Class declaration not found"));

        var imports = buildImports(root);
        String className = classDecl.get("name");
        String superClass = classDecl.hasAttribute("superClass") ? classDecl.get("superClass") : null;

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        // Check variable declarations in method bodies
        for (JmmNode node : classDecl.getChildren()) {
            if (node.getKind().equals("MethodDecl")) {
                currentMethod = node.get("name");
                checkNode(node, params.get(currentMethod), locals.get(currentMethod), fields);
            }
        }

        return new JmmSymbolTable(className, superClass, imports, fields, methods, returnTypes, params, locals);
    }

    private void checkNode(JmmNode node, List<Symbol> methodParams, List<Symbol> localVars, List<Symbol> fields) {
        // Check for undeclared variables
        if (node.getKind().equals("Id")) {
            String varName = node.get("name");
            boolean isDeclared = false;

            if (methodParams != null) {
                isDeclared = methodParams.stream().anyMatch(p -> p.getName().equals(varName));
            }
            if (!isDeclared && localVars != null) {
                isDeclared = localVars.stream().anyMatch(l -> l.getName().equals(varName));
            }
            if (!isDeclared) {
                isDeclared = fields.stream().anyMatch(f -> f.getName().equals(varName));
            }

            if (!isDeclared) {
                reports.add(Report.newError(
                        Stage.SEMANTIC,
                        node.getLine(),
                        node.getColumn(),
                        "Variable \\ +varName+ \\  not declared",
                        null
                ));
            }
        }

        for (JmmNode child : node.getChildren()) {
            checkNode(child, methodParams, localVars, fields);
        }
    }



    private List<String> buildImports(JmmNode root) {
        List<String> imports = new ArrayList<>();
        for (JmmNode child : root.getChildren()) {
            if (child.getKind().equals("ImportDecl")) {
                JmmNode qualifiedNameNode = child.getChildren().getFirst();
                List<String> nameParts = qualifiedNameNode.getOptionalObject("name")
                        .map(obj -> (List<String>) obj)
                        .orElse(new ArrayList<>());
                String importName = String.join(".", nameParts);
                imports.add(importName);
            }
        }
        return imports;
    }


    private String getNodeText(JmmNode node) {
        String text = node.get("name");
        return text != null ? text : node.toString();
    }


    private List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> {
                    var typeNode = varDecl.getChild(0);
                    var type = TypeUtils.convertType(typeNode);
                    return new Symbol(type, varDecl.get("name"));
                })
                .collect(Collectors.toList());
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var methodName = method.get("name");
            var returnTypeNode = method.getChild(0); // O primeiro filho deve ser o tipo de retorno
            var returnType = TypeUtils.convertType(returnTypeNode);
            map.put(methodName, returnType);
        }

        return map;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var name = method.get("name");
            List<Symbol> params = new ArrayList<>();
            var paramListOpt = method.getChildren().stream()
                    .filter(child -> child.getKind().equals("ParamList"))
                    .findFirst();

            if (paramListOpt.isPresent()) {
                var paramList = paramListOpt.get();
                var paramNodes = paramList.getChildren();
                params = paramNodes.stream()
                        .map(param -> {
                            var typeNode = param.getChild(0);
                            var type = TypeUtils.convertType(typeNode);
                            var paramName = param.get("name");
                            return new Symbol(type, paramName);
                        })
                        .collect(Collectors.toList());
            }

            map.put(name, params);
        }
        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            List<Symbol> locals = new ArrayList<>();
            collectLocalVars(method, locals);
            map.put(name, locals);
        }
        return map;
    }

    private void collectLocalVars(JmmNode node, List<Symbol> locals) {
        if (node.getKind().equals("VarDecl")) {
            String varName = node.get("name");
            // Only add the variable if it hasn't been declared in an outer/nested scope already
            boolean alreadyDeclared = locals.stream().anyMatch(s -> s.getName().equals(varName));
            if (!alreadyDeclared) {
                locals.add(new Symbol(TypeUtils.convertType(node.getChild(0)), varName));
            }
        }
        for (JmmNode child : node.getChildren()) {
            collectLocalVars(child, locals);
        }
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }
}