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
                continue; // Already handled default constructor, or handle custom constructors
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

        // generate class name
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

        var modifier   = types.getModifier(method.getMethodAccessModifier());
        var methodName = method.getMethodName();
        // derive parameters from OLLIR Method.params
        var params = method.getParams().stream()
                .map(p -> toDescriptor(p.getType()))
                .collect(Collectors.joining());
        // derive return type descriptor
        var returnType = toDescriptor(method.getReturnType());

        code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(").append(params).append(")")
                .append(returnType).append(NL);
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        // 1st pass...
        for (var inst : method.getInstructions()) {
            // unchanged
            var instCode = StringLines.getLines(apply(inst))
                    .stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            code.append(instCode);
            if (inst instanceof ReturnInstruction) break;
        }
        // 2nd pass...
        for (var inst : method.getInstructions()) {
            if (inst instanceof ReturnInstruction returnInst) {
                code.append(TAB).append(generateReturn(returnInst));
                break;
            }
            // unchanged
            if (inst instanceof AssignInstruction assign) {
                code.append(TAB).append(generateAssign(assign));
            } else if (inst instanceof BinaryOpInstruction binOp) {
                code.append(TAB).append(generateBinaryOp(binOp));
            } else if (inst instanceof SingleOpInstruction singleOp) {
                code.append(TAB).append(generateSingleOp(singleOp));
            } else {
                break;
            }
        }

        code.append(".end method\n");
        currentMethod = null;
        return code.toString();
    }



//
//    private String generateMethod(Method method) {
//        //System.out.println("STARTING METHOD " + method.getMethodName());
//        // set method
//        currentMethod = method;
//
//        var code = new StringBuilder();
//
//        // calculate modifier
//        var modifier = types.getModifier(method.getMethodAccessModifier());
//
//        var methodName = method.getMethodName();
//
//        // TODO: Hardcoded param types and return type, needs to be expanded
//        var params = "I";
//        var returnType = "I";
//
//        code.append("\n.method ").append(modifier)
//                .append(methodName)
//                .append("(" + params + ")" + returnType).append(NL);
//
//        // Add limits
//        // FAZER: ir incrementando limite da stack a medida que adiiciona mais variaveis
//        code.append(TAB).append(".limit stack 99").append(NL);
//        code.append(TAB).append(".limit locals 99").append(NL);
//
//        for (var inst : method.getInstructions()) {
//            var instCode = StringLines.getLines(apply(inst)).stream()
//                    .collect(Collectors.joining(NL + TAB, TAB, NL));
//
//            code.append(instCode);
//        }
//
//        for (var instruction : method.getInstructions()) {
//            if (instruction instanceof AssignInstruction assign) {
//                code.append(TAB).append(generateAssign(assign));
//            } else if (instruction instanceof BinaryOpInstruction binaryOp) {
//                code.append(TAB).append(generateBinaryOp(binaryOp));
//            } else if (instruction instanceof ReturnInstruction returnInst) {
//                code.append(TAB).append(generateReturn(returnInst));
//            } else if (instruction instanceof SingleOpInstruction singleOp) {
//                code.append(TAB).append(generateSingleOp(singleOp));
//            } else {
//                code.append(TAB).append("; unhandled instruction").append(NL);
//            }
//        }
//
//        code.append(".end method\n");
//
//        // unset method
//        currentMethod = null;
//        //System.out.println("ENDING METHOD " + method.getMethodName());
//        return code.toString();
//    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        code.append(apply(assign.getRhs()));
        var op    = (Operand) assign.getDest();
        var desc  = currentMethod.getVarTable().get(op.getName());
        int reg   = desc.getVirtualReg();
        boolean isInt = op.getType().toString().equals("INT32");
        String instr;
        if (isInt) {
            switch (reg) {
                case 0: instr = "istore_0"; break;
                case 1: instr = "istore_1"; break;
                case 2: instr = "istore_2"; break;
                case 3: instr = "istore_3"; break;
                default: instr = "istore " + reg; break;
            }
        } else {
            switch (reg) {
                case 0: instr = "astore_0"; break;
                case 1: instr = "astore_1"; break;
                case 2: instr = "astore_2"; break;
                case 3: instr = "astore_3"; break;
                default: instr = "astore " + reg; break;
            }
        }
        code.append(instr).append(NL);
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
                String code = (val == -1) ? "iconst_m1" : "iconst_" + val;
                return code + NL;
            }
            if (val >= -128 && val <= 127) {
                return "bipush " + val + NL;
            }
            if (val >= -32768 && val <= 32767) {
                return "sipush " + val + NL;
            }
        } catch (NumberFormatException e) {
            // Non-numeric literal, add quotes if necessary
            if (!(lit.startsWith("\"") && lit.endsWith("\""))) {
                lit = "\"" + lit + "\"";
            }
        }
        return "ldc " + lit + NL;
    }

    private String generateOperand(Operand operand) {
        var desc = currentMethod.getVarTable().get(operand.getName());
        int reg = desc.getVirtualReg();
        boolean isInt = operand.getType().toString().equals("INT32");
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

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("ireturn").append(NL);

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
        String cls = inst.getReturnType().toString();
        if (cls.startsWith("OBJECTREF(") && cls.endsWith(")")) {
            cls = cls.substring(10, cls.length() - 1);
        }
        code.append("new ").append(cls).append(NL);
        code.append("dup").append(NL);
        code.append("invokespecial ").append(cls).append("/<init>()V").append(NL);
        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction inst) {
        var code = new StringBuilder();
        for (var op : inst.getOperands()) {
            code.append(apply((TreeNode) op));
        }
        String owner = ollirResult.getOllirClass().getClassName();
        String method = inst.getMethodName().toString();
        String paramsDesc = inst.getArguments().stream()
                .map(arg -> toDescriptor(arg.getType()))
                .collect(Collectors.joining());
        String desc = "(" + paramsDesc + ")" + toDescriptor(inst.getReturnType());
        code.append("invokespecial ")
                .append(owner).append("/").append(method)
                .append(desc).append(NL);
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
        var code = new StringBuilder();
        var operands = inst.getOperands();

        var classOp = (Operand) operands.get(0);
        String owner = classOp.getName();

        for (var op : operands.subList(1, operands.size())) {
            code.append(apply((TreeNode) op));
        }
        Element methodNameElement = inst.getMethodName();
        String methodNameString;

        if (methodNameElement instanceof LiteralElement) {
            methodNameString = ((LiteralElement) methodNameElement).getLiteral();
            if (methodNameString.startsWith("\"") && methodNameString.endsWith("\"")) {
                methodNameString = methodNameString.substring(1, methodNameString.length() - 1);
            }
        } else if (methodNameElement instanceof Operand) {
            methodNameString = ((Operand) methodNameElement).getName();
        } else {

            methodNameString = methodNameElement.toString();
        }

        String paramsDescriptor = inst.getArguments().stream()
                .map(arg -> toDescriptor(arg.getType()))
                .collect(Collectors.joining());
        String returnTypeDescriptor = toDescriptor(inst.getReturnType());
        String methodDescriptor = "(" + paramsDescriptor + ")" + returnTypeDescriptor;

        code.append("invokestatic ")
                .append(owner).append("/")
                .append(methodNameString)
                .append(methodDescriptor)
                .append(NL);

        return code.toString();
    }

}