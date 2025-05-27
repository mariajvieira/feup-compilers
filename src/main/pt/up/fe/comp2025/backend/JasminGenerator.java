package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;
import org.specs.comp.ollir.type.Type;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.inst.SingleOpInstruction;
import org.specs.comp.ollir.ArrayOperand;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";
    private int booleanOpCount = 0;
    private final OllirResult ollirResult;

    List<Report> reports;
    private int stackLimit;
    private int currentStack;


    String code;

    Method currentMethod;

    private final JasminUtils types;

    private final FunctionClassMap<TreeNode, String> generators;

    private int localLimit;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;


        types = new JasminUtils(ollirResult);

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(NewInstruction.class,  this::generateNewInstruction);
        generators.put(InvokeSpecialInstruction.class, this::generateInvokeSpecial);
        generators.put(SingleOpCondInstruction.class,
                n -> generateSingleOpCond((SingleOpCondInstruction) n));
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(InvokeStaticInstruction.class, this::generateInvokeStatic);
        generators.put(Operand.class, this::generateOperand);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(InvokeVirtualInstruction.class, this::generateInvokeVirtual);
        generators.put(ArrayLengthInstruction.class, this::generateArrayLength);
        generators.put(ArrayOperand.class, this::generateArrayLoad);




    }

    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);

        code.append(generators.apply(node));

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        ClassUnit classUnit = ollirResult.getOllirClass();
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(".class public ").append(classUnit.getClassName()).append("\n");
        jasminCode.append(".super ")
                .append(classUnit.getSuperClass() != null && !classUnit.getSuperClass().isEmpty() ? classUnit.getSuperClass() : "java/lang/Object")
                .append("\n\n");

        // Fields
        for (var field : classUnit.getFields()) {
            jasminCode.append(".field public ")
                    .append(field.getFieldName()).append(" ")
                    .append(toDescriptor(field.getFieldType())) // Assuming toDescriptor handles Type
                    .append("\n");
        }
        jasminCode.append("\n");

        // Default constructor
        jasminCode.append(".method public <init>()V\n");
        jasminCode.append("    aload_0\n");
        jasminCode.append("    invokespecial ")
                .append(classUnit.getSuperClass() != null && !classUnit.getSuperClass().isEmpty() ? classUnit.getSuperClass() : "java/lang/Object")
                .append("/<init>()V\n");
        jasminCode.append("    return\n");
        jasminCode.append(".end method\n\n");


        for (Method method : classUnit.getMethods()) {
            if (method.isConstructMethod()) {
                continue;
            }
            jasminCode.append(generateMethod(method));
        }
        return jasminCode.toString();

//        // This way, build is idempotent
//        if (code == null) {
//            code = apply(ollirResult.getOllirClass());
//        }
//
//        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: When you support 'extends', this must be updated
        var fullSuperClass = "java/lang/Object";

        code.append(".super ").append(fullSuperClass).append(NL);


        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """.formatted(fullSuperClass);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        currentMethod = method;
        var code = new StringBuilder();

        var modifier = types.getModifier(method.getMethodAccessModifier());
        var staticModifier = method.isStaticMethod() ? "static " : "";

        var methodName = method.getMethodName();
        var params = method.getParams().stream()
                .map(p -> toDescriptor(p.getType()))
                .collect(Collectors.joining());

        var returnType = toDescriptor(method.getReturnType());

        code.append("\n.method ").append(modifier).append(staticModifier)
                .append(methodName).append("(").append(params).append(")")
                .append(returnType).append(NL);

        localLimit = calculateLocalLimit(method);
        stackLimit = localLimit;

        code.append(TAB).append(".limit stack ").append(stackLimit).append(NL);
        code.append(TAB).append(".limit locals ").append(localLimit).append(NL);

        var labelMap = method.getLabels();
        var instructions = method.getInstructions();

        for (var inst : instructions) {
            for (var entry : labelMap.entrySet()) {
                if (entry.getValue().equals(inst)) {
                    code.append(TAB)
                            .append(entry.getKey())
                            .append(":")
                            .append(NL);
                }
            }
            code.append(TAB).append(apply(inst));
        }

        code.append(".end method\n");
        currentMethod = null;
        return code.toString();
    }


    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        if (assign.getDest() instanceof ArrayOperand) {
            var ac = (ArrayOperand) assign.getDest();

            int arrReg = currentMethod.getVarTable()
                    .get(ac.getName())
                    .getVirtualReg();
            code.append(arrReg <= 3
                            ? "aload_" + arrReg
                            : "aload " + arrReg)
                    .append(NL);
            code.append(apply(ac.getIndexOperands().get(0)));

            code.append(apply(assign.getRhs()));

            code.append("iastore").append(NL);
        }
        else {
            code.append(apply(assign.getRhs()));

            var destOp = (Operand) assign.getDest();
            int reg = currentMethod.getVarTable()
                    .get(destOp.getName())
                    .getVirtualReg();
            boolean isInt = destOp.getType().toString().equals("INT32")
                    || destOp.getType().toString().equals("BOOLEAN");
            String instr = isInt
                    ? (reg <= 3 ? "istore_" + reg : "istore " + reg)
                    : (reg <= 3 ? "astore_" + reg : "astore " + reg);
            code.append(instr).append(NL);
        }

        return code.toString();
    }




    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        String lit = literal.getLiteral();
        try {
            int val = Integer.parseInt(lit);
            if (val >= -1 && val <= 5) {
                return (val == -1 ? "iconst_m1" : "iconst_" + val) + "\n";
            }
            if (val >= -128 && val <= 127) {
                return "bipush " + val + "\n";
            }
            if (val >= -32768 && val <= 32767) {
                return "sipush " + val + "\n";
            }
        } catch (NumberFormatException e) {
            if (!(lit.startsWith("\"") && lit.endsWith("\""))) {
                lit = "\"" + lit + "\"";
            }
        }
        return "ldc " + lit + "\n";
    }

    private String generateOperand(Operand operand) {
        var desc = currentMethod.getVarTable().get(operand.getName());
        int reg = desc.getVirtualReg();
        boolean isInt = operand.getType().toString().equals("INT32")
                || operand.getType().toString().equals("BOOLEAN");
        if (isInt) {
            switch (reg) {
                case 0: return "iload_0" + NL;
                case 1: return "iload_1" + NL;
                case 2: return "iload_2" + NL;
                case 3: return "iload_3" + NL;
                default: return "iload " + reg + NL;
            }
        } else {
            switch (reg) {
                case 0: return "aload_0" + NL;
                case 1: return "aload_1" + NL;
                case 2: return "aload_2" + NL;
                case 3: return "aload_3" + NL;
                default: return "aload " + reg + NL;
            }
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));

        switch (binaryOp.getOperation().getOpType()) {
            case ADD   -> code.append("iadd").append(NL);
            case SUB   -> code.append("isub").append(NL);
            case MUL   -> code.append("imul").append(NL);
            case DIV   -> code.append("idiv").append(NL);
            case LTH   -> {
                int id = booleanOpCount++;
                String t = "LTH_true" + id;
                String e = "LTH_end"  + id;
                code.append("if_icmplt ").append(t).append(NL)
                        .append("iconst_0").append(NL)
                        .append("goto ").append(e).append(NL)
                        .append(t).append(":").append(NL)
                        .append("iconst_1").append(NL)
                        .append(e).append(":").append(NL);
            }
            default   -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var operandOpt = returnInst.getOperand();
        if (operandOpt.isEmpty()) {
            code.append("return").append(NL);
            return code.toString();
        }

        Element operand = operandOpt.get();
        if (operand instanceof Operand) {
            var desc = currentMethod.getVarTable().get(((Operand) operand).getName());
            int reg = desc.getVirtualReg();
            code.append("iload").append(reg == 1 ? "_1" : " " + reg).append(NL);
        }

        Type type = operand.getType();
        if (type.toString().equals("INT32") || type.toString().equals("BOOLEAN")) {
            code.append("ireturn").append(NL);
        } else {
            code.append("areturn").append(NL);
        }

        return code.toString();
    }

    private String toDescriptor(Type type) {
        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            var dims = "[".repeat(arrayType.getNumDimensions());
            return dims + toDescriptor(arrayType.getElementType());
        }
        switch (type.toString()) {
            case "INT32":   return "I";
            case "BOOLEAN": return "Z";
            case "VOID":    return "V";
            case "STRING":  return "Ljava/lang/String;";
            default:        throw new NotImplementedException("Descriptor for " + type);
        }
    }


    private String generatePutField(PutFieldInstruction inst) {
        var code = new StringBuilder();
        code.append(apply(inst.getObject()));
        code.append(apply(inst.getValue()));
        String owner      = ollirResult.getOllirClass().getClassName();
        String name       = inst.getField().getName();
        String descriptor = toDescriptor(inst.getFieldType());
        code.append("putfield ")
                .append(owner).append("/").append(name)
                .append(" ").append(descriptor)
                .append(NL);
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction inst) {
        var code = new StringBuilder();
        code.append(apply(inst.getObject()));
        String owner      = ollirResult.getOllirClass().getClassName();
        String name       = inst.getField().getName();
        String descriptor = toDescriptor(inst.getFieldType());
        code.append("getfield ")
                .append(owner).append("/").append(name)
                .append(" ").append(descriptor)
                .append(NL);
        return code.toString();
    }

    private String generateNewInstruction(NewInstruction inst) {
        var code = new StringBuilder();
        var type = inst.getReturnType();

        if (type instanceof ArrayType) {
            Element sizeElem = inst.getOperands().get(1);
            if (sizeElem instanceof Operand) {
                code.append(generateOperand((Operand) sizeElem));
            } else if (sizeElem instanceof LiteralElement) {
                code.append(generateLiteral((LiteralElement) sizeElem));
            } else {
                code.append(apply((TreeNode) sizeElem));
            }

            ArrayType arrayType = (ArrayType) type;
            String elemDesc = toDescriptor(arrayType.getElementType());
            String jasType = elemDesc.equals("I") ? "int" : elemDesc;
            code.append("newarray ").append(jasType).append(NL);
        } else {
            String cls = type.toString();
            if (cls.startsWith("OBJECTREF(") && cls.endsWith(")")) {
                cls = cls.substring(10, cls.length() - 1);
            }
            code.append("new ").append(cls).append(NL)
                    .append("dup").append(NL)
                    .append("invokespecial ").append(cls).append("/<init>()V").append(NL);
        }

        return code.toString();
    }


    private String generateInvokeSpecial(InvokeSpecialInstruction inst) {
        var code = new StringBuilder();

        if (inst.getArguments().isEmpty()) {
            code.append("aload_0\n");
        } else {
            Element objectRef = inst.getArguments().get(0);
            code.append(apply(objectRef));
        }

        String className;
        Element objectRef = inst.getArguments().isEmpty() ? null : inst.getArguments().get(0);
        if (objectRef instanceof Operand) {
            className = ((Operand) objectRef).getName();
        } else {
            className = ollirResult.getOllirClass().getClassName();
        }

        String methodName;
        Element methodNameElement = inst.getMethodName();
        if (methodNameElement instanceof LiteralElement) {
            methodName = ((LiteralElement) methodNameElement).getLiteral();
        } else {
            throw new RuntimeException("Unsupported method name type: " + methodNameElement.getClass());
        }

        code.append("invokespecial ").append(className).append("/").append(methodName).append("()V\n");

        return code.toString();
    }


    private String generateSingleOpCond(SingleOpCondInstruction inst) {
        var sb = new StringBuilder();
        sb.append(apply(inst.getOperands().get(0)));
        sb.append("ifne ").append(inst.getLabel()).append(NL);
        return sb.toString();
    }

    private String generateGoto(GotoInstruction inst) {
        return "goto " + inst.getLabel() + NL;
    }


    private String generateInvokeStatic(InvokeStaticInstruction inst) {
        var sb = new StringBuilder();
        var ops = inst.getOperands();
        var args = ops.subList(2, ops.size());
        for (var arg : args) {
            sb.append(apply(arg));
        }
        Element ownerElem = ops.get(0);
        String owner = ownerElem instanceof LiteralElement
                ? ((LiteralElement) ownerElem).getLiteral()
                : ((Operand) ownerElem).getName();

        Element mElem = inst.getMethodName();
        String mName = mElem instanceof LiteralElement
                ? ((LiteralElement) mElem).getLiteral()
                : ((Operand) mElem).getName();

        String params = args.stream()
                .map(o -> toDescriptor(o.getType()))
                .collect(Collectors.joining());

        sb.append("invokestatic ")
                .append(owner).append("/").append(mName)
                .append("(").append(params).append(")")
                .append(toDescriptor(inst.getReturnType()))
                .append(NL);

        return sb.toString();
    }


    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();

        code.append(apply(unaryOp.getOperand()));

        switch (unaryOp.getOperation().getOpType()) {
            case NOTB -> {
                int id = booleanOpCount++;
                String trueLabel = "NOTB_true_" + id;
                String endLabel = "NOTB_end_" + id;

                code.append("ifeq ").append(trueLabel).append(NL);
                code.append("iconst_0").append(NL);
                code.append("goto ").append(endLabel).append(NL);
                code.append(trueLabel).append(":").append(NL);
                code.append("iconst_1").append(NL);
                code.append(endLabel).append(":").append(NL);
            }
            default -> throw new NotImplementedException("Unary operation not implemented: " + unaryOp.getOperation().getOpType());
        }

        return code.toString();
    }


    private String generateInvokeVirtual(InvokeVirtualInstruction inst) {
        var code = new StringBuilder();

        Element objectRef = inst.getOperands().getFirst();
        code.append(apply(objectRef));

        for (int i = 1; i < inst.getOperands().size(); i++) {
            code.append(apply(inst.getOperands().get(i)));
        }

        String methodName;
        if (inst.getMethodName() instanceof LiteralElement) {
            methodName = ((LiteralElement) inst.getMethodName()).getLiteral();
        } else if (inst.getMethodName() instanceof Operand) {
            methodName = ((Operand) inst.getMethodName()).getName();
        } else {
            throw new NotImplementedException("Unsupported method name type: " + inst.getMethodName().getClass());
        }

        String className;
        if (objectRef instanceof Operand) {
            className = ((Operand) objectRef).getName();
        } else if (objectRef instanceof LiteralElement) {
            className = ((LiteralElement) objectRef).getLiteral();
        } else {
            throw new NotImplementedException("Unsupported object reference type: " + objectRef.getClass());
        }

        String descriptor = "(" + inst.getOperands().subList(1, inst.getOperands().size()).stream()
                .map(arg -> toDescriptor(arg.getType()))
                .collect(Collectors.joining()) + ")" + toDescriptor(inst.getReturnType());

        code.append("invokevirtual ").append(className).append("/").append(methodName)
                .append(descriptor).append(NL);

        return code.toString();
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInst) {
        var code = new StringBuilder();

        var arrayRef = arrayLengthInst.getOperands().get(0);
        code.append(apply(arrayRef));

        code.append("arraylength").append(NL);

        return code.toString();
    }

    private int calculateLocalLimit(Method method) {
        return method.getVarTable().size();
    }

    private String generateArrayLoad(ArrayOperand ac) {
        var code = new StringBuilder();
        int arrReg = currentMethod.getVarTable()
                .get(ac.getName())
                .getVirtualReg();
        code.append(arrReg <= 3
                        ? "aload_" + arrReg
                        : "aload " + arrReg)
                .append(NL);
        code.append(apply(ac.getIndexOperands().get(0)));
        code.append("iaload").append(NL);
        return code.toString();
    }



}