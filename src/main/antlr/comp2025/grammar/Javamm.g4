grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

// Keywords
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

// Tokens
INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
COMMENT : '//' ~[\r\n]* -> skip ;
MULTILINE_COMMENT : '/*' .*? '*/' -> skip ;
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
    : CLASS name=ID (EXTENDS superClass=ID)? '{' 
        (varDecl)* 
        (methodDecl)* 
      '}'
    ;

varDecl
    : type name=ID ';'
    ;

type locals[boolean isArray=false]
    : name=INT ('[' ']' {$isArray=true;})?       // int, int[]
    | name=BOOLEAN ('[' ']')?                   // boolean, boolean[]
    | name=STRING ('[' ']')?                    // String, String[]
    | name=ID                                   // class name
    | name=VOID                                 // void (for methods)
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
      type name=ID
      '(' paramList? ')'
      '{' 
        (varDecl)* 
        (stmt)* 
        (RETURN expr? ';')?
      '}'
    ;

paramList
    : param (',' param)*
    ;

param
    : type name=ID                  // Regular parameter
    | type '...' name=ID            // Varargs parameter
    ;

stmt
    : '{' stmt* '}'                           # Block
    | 'if' '(' expr ')' stmt
      ('else' stmt)?                          # IfStmt
    | 'while' '(' expr ')' stmt               # WhileStmt
    | target=expr '=' value=expr ';'          # AssignStmt
    | expr ';'                                # ExprStmt
    | returnStmt                              #RetStmt
    ;

returnStmt
    : RETURN expr? ';'
    ;

expr
    : '!' expr                                # Not
    | expr op=('<'|'>'|'<='|'>='|'=='|'!=') expr  # Compare
    | expr '&&' expr                          # And
    | expr '||' expr                          # Or
    | expr op=('*'|'/') expr                  # MulDiv
    | expr op=('+'|'-') expr                  # AddSub
    | expr '[' expr ']'                       # ArrayAccess
    | NEW name=INT '[' expr ']'               # NewArray
    | expr '.' 'length'                       # Length
    | expr '.' name=ID '(' (expr(',' expr)*)? ')'  # MethodCall
    | NEW name=ID '(' ')'                     # NewObject
    | '(' expr ')'                            # Parenthesis
    | name=INTEGER                            # Int
    | name=(TRUE|FALSE)                       # Boolean
    | name=ID                                 # Id
    | name=THIS                               # This
    | '[' arrayInit? ']'                      # ArrayLiteral
    ;

arrayInit
    : expr (',' expr)*
    ;