grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : classDecl EOF
    ;

classDecl
    : CLASS name=ID
        '{'
        methodDecl*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type
    : name= INT ;

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
    : lhs=ID '=' rhs=expr ';' #AssignStmt
    | RETURN expr ';' #ReturnStmt
    ;

expr
    : left=expr '||' right=expr1    # LogicalOr
    | expr1                        # Expr1Pass
    ;

expr1
    : left=expr1 '&&' right=expr2   # LogicalAnd
    | expr2                        # Expr2Pass
    ;

expr2
    : left=expr2 op=('+'|'-') right=expr3  # AddSub
    | expr3                               # Expr3Pass
    ;

expr3
    : left=expr3 op=('*'|'/') right=unary  # MulDiv
    | unary                               # UnaryPass
    ;

unary
    : op='!' operand=unary               # Negate
    | access                            # UnaryAccess
    ;

access
    : primary (suffix)*
    ;

primary
    : value=INTEGER                     # IntegerLiteral
    | name=ID                           # VarRef
    | '(' expr ')'                      # ParenthesizedExpr
    ;

suffix
    : '.' method=ID '(' (expr (',' expr)*)? ')'   # MethodCall
    | '[' expr ']'                              # ArrayAccess
    | '.' field=ID                              # FieldAccess
    ;