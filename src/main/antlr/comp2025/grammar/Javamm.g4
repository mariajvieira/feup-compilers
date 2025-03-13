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
    | BOOLEAN           // boolean
    | ID ('[' ']')?        // class name and class[]
    | VOID                 // void return type
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
      type name=ID
      '(' paramList? ')'
      '{' varDecl* stmt* '}'
    ;

paramList
    : param (',' param)*              # MultipleParams
    ;

param
    : type name=ID                    # SingleParam
    | type '...' name=ID              # VarargParam
    ;

stmt
    : '{' stmt* '}'                           # Block
    | 'if' '(' cond=expr ')' stmt ( 'else' stmt )?  # If
    | 'while' '(' cond=expr ')' stmt             # While
    | 'return' expr? ';'                        # Return
    | expr '=' expr ';'                         # Assign
    | expr ';'                                  # ExprStmt
    ;

expr
    : left=expr ('*'|'/')  right=expr1    # MulDiv
    | expr1                        # Expr1Pass
    ;

expr1
    : left=expr1 ('+'|'-') right=expr2   # AddSub
    | exprRel                        # ExprRelPass
    ;

exprRel
    : left=exprRel op=('<' | '>' | '<=' | '>=') right=expr2  # Relational
    | expr2                                                 # Expr2Pass
    ;

expr2
    : left=expr2 op='||' right=expr3  # LogicalOr
    | expr3                                # Expr3Pass
    ;

expr3
    : left=expr3 op='&&' right=unary  # LogicalAnd
    | unary                                # UnaryPass
    ;

unary
    : op='!' operand=unary               # Negate
    | 'new' baseType=type '(' expr* ')'  # NewClassInstance
    | 'new' baseType=type '[' expr ']'   # NewArray
    | primary                           # UnaryPrimary
    ;

primary
    : base=basePrimary (methodSuffix)*  # PrimaryWithSuffix
    ;

basePrimary
    : value=INTEGER                     # IntegerLiteral
    | name=ID                           # VarRef
    | '(' expr ')'                      # ParenthesizedExpr
    | '[' arrayExpr ']'                  # ArrayLiteral
    ;

methodSuffix
    : '.' method=ID '(' exprList? ')'     # MethodCall
    | '.' field=ID                        # FieldAccess
    | '[' expr ']'                        # ArrayAccess
    ;

arrayExpr
    : expr (',' expr)*                    # ArrayInit
    ;

methodAccess
    : primary (methodSuffix)*             # MethodCallChain
    ;



exprList
    : expr (',' expr)*                    # ArgumentList
    ;

