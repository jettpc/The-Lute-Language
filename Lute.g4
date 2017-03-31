//Paul Jett
grammar Lute;

// Lexer rules
BOOL_LITERAL: 'true' | 'false' ;
COMP_OP: '>' | '<' | '<=' | '>=';
EQUALITY_OP:  '==' | '!=';
AND_OP:  'and';
OR_OP:  'or';
NOT_OP:  'not' ;
INT_LITERAL: DIGIT+ ;
FLOAT_LITERAL: DIGIT* '.' DIGIT+;
MULT_OP:  '*' | '/' | '%';
ADD_OP: '+' | '-' ;
STRING_LITERAL: '"' ~[\n\r]*? '"' ;
ID: ID_LETTER (ID_LETTER | DIGIT)* ;
LEFT_PAREN: '(';
RIGHT_PAREN: ')';

// Skip
SL_COMMENT:  '//' ~[\r\n]+ -> skip;
WS: [ \n\r\t]+ -> skip ;

fragment DIGIT: [0-9] ;
fragment ID_LETTER: [a-zA-Z_] ;

// Grammar rules
start: (fctnDef | stmt)+ ;

stmts: stmt* ;

stmt:
   'print' expr ';'                  # PrintStmt
  | ID '=' expr ';'                  # AssignStmt
  | 'if' LEFT_PAREN expr RIGHT_PAREN
     stmts 'end'                     # IfStmt
  | 'if' LEFT_PAREN expr RIGHT_PAREN
     stmts 'else' stmts 'end'        # IfElseStmt
  | 'if' LEFT_PAREN expr RIGHT_PAREN
     stmts
     ('elsif' LEFT_PAREN expr RIGHT_PAREN stmts )+
     ('else' stmts)?
     'end'                           # IfElsifStmt
  | 'while' LEFT_PAREN expr RIGHT_PAREN
     stmts 'end'                     # While
  | 'break' ';'                      # BreakStmt
  | 'continue' ';'                   # ContinueStmt
  | expr ';'                         # ExprStmt
  | 'return' expr? ';'               # ReturnStmt
  ;

fctnDef:
  'function' ID LEFT_PAREN (ID (',' ID)*)? RIGHT_PAREN stmts 'end' ;

fctnCall:
  ID LEFT_PAREN (expr (',' expr)*)? RIGHT_PAREN ;

expr:
    ADD_OP expr           # UnaryPlusMinus
  | NOT_OP expr           # UnaryNot
  | expr MULT_OP expr     # Mult
  | expr ADD_OP expr      # Add
  | expr COMP_OP expr     # Comparison
  | expr EQUALITY_OP expr # Equality
  | expr AND_OP expr      # And
  | expr OR_OP expr       # Or
  | INT_LITERAL           # IntLiteral
  | FLOAT_LITERAL         # FloatLiteral
  | STRING_LITERAL        # StringLiteral
  | BOOL_LITERAL          # BoolLiteral
  | ID                    # Id
  | fctnCall              # FunctionCall
  | LEFT_PAREN expr RIGHT_PAREN          # Paren
;
