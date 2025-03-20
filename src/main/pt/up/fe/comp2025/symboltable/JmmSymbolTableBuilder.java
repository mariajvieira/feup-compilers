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

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
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

        return new JmmSymbolTable(className, superClass, imports, fields, methods, returnTypes, params, locals);
    }

    private List<String> buildImports(JmmNode root) {
        return root.getChildren().stream()
                .filter(child -> child.getKind().equals("ImportDecl"))
                .map(importNode -> {
                    var qualifiedNameNode = importNode.getChild(0);
                    var qualifiedName = qualifiedNameNode.getChildren().stream()
                            .map(part -> part.get("name"))
                            .collect(Collectors.joining("."));
                    return qualifiedName;
                })
                .toList();
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        return classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(TypeUtils.convertType(varDecl.getChild(0)), varDecl.get("name")))
                .toList();
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var returnType = TypeUtils.convertType(method.getChild(0));
            map.put(name, returnType);
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
            var locals = method.getChildren(VAR_DECL).stream()
                    .map(varDecl -> new Symbol(TypeUtils.convertType(varDecl.getChild(0)), varDecl.get("name")))
                    .toList();
            map.put(name, locals);
        }
        return map;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }
}