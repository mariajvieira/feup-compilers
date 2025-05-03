package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2025.CompilerConfig;
import pt.up.fe.comp2025.ConfigOptions;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        System.out.println(semanticsResult.getRootNode().toTree());

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());
        var result    = new OllirResult(semanticsResult, ollirCode, Collections.emptyList());

        System.out.println("\nOLLIR:\n\n" + ollirCode);
        result = optimize(result);

        return result;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        if (CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            var optimizer = new AstOptimizerVisitor();

            do {
                optimizer.resetOptimized();
                optimizer.visit(semanticsResult.getRootNode());
                System.out.println("optimized ast:\n" + semanticsResult.getRootNode().toTree());

            } while (optimizer.hasOptimized());
        }
        return semanticsResult;
    }



    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // Only perform register allocation if the "-r" flag appears
        var config = ollirResult.getConfig();
        if (!config.containsKey(ConfigOptions.getRegister())) {
            return ollirResult;
        }

        int maxRegs = Integer.parseInt(config.get(ConfigOptions.getRegister()));
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            var allocation = RegisterAllocator.allocate(method, maxRegs);
            allocation.forEach((varName, reg) -> {
                var descriptor = method.getVarTable().get(varName);
                if (descriptor != null) {
                    descriptor.setVirtualReg(reg);
                }
            });
        }

        return ollirResult;
    }


}
