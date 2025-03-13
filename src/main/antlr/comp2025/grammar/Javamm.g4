grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
EXTENDS : 'extends' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
IMPORT : 'import' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;

importDecl
    : IMPORT qualifiedName ';'
    ;

qualifiedName
    : ID ('.' ID)*
    ;

classDecl
    : CLASS name=ID
        ('extends' superClass=ID)?
        '{'
        varDecl* methodDecl*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type
    : INT ('[' ']')?       // int, int[]
    | 'boolean'            // boolean
    | ID                  // class name
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        '(' param ')'
        '{' varDecl* stmt* '}'
    ;

param
    : type name=ID
    ;

stmt
    : '{' stmt* '}'                           # Block
    | 'if' '(' cond=expr ')' stmt ( 'else' stmt )?  # If
    | 'while' '(' cond=expr ')' stmt             # While
    ;

expr
    : left=expr '||' right=expr1    # LogicalOr
    | expr1                        # Expr1Pass
    ;

expr1
    : left=expr1 '&&' right=expr2   # LogicalAnd
    | exprRel                        # ExprRelPass
    ;

exprRel
    : left=exprRel op=('<' | '>' | '<=' | '>=') right=expr2  # Relational
    | expr2                                                 # Expr2Pass
    ;

expr2
    : left=expr2 op=('*'|'/') right=expr3  # MulDiv
    | expr3                                # EXpr3Pass
    ;

expr3
    : left=expr3 op=('+'|'-') right=unary  # AddSub
    | unary                                # UnaryPass
    ;

unary
    : op='!' operand=unary               # Negate
    | methodAccess                       # UnaryAccess
    | 'new' baseType=type '(' expr* ')'  # NewClassInstance
    | 'new' baseType=type '[' expr ']'   # NewArray
    ;

primary
    : value=INTEGER                     # IntegerLiteral
    | name=ID                           # VarRef
    | '(' expr ')'                      # ParenthesizedExpr
    | '[' arrayExpr ']'                  # ArrayLiteral
    ;

arrayExpr
    : expr (',' expr)*                    # ArrayInit
    ;

methodAccess
    : primary (methodSuffix)*             # MethodCallChain
    ;

methodSuffix
    : '.' method=ID '(' exprList? ')'     # MethodCall
    | '.' field=ID                        # FieldAccess
    | '[' expr ']'                        # ArrayAccess
    ;

exprList
    : expr (',' expr)*                    # ArgumentList
    ;
