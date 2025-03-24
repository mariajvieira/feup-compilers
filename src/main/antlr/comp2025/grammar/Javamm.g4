
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
    : CLASS name=ID (EXTENDS superClass=ID)? '{' (varDecl)* (methodDecl)* '}'
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
    : '!' expr                                # Not
    | expr op=('<'|'>'|'<='|'>='|'=='|'!=') expr  # Compare
    | expr '&&' expr             # And
    | expr '||' expr              # Or
    | expr op=('*'|'/') expr     # MulDiv
    | expr op=('+'|'-') expr     # AddSub
    | expr '[' expr ']'                       # ArrayAccess
    | NEW name=INT '[' expr ']'                   # NewArray
    | expr '.' 'length'                     # Length
    | expr '.' name=ID '(' (expr(',' expr)*) ? ')'  #MethodCall
    | NEW name=ID '(' ')'                          # NewObject
    | '(' expr ')'                            # Parenthesis
    | name=INTEGER                               # Int
    | name = (TRUE|FALSE)                     # Boolean
    | name=ID                                 # Id
    | name=THIS                                # This
    | '[' arrayInit? ']'                      # ArrayLiteral
    ;

args
    : expr (',' expr)*
    ;

arrayInit
    : expr (',' expr)*
    ;
