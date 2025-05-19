package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.AssignInstruction;
import org.specs.comp.ollir.inst.BinaryOpInstruction;
import org.specs.comp.ollir.inst.ReturnInstruction;
import org.specs.comp.ollir.inst.SingleOpInstruction;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;
import org.specs.comp.ollir.inst.PutFieldInstruction;
import org.specs.comp.ollir.inst.GetFieldInstruction;
import org.specs.comp.ollir.type.Type;
import org.specs.comp.ollir.inst.NewInstruction;
import org.specs.comp.ollir.inst.InvokeSpecialInstruction;



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

    private final OllirResult ollirResult;

    List<Report> reports;

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

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
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
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        var params = "I";
        var returnType = "I";

        code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(" + params + ")" + returnType).append(NL);

        // Add limits
        // FAZER: ir incrementando limite da stack a medida que adiiciona mais variaveis
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        for (var instruction : method.getInstructions()) {
            if (instruction instanceof AssignInstruction assign) {
                code.append(TAB).append(generateAssign(assign));
            } else if (instruction instanceof BinaryOpInstruction binaryOp) {
                code.append(TAB).append(generateBinaryOp(binaryOp));
            } else if (instruction instanceof ReturnInstruction returnInst) {
                code.append(TAB).append(generateReturn(returnInst));
            } else if (instruction instanceof SingleOpInstruction singleOp) {
                code.append(TAB).append(generateSingleOp(singleOp));
            } else {
                code.append(TAB).append("; unhandled instruction").append(NL);
            }
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }

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
            // non-integer literal
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

        // load values on the left and on the right
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));

        // TODO: Hardcoded for int type, needs to be expanded
        var typePrefix = "i";

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "add";
            case MUL -> "mul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(typePrefix + op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("ireturn").append(NL);

        return code.toString();
    }

    private String toDescriptor(Type type) {
        switch (type.toString()) {
            case "INT32":   return "I";
            case "BOOLEAN": return "Z";
            case "VOID":    return "V";
            default:      throw new NotImplementedException("Descriptor for " + type);
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
        code.append("new ").append(cls).append(NL);
        code.append("dup").append(NL);
        code.append("invokespecial ")
                .append(cls).append("/<init>()V")
                .append(NL);
        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction inst) {
        var code = new StringBuilder();
        for (var op : inst.getOperands()) {
            code.append(apply((TreeNode) op));
        }
        String owner = ollirResult.getOllirClass().getClassName();
        String method = inst.getMethodName();
        String paramsDesc = inst.getArguments().stream()
                .map(arg -> toDescriptor(arg.getType()))
                .collect(Collectors.joining());
        String desc = "(" + paramsDesc + ")" + toDescriptor(inst.getReturnType());
        code.append("invokespecial ")
                .append(owner).append("/").append(method)
                .append(desc).append(NL);
        return code.toString();
    }

}