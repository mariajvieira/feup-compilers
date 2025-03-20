
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
STATIC : 'static' ;
VOID : 'void' ;
NEW : 'new' ;
TRUE : 'true' ;
FALSE : 'false' ;
THIS : 'this' ;
STRING : 'String' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT qualifiedName ';'
    ;

qualifiedName
    : ID ('.' ID)*
    ;

classDecl
    : CLASS name=ID (EXTENDS superClass=ID)?
      '{' varDecl* methodDecl* '}'
    ;

varDecl
    : type name=ID ';'
    ;

type
    : name=INT ('[' ']')?       // int, int[]
    | name=BOOLEAN              // boolean
    | name=STRING ('[' ']')?    // String, String[]
    | name=ID                   // class name
    | name = VOID
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})? 
      (STATIC {$isStatic=true;})? 
      type name=ID
      '(' paramList? ')'
      '{' varDecl* stmt* '}'
    ;

paramList
    : param (',' param)*
    ;

param
    : type name=ID
    | type '...' name=ID
    ;

stmt
    : '{' stmt* '}'                           # Block
    | 'if' '(' expr ')' stmt 
      ('else' stmt)?                          # IfStmt
    | 'while' '(' expr ')' stmt               # WhileStmt
    | RETURN expr? ';'                        # ReturnStmt
    | target=expr '=' value=expr ';'          # AssignStmt
    | expr ';'                                # ExprStmt
    ;

expr
    : left=expr '||' right=expr2              # Or
    | expr2                                   # Expr2Pass
    ;

expr2
    : left=expr2 '&&' right=expr3             # And
    | expr3                                   # Expr3Pass
    ;

expr3
    : left=expr3 op=('<'|'>'|'<='|'>=') right=expr4    # Compare
    | expr4                                             # Expr4Pass
    ;

expr4
    : left=expr4 op=('+'|'-') right=expr5     # AddSub
    | expr5                                   # Expr5Pass
    ;

expr5
    : left=expr5 op=('*'|'/') right=expr6     # MulDiv
    | expr6                                   # Expr6Pass
    ;

expr6
    : '!' expr6                               # Not
    | NEW type '[' expr ']'                   # NewArray
    | NEW ID '(' ')'                          # NewObject
    | atom                                    # AtomExpr
    ;

atom
    : INTEGER                                 # Int
    | TRUE                                    # True
    | FALSE                                   # False
    | THIS                                    # This
    | name=ID                                 # Id
    | '(' expr ')'                            # Parenthesis
    | '[' arrayInit? ']'                      # ArrayLiteral
    | atom '.' ID '(' args? ')'               # MethodCall
    | atom '.' ID                             # FieldAccess
    | atom '[' expr ']'                       # ArrayAccess
    ;

args
    : expr (',' expr)*
    ;

arrayInit
    : expr (',' expr)*
    ;
