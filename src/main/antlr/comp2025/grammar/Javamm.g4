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
    : left=expr op='+' right=term #Add
    | left=expr op='-' right=term #Sub
    | term #ExprTerm
    ;


term
    : left=term op='*' right=unary #Mult
    | left=term op='/' right=unary #Div
    | unary #TermUnary
    ;

unary
    : op='!' operand=unary #Negate
    | factor #UnaryFactor
    ;

factor
    : value=INTEGER #IntegerLiteral
    | name=ID #VarRef
    | '(' expr ')' #ParenthesizedExpr
    ;