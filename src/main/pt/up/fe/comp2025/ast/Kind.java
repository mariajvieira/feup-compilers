package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Enum that mirrors the nodes that are supported by the AST.
 *
 * This enum allows to handle nodes in a safer and more flexible way that using strings with the names of the nodes.
 */
public enum Kind {
    PROGRAM,
    IMPORT_DECL,
    QUALIFIED_NAME,
    CLASS_DECL,
    VAR_DECL,         // type name=ID ';'
    VAR_DECL_STMT,    // stmt : varDecl #VarDeclStmt
    BLOCK,            // stmt : '{' stmt* '}' #Block
    IF_STMT,          // stmt : 'if' ... #IfStmt
    WHILE_STMT,       // stmt : 'while' ... #WhileStmt
    ASSIGN_STMT,      // stmt : target=expr '=' value=expr ';' #AssignStmt
    EXPR_STMT,        // stmt : expr ';' #ExprStmt
    RET_STMT,         // stmt : returnStmt #RetStmt
    TYPE,
    METHOD_DECL,
    PARAM_LIST,
    PARAM,
    ADD_SUB,          // expr : ... #AddSub
    MUL_DIV,          // expr : ... #MulDiv
    COMPARE,          // expr : ... #Compare
    EQUAL_DIFF,       // expr : ... #EqualDiff
    AND,              // expr : ... #And
    OR,               // expr : ... #Or
    NOT,              // expr : ... #Not
    PARENTHESIS,      // primary : '(' expr ')' #Parenthesis
    NEW_OBJECT,       // primary : NEW name=ID '(' ')' #NewObject
    NEW_ARRAY,        // primary : NEW name=INT '[' expr ']' #NewArray
    ARRAY_ACCESS,     // primary : primary '[' expr ']' #ArrayAccess
    LENGTH,           // primary : primary '.' 'length' #Length
    METHOD_CALL,      // primary : primary '.' methodName=ID '(' ... ')' #MethodCall
    ID,               // primary : name=ID #Id
    THIS,             // primary : name=THIS #This
    INT,              // primary : name=INTEGER #Int
    BOOLEAN,          // primary : name=(TRUE|FALSE) #Boolean
    ARRAY_LITERAL,    // primary : '[' arrayInit? ']' #ArrayLiteral
    ARRAY_INIT;




    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {
        for (Kind k : values()) {
            if (k.getNodeName().equals(kind)) return k;
        }
        throw new RuntimeException("Unknown kind '" + kind + "'");
    }

    public static List<String> toNodeName(Kind firstKind, Kind... otherKinds) {
        var nodeNames = new ArrayList<String>();
        nodeNames.add(firstKind.getNodeName());

        for(Kind kind : otherKinds) {
            nodeNames.add(kind.getNodeName());
        }

        return nodeNames;
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(getNodeName());
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
