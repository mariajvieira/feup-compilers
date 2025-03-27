package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.specs.util.SpecsIo;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Test variable lookup.
 */
public class SymbolTableTest {

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/" + filename));
    }

    static JmmSemanticsResult test(String filename, boolean fail) {
        var semantics = getSemanticsResult(filename);
        if (fail) {
            TestUtils.mustFail(semantics.getReports());
        } else {
            TestUtils.noErrors(semantics.getReports());
        }
        return semantics;
    }


    /**
     * Test if fields are not being accessed from static methods.
     */
    @Test
    public void NumImports() {
        var semantics = test("symboltable/Imports.jmm", false);
        assertEquals(2, semantics.getSymbolTable().getImports().size());
    }

    @Test
    public void ClassAndSuper() {
        var semantics = test("symboltable/Super.jmm", false);
        assertEquals("Super", semantics.getSymbolTable().getClassName());
        assertEquals("UltraSuper", semantics.getSymbolTable().getSuper());

    }

    @Test
    public void Fields() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(3, fields.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;

        for (var f : fields) {
            switch (f.getType().getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    break;
                case "boolean":
                    checkBool++;
                    break;
                case "int":
                    checkInt++;
                    break;
            }
        }
        ;
        assertEquals("Field of type int", 1, checkInt);
        assertEquals("Field of type boolean", 1, checkBool);
        assertEquals("Field of type object", 1, checkObj);

    }

    @Test
    public void Methods() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(5, methods.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;
        var checkAll = 0;

        for (var m : methods) {
            var ret = st.getReturnType(m);
            var numParameters = st.getParameters(m).size();
            switch (ret.getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "boolean":
                    checkBool++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "int":
                    if (ret.isArray()) {
                        checkAll++;
                        assertEquals("Method " + m + " parameters", 3, numParameters);
                    } else {
                        checkInt++;
                        assertEquals("Method " + m + " parameters", 0, numParameters);
                    }
                    break;

            }
        }
        ;
        assertEquals("Method with return type int", 1, checkInt);
        assertEquals("Method with return type boolean", 1, checkBool);
        assertEquals("Method with return type object", 1, checkObj);
        assertEquals("Method with three arguments", 1, checkAll);


    }

    @Test
    public void Parameters() {
        var semantics = test("symboltable/Parameters.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(1, methods.size());

        var parameters = st.getParameters(methods.get(0));
        assertEquals(3, parameters.size());
        assertEquals("Parameter 1", "int", parameters.get(0).getType().getName());
        assertEquals("Parameter 2", "boolean", parameters.get(1).getType().getName());
        assertEquals("Parameter 3", "Parameters", parameters.get(2).getType().getName());
    }



    // ---------------------------------------------//
    //              ADDITIONAL TESTS                //
    // ---------------------------------------------//

    @Test
    public void testMultipleMethodsSymbolTable() {
        var semantics = test("symboltable/MultipleMethods.jmm", false);
        assertEquals("MultipleMethods", semantics.getSymbolTable().getClassName());
        assertEquals(1, semantics.getSymbolTable().getFields().size());
        List<String> methods = semantics.getSymbolTable().getMethods();
        assertTrue(methods.contains("method1"));
        assertTrue(methods.contains("method2"));
        List<Symbol> paramsMethod1 = semantics.getSymbolTable().getParameters("method1");
        assertEquals(1, paramsMethod1.size());
        List<Symbol> paramsMethod2 = semantics.getSymbolTable().getParameters("method2");
        assertEquals(2, paramsMethod2.size());
    }

    @Test
    public void testNoMethodSymbolTable() {
        var semantics = test("symboltable/NoMethod.jmm", false);
        assertEquals("NoMethod", semantics.getSymbolTable().getClassName());
        assertEquals(2, semantics.getSymbolTable().getFields().size());
        assertTrue(semantics.getSymbolTable().getMethods().isEmpty());
    }

    @Test
    public void testSymbolTableAttributes() {
        var semantics = test("symboltable/NoMethod.jmm", false);
        var symbolTable = semantics.getSymbolTable();
        symbolTable.putObject("TestKey", "TestValue");
        assertEquals("TestValue", symbolTable.getObject("TestKey"));
    }

    @Test
    public void fullSymbolTableImportsTest() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();

        System.out.println("-----------------\n----------------\nDEBUG: Imports in Symbol Table: " + st.getImports()+ "\n----------------\n-----------------");

        assertEquals(1, st.getImports().size());
        assertTrue(st.getImports().contains("io"));
    }

    @Test
    public void fullSymbolTableDeclaredClassTest() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        assertEquals("FullTest", st.getClassName());
    }

    @Test
    public void fullSymbolTableFieldsTest() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<Symbol> fields = st.getFields();
        assertEquals(3, fields.size());
        var field1 = fields.stream().filter(s -> s.getName().equals("field1")).findFirst().orElse(null);
        var field2 = fields.stream().filter(s -> s.getName().equals("field2")).findFirst().orElse(null);
        var fieldArr = fields.stream().filter(s -> s.getName().equals("fieldArr")).findFirst().orElse(null);
        assertNotNull(field1);
        assertEquals("int", field1.getType().getName());
        assertFalse(field1.getType().isArray());
        assertNotNull(field2);
        assertEquals("boolean", field2.getType().getName());
        assertFalse(field2.getType().isArray());
        assertNotNull(fieldArr);
        assertEquals("int", fieldArr.getType().getName());
        assertTrue(fieldArr.getType().isArray());
    }

    @Test
    public void fullSymbolTableMethodsTest() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<String> methods = st.getMethods();
        assertEquals(2, methods.size());
        assertTrue(methods.contains("method1"));
        assertTrue(methods.contains("method2"));

        Type retMethod1 = st.getReturnType("method1");
        assertEquals("int", retMethod1.getName());
        assertFalse(retMethod1.isArray());
        List<Symbol> paramsMethod1 = st.getParameters("method1");
        assertEquals(2, paramsMethod1.size());
        var paramA = paramsMethod1.stream().filter(s -> s.getName().equals("a")).findFirst().orElse(null);
        var paramB = paramsMethod1.stream().filter(s -> s.getName().equals("b")).findFirst().orElse(null);
        assertNotNull(paramA);
        assertEquals("int", paramA.getType().getName());
        assertNotNull(paramB);
        assertEquals("boolean", paramB.getType().getName());

        Type retMethod2 = st.getReturnType("method2");
        assertEquals("boolean", retMethod2.getName());
        List<Symbol> paramsMethod2 = st.getParameters("method2");
        assertEquals(1, paramsMethod2.size());
        var paramC = paramsMethod2.stream().filter(s -> s.getName().equals("c")).findFirst().orElse(null);
        assertNotNull(paramC);
        assertEquals("boolean", paramC.getType().getName());
    }

    @Test
    public void fullSymbolTableLocalsTest() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<Symbol> localsMethod1 = st.getLocalVariables("method1");
        assertEquals(2, localsMethod1.size());
        var local1 = localsMethod1.stream().filter(s -> s.getName().equals("local1")).findFirst().orElse(null);
        var local2 = localsMethod1.stream().filter(s -> s.getName().equals("local2")).findFirst().orElse(null);
        assertNotNull(local1);
        assertEquals("int", local1.getType().getName());
        assertNotNull(local2);
        assertEquals("boolean", local2.getType().getName());

        List<Symbol> localsMethod2 = st.getLocalVariables("method2");
        assertEquals(1, localsMethod2.size());
        var local3 = localsMethod2.stream().filter(s -> s.getName().equals("local3")).findFirst().orElse(null);
        assertNotNull(local3);
        assertEquals("boolean", local3.getType().getName());
    }
    @Test
    public void testVarargsMethod() {
        var semantics = test("symboltable/Varargs.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertTrue(methods.contains("varargs"));

        List<Symbol> parameters = st.getParameters("varargs");
        assertEquals(1, parameters.size());
        assertTrue(parameters.get(0).getType().isArray());
        assertEquals("int", parameters.get(0).getType().getName());
    }

    @Test
    public void testEmptyClass() {
        var semantics = test("symboltable/EmptyClass.jmm", false);
        var st = semantics.getSymbolTable();
        assertTrue(st.getImports().isEmpty());
        assertEquals("EmptyClass", st.getClassName());
        assertNull(st.getSuper());
        assertTrue(st.getFields().isEmpty());
        assertTrue(st.getMethods().isEmpty());
    }


    @Test
    public void testMethodReturnTypes() {
        var semantics = test("symboltable/ReturnTypes.jmm", false);
        var st = semantics.getSymbolTable();

        assertEquals(new Type("void", false), st.getReturnType("voidMethod"));
        assertEquals(new Type("int", true), st.getReturnType("arrayMethod"));
        assertEquals(new Type("ReturnTypes", false), st.getReturnType("objectMethod"));
    }

    @Test
    public void testDeclaredClass() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        assertEquals("Declared class name", "FullTest", st.getClassName());
        assertNull("Super class should be null", st.getSuper());
    }

    @Test
    public void testFields() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<Symbol> fields = st.getFields();
        assertEquals("Number of fields", 3, fields.size());

        var field1 = fields.stream().filter(s -> s.getName().equals("field1")).findFirst().orElse(null);
        assertNotNull("Field field1 is missing", field1);
        assertEquals("Type for field1", "int", field1.getType().getName());
        assertFalse("field1 is not an array", field1.getType().isArray());

        var field2 = fields.stream().filter(s -> s.getName().equals("field2")).findFirst().orElse(null);
        assertNotNull("Field field2 is missing", field2);
        assertEquals("Type for field2", "boolean", field2.getType().getName());
        assertFalse("field2 is not an array", field2.getType().isArray());

        var fieldArr = fields.stream().filter(s -> s.getName().equals("fieldArr")).findFirst().orElse(null);
        assertNotNull("Field fieldArr is missing", fieldArr);
        assertEquals("Type for fieldArr", "int", fieldArr.getType().getName());
        assertTrue("fieldArr is an array", fieldArr.getType().isArray());
    }

    @Test
    public void testMethodsAndParameters() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<String> methods = st.getMethods();
        assertEquals("Number of methods", 2, methods.size());
        assertTrue("Method method1 not found", methods.contains("method1"));
        assertTrue("Method method2 not found", methods.contains("method2"));

        Type retMethod1 = st.getReturnType("method1");
        assertEquals("Return type of method1", "int", retMethod1.getName());
        assertFalse("Return type of method1 should not be an array", retMethod1.isArray());
        List<Symbol> paramsMethod1 = st.getParameters("method1");
        assertEquals("Number of parameters for method1", 2, paramsMethod1.size());
        var paramA = paramsMethod1.stream().filter(s -> s.getName().equals("a")).findFirst().orElse(null);
        var paramB = paramsMethod1.stream().filter(s -> s.getName().equals("b")).findFirst().orElse(null);
        assertNotNull("Parameter a for method1 missing", paramA);
        assertEquals("Parameter a type", "int", paramA.getType().getName());
        assertNotNull("Parameter b for method1 missing", paramB);
        assertEquals("Parameter b type", "boolean", paramB.getType().getName());

        Type retMethod2 = st.getReturnType("method2");
        assertEquals("Return type of method2", "boolean", retMethod2.getName());
        List<Symbol> paramsMethod2 = st.getParameters("method2");
        assertEquals("Number of parameters for method2", 1, paramsMethod2.size());
        var paramC = paramsMethod2.stream().filter(s -> s.getName().equals("c")).findFirst().orElse(null);
        assertNotNull("Parameter c for method2 missing", paramC);
        assertEquals("Parameter c type", "boolean", paramC.getType().getName());
    }

    @Test
    public void testLocals() {
        var st = test("symboltable/FullTest.jmm", false).getSymbolTable();
        List<Symbol> localsMethod1 = st.getLocalVariables("method1");
        assertEquals("Number of locals for method1", 2, localsMethod1.size());
        var local1 = localsMethod1.stream().filter(s -> s.getName().equals("local1")).findFirst().orElse(null);
        var local2 = localsMethod1.stream().filter(s -> s.getName().equals("local2")).findFirst().orElse(null);
        assertNotNull("Local local1 for method1 missing", local1);
        assertEquals("Type for local1", "int", local1.getType().getName());
        assertNotNull("Local local2 for method1 missing", local2);
        assertEquals("Type for local2", "boolean", local2.getType().getName());

        List<Symbol> localsMethod2 = st.getLocalVariables("method2");
        assertEquals("Number of locals for method2", 1, localsMethod2.size());
        var local3 = localsMethod2.stream().filter(s -> s.getName().equals("local3")).findFirst().orElse(null);
        assertNotNull("Local local3 for method2 missing", local3);
        assertEquals("Type for local3", "boolean", local3.getType().getName());
    }

    @Test
    public void testEmptyClassSymbolTable() {
        var st = test("symboltable/EmptyClass.jmm", false).getSymbolTable();
        assertTrue("Imports should be empty", st.getImports().isEmpty());
        assertEquals("Class name mismatch", "EmptyClass", st.getClassName());
        assertNull("Super should be null", st.getSuper());
        assertTrue("Fields should be empty", st.getFields().isEmpty());
        assertTrue("Methods should be empty", st.getMethods().isEmpty());
    }

    @Test
    public void testVarargsParameterExtended() {
        var st = test("symboltable/Varargs.jmm", false).getSymbolTable();
        assertTrue("Method 'varargs' should be present", st.getMethods().contains("varargs"));
        List<Symbol> parameters = st.getParameters("varargs");
        assertEquals("Method 'varargs' must have one parameter", 1, parameters.size());
        Type varargType = parameters.get(0).getType();
        assertTrue("Parameter should be an array", varargType.isArray());
        assertEquals("Parameter type should be int", "int", varargType.getName());
    }

    @Test
    public void testMultipleNestedLocals() {
        var st = test("symboltable/MultipleLocals.jmm", false).getSymbolTable();
        List<Symbol> locals = st.getLocalVariables("compute");
        assertEquals("Method 'compute' should have three locals", 3, locals.size());
        assertNotNull("Local 'temp' is missing",
                locals.stream().filter(s -> s.getName().equals("temp")).findFirst().orElse(null));
        assertNotNull("Local 'result' is missing",
                locals.stream().filter(s -> s.getName().equals("result")).findFirst().orElse(null));
        assertNotNull("Local 'flag' is missing",
                locals.stream().filter(s -> s.getName().equals("flag")).findFirst().orElse(null));
    }

    @Test
    public void testFieldArrayTypes() {
        var st = test("symboltable/FieldsArrays.jmm", false).getSymbolTable();
        List<Symbol> fields = st.getFields();
        var numbers = fields.stream().filter(f -> f.getName().equals("numbers")).findFirst().orElse(null);
        var booleans = fields.stream().filter(f -> f.getName().equals("booleans")).findFirst().orElse(null);
        var name = fields.stream().filter(f -> f.getName().equals("name")).findFirst().orElse(null);

        assertNotNull("Field 'numbers' is missing", numbers);
        assertTrue("Field 'numbers' should be an array", numbers.getType().isArray());
        assertEquals("Field 'numbers' type should be int", "int", numbers.getType().getName());

        assertNotNull("Field 'booleans' is missing", booleans);
        assertTrue("Field 'booleans' should be an array", booleans.getType().isArray());
        assertEquals("Field 'booleans' type should be boolean", "boolean", booleans.getType().getName());

        assertNotNull("Field 'name' is missing", name);
        assertFalse("Field 'name' should not be an array", name.getType().isArray());
        assertEquals("Field 'name' type should be String", "String", name.getType().getName());
    }

    @Test
    public void testMethodReturnTypesExtended() {
        var st = test("symboltable/ReturnTypes.jmm", false).getSymbolTable();
        Type voidType = st.getReturnType("voidMethod");
        Type arrayType = st.getReturnType("arrayMethod");
        Type objectType = st.getReturnType("objectMethod");

        assertEquals("voidMethod should return void", new Type("void", false), voidType);
        assertEquals("arrayMethod should return int array", new Type("int", true), arrayType);
        assertEquals("objectMethod should return ReturnTypes", new Type("ReturnTypes", false), objectType);
    }



    @Test
    public void testTwoImportsUsed() {
        var semantics = test("symboltable/TwoImports.jmm", false);
        var st = semantics.getSymbolTable();

        System.out.println("-----------------\n----------------\nDEBUG: Imports in Symbol Table: " + st.getImports()+ "\n----------------\n-----------------");

        assertEquals("There should be 2 imports", 2, st.getImports().size());
        assertTrue("Import 'io' missing", st.getImports().contains("io"));
        assertTrue("Import 'util.List' missing", st.getImports().contains("util.List"));
    }


}
