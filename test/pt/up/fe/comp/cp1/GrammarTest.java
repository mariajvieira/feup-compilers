/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class GrammarTest {


    private static final String IMPORT = "importDecl";
    private static final String MAIN_METHOD = "methodDecl";
    private static final String INSTANCE_METHOD = "methodDecl";
    private static final String STATEMENT = "stmt";
    private static final String EXPRESSION = "expr";

    @Test
    public void testImportSingle() {
        TestUtils.parseVerbose("import bar;", IMPORT);
    }

    @Test
    public void testImportMulti() {
        TestUtils.parseVerbose("import bar.foo.a;", IMPORT);
    }

    @Test
    public void testClass() {
        TestUtils.parseVerbose("class Foo extends Bar {}");
    }

    @Test
    public void testVarDecls() {
        TestUtils.parseVerbose("class Foo {int a; int[] b; int c; boolean d; Bar e;}");
    }

   /*@Test
    public void testVarDeclString() {
        TestUtils.parseVerbose("String aString;", "VarDecl");
    }*/

    @Test
    public void testMainMethodEmpty() {
        TestUtils.parseVerbose("static void main(String[] args) {}", MAIN_METHOD);
    }

    @Test
    public void testInstanceMethodEmpty() {
        TestUtils.parseVerbose("int foo(int anInt, int[] anArray, boolean aBool, String aString) {return a;}",
                INSTANCE_METHOD);
    }

    @Test
    public void testInstanceMethodVarargs() {
        TestUtils.parseVerbose("int foo(int... ints) {return 0;}",
                INSTANCE_METHOD);
    }

    @Test
    public void testStmtScope() {
        TestUtils.parseVerbose("{a; b; c;}", STATEMENT);
    }

    @Test
    public void testStmtEmptyScope() {
        TestUtils.parseVerbose("{}", STATEMENT);
    }

    @Test
    public void testStmtIfElse() {
        TestUtils.parseVerbose("if(a){ifStmt1;ifStmt2;}else{elseStmt1;elseStmt2;}", STATEMENT);
    }

    @Test
    public void testStmtIfElseWithoutBrackets() {
        TestUtils.parseVerbose("if(a)ifStmt;else elseStmt;", STATEMENT);
    }

    @Test
    public void testStmtWhile() {
        TestUtils.parseVerbose("while(a){whileStmt1;whileStmt2;}", STATEMENT);
    }

    @Test
    public void testStmtWhileWithoutBrackets() {
        TestUtils.parseVerbose("while(a)whileStmt1;", STATEMENT);
    }

    @Test
    public void testStmtAssign() {
        TestUtils.parseVerbose("a=b;", STATEMENT);
    }

    @Test
    public void testStmtArrayAssign() {
        TestUtils.parseVerbose("anArray[a]=b;", STATEMENT);
    }

    @Test
    public void testExprTrue() {
        TestUtils.parseVerbose("true", EXPRESSION);
    }

    @Test
    public void testExprFalse() {
        TestUtils.parseVerbose("false", EXPRESSION);
    }

    @Test
    public void testExprThis() {
        TestUtils.parseVerbose("this", EXPRESSION);
    }

    @Test
    public void testExprId() {
        TestUtils.parseVerbose("a", EXPRESSION);
    }

    @Test
    public void testExprIntLiteral() {
        TestUtils.parseVerbose("10", EXPRESSION);
    }

    @Test
    public void testExprParen() {
        TestUtils.parseVerbose("(10)", EXPRESSION);
    }

    @Test
    public void testExprMemberCall() {
        TestUtils.parseVerbose("foo.bar(10, a, true)", EXPRESSION);
    }

    @Test
    public void testExprMemberCallChain() {
        TestUtils.parseVerbose("callee.level1().level2(false, 10).level3(true)", EXPRESSION);
    }

    @Test
    public void testExprLength() {
        TestUtils.parseVerbose("a.length", EXPRESSION);
    }

    @Test
    public void testExprLengthChain() {
        TestUtils.parseVerbose("a.length.length", EXPRESSION);
    }

    @Test
    public void testArrayAccess() {
        TestUtils.parseVerbose("a[10]", EXPRESSION);
    }

    @Test
    public void testArrayAccessChain() {
        TestUtils.parseVerbose("a[10][20]", EXPRESSION);
    }

    @Test
    public void testParenArrayChain() {
        TestUtils.parseVerbose("(a)[10]", EXPRESSION);
    }

    @Test
    public void testCallArrayAccessLengthChain() {
        TestUtils.parseVerbose("callee.foo()[10].length", EXPRESSION);
    }

    @Test
    public void testExprNot() {
        TestUtils.parseVerbose("!true", EXPRESSION);
    }

    @Test
    public void testExprNewArray() {
        TestUtils.parseVerbose("new int[!a]", EXPRESSION);
    }

    @Test
    public void testExprNewClass() {
        TestUtils.parseVerbose("new Foo()", EXPRESSION);
    }

    @Test
    public void testExprMult() {
        TestUtils.parseVerbose("2 * 3", EXPRESSION);
    }

    @Test
    public void testExprDiv() {
        TestUtils.parseVerbose("2 / 3", EXPRESSION);
    }

    @Test
    public void testExprMultChain() {
        TestUtils.parseVerbose("1 * 2 / 3 * 4", EXPRESSION);
    }

    @Test
    public void testExprAdd() {
        TestUtils.parseVerbose("2 + 3", EXPRESSION);
    }

    @Test
    public void testExprSub() {
        TestUtils.parseVerbose("2 - 3", EXPRESSION);
    }

    @Test
    public void testExprAddChain() {
        TestUtils.parseVerbose("1 + 2 - 3 + 4", EXPRESSION);
    }

    @Test
    public void testExprRelational() {
        TestUtils.parseVerbose("1 < 2", EXPRESSION);
    }

    @Test
    public void testExprRelationalChain() {
        TestUtils.parseVerbose("1 < 2 < 3 < 4", EXPRESSION);
    }

    @Test
    public void testExprLogical() {
        TestUtils.parseVerbose("1 && 2", EXPRESSION);
    }

    @Test
    public void testExprLogicalChain() {
        TestUtils.parseVerbose("1 && 2 && 3 && 4", EXPRESSION);
    }

    @Test
    public void testExprChain() {
        TestUtils.parseVerbose("1 && 2 < 3 + 4 - 5 * 6 / 7", EXPRESSION);
    }

    @Test
    public void testExprArrayInit() {
        TestUtils.parseVerbose("[10, 20, 30]", EXPRESSION);
    }


    // Given tests: 44


    // ------------------------------------------------ //
    //                  Additional tests: 29            //
    // ------------------------------------------------ //

    @Test
    public void testBooleanVarargsMethod() {
        TestUtils.parseVerbose("int foo(boolean... flags) { if(flags[0]) return 1; else return 0; }", INSTANCE_METHOD);
    }

    @Test
    public void testEmptyArrayLiteral() {
        TestUtils.parseVerbose("int[] getArray() { return []; }", INSTANCE_METHOD);
    }

    @Test
    public void testComplexExpression() {
        String expr = "((10 + 20) * (30 - 5)) / 2 && a < b || !false";
        TestUtils.parseVerbose(expr, EXPRESSION);
    }

    @Test
    public void testNewObjectAndArray() {
        String expr = "new Person()";
        TestUtils.parseVerbose(expr, EXPRESSION);
        expr = "new int[100]";
        TestUtils.parseVerbose(expr, EXPRESSION);
    }

    @Test
    public void testSimpleImport() {
        TestUtils.parseVerbose("import foo;", "importDecl");
    }

    @Test
    public void testSimpleClass() {
        TestUtils.parseVerbose("class A {}", "classDecl");
    }

    @Test
    public void testSimpleVarDecl() {
        TestUtils.parseVerbose("class A { int a; }", "classDecl");
    }

    @Test
    public void testSimpleMethod() {
        TestUtils.parseVerbose("int main() { return 0; }", "methodDecl");
    }

    @Test
    public void testSimpleExpression() {
        TestUtils.parseVerbose("1 + 2", "expr");
    }

    @Test
    public void testComplexMethodCallChainNew() {
        TestUtils.parseVerbose("a.b(10, x).c().d(e.f(3)[1]).g()", "expr");
    }

    @Test
    public void testNestedArrayLiteral() {
        TestUtils.parseVerbose("int[] init() { return [[1+2, 3*4], [5-1, 8/2]]; }", "methodDecl");
    }

    @Test
    public void testMixedOperatorsExpression() {
        TestUtils.parseVerbose("(a + b*(c - d)) / e < (f + g) && !h || i == j", "expr");
    }

    @Test
    public void testNestedBlocksAssignment() {
        TestUtils.parseVerbose("{ { x=10; y=x+5; } z=y*2; }", "stmt");
    }

    @Test
    public void testMethodReturnWithoutExpression() {
        TestUtils.parseVerbose("void doNothing() { return; }", INSTANCE_METHOD);
    }

    @Test
    public void testInstanceMethodMixedParams() {
        TestUtils.parseVerbose("int sum(int a, int b, int... extras) { return a + b; }", INSTANCE_METHOD);
    }

    @Test
    public void testNestedIfElseNoBraces() {
        TestUtils.parseVerbose("if(a==b) if(c==d) e=f; else e=g;", STATEMENT);
    }

    @Test
    public void testChainedNotOperators() {
        TestUtils.parseVerbose("!!true", EXPRESSION);
    }

    @Test
    public void testComplexArithmeticLogicalExpression() {
        TestUtils.parseVerbose("(((a+b)*c)/d - e) && f || (g < h)", EXPRESSION);
    }

    @Test
    public void testArrayAccessOfMethodCall() {
        TestUtils.parseVerbose("foo.bar()[bar].length", EXPRESSION);
    }

    @Test
    public void testComplexClassDeclaration() {
        String program = "class Complex extends Base { "
                + "int x; boolean y; "
                + "int init(int a, int b) { x = a + b * 2; y = x > 0; return x; } "
                + "}";
        TestUtils.parseVerbose(program, "classDecl");
    }

    @Test
    public void testNestedMethodCalls() {
        TestUtils.parseVerbose("obj.getA().getB().getC()", "expr");
    }

    @Test
    public void testArrayLiteralWithMethodCalls() {
        TestUtils.parseVerbose("[foo.bar(), bar.baz()]", "expr");
    }

    @Test
    public void testArithmeticLogicalExpression() {
        String expr = "1 + 2 * 3 - 4 / 2 < 10 && !false";
        TestUtils.parseVerbose(expr, "expr");
    }

    @Test
    public void testStmtComplexAssignment() {
        // Assignment where the target is an array access using nested array access.
        TestUtils.parseVerbose("a[b[0]] = 10;", "stmt");
    }

    @Test
    public void testExprNestedParentheses() {
        TestUtils.parseVerbose("((a + (b)))", "expr");
    }

    @Test
    public void testMethodWithMixedParamsAndVarargs() {
        String method = "int compute(int a, int b, int... extras) { return a + b; }";
        TestUtils.parseVerbose(method, "methodDecl");
    }

    @Test
    public void testIfWithoutElseBrackets() {
        TestUtils.parseVerbose("if(a) if(b) c; else d;", "stmt");
    }

    @Test
    public void testWhileWithBlock() {
        TestUtils.parseVerbose("while(a < b) { x = x + 1; y = y - 1; }", "stmt");
    }

    @Test
    public void testImportDeclarationWithQualifiedName() {
        TestUtils.parseVerbose("import pt.up.fe.comp.project;", "importDecl");
    }

}
