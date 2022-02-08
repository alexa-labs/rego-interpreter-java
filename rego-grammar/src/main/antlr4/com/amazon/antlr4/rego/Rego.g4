// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

grammar Rego;
/* https://www.openpolicyagent.org/docs/latest/policy-reference/#grammar */

module           : rpackage rimport* policy EOF ;
rpackage         : 'package' VAR ('.' VAR)* ;
rimport          : 'import' ref ('as' VAR)? ;
policy           : rrule* ;
rrule            : user_function | rule_definition ;
rule_definition  : DEFAULT VAR '=' scalar | rule_head rule_body? ;
user_function    : VAR '(' dest_args ')' ('=' iterm)? rule_body? ;
dest_args        : (VAR (',' VAR)*)? ;
rule_head        : VAR ('(' rule_args ')')? ('[' rule_index ']')? ((':='|'=') rule_assignment)? ;
rule_args        : (iterm (',' iterm)*)? ;
rule_index       : iterm ;
rule_assignment  : iterm ;
rule_body        : '{' query '}' ('else' ('=' iterm)? rule_body?)? ;
query            : literal+ ;
literal          : ( some_decl | stat | NOT stat ) with_modifier* ;
some_decl        : 'some' VAR (',' VAR)* ;
with_modifier    : 'with' term 'as' term ;
stat             : stat_infix | term ;
stat_infix       : (VAR ('(' rule_args ')')? ('[' rule_index ']')? ':=')? term (infix_operator term)? ;
term             : ref | scalar ;
iterm            : iterm infix_operator iterm | '(' iterm ')' | term ;
array_compr      : '[' iterm PIPE query ']' ;
set_compr        : '{' iterm PIPE query '}' ;
object_compr     : '{' object_item PIPE query '}' ;
ref              : ( array_compr | object_compr | set_compr | array | object | set | expr_call | VAR) ref_arg* ;
expr_call        : VAR ('.' VAR)* '(' (iterm (',' iterm)* )? ')' ;
ref_arg          : ref_arg_brack | '.' VAR ;
ref_arg_brack    : '[' ( iterm | '_' ) ']' ;
array            : non_empty_array | EMPTY_ARRAY ;
non_empty_array  : '[' iterm (',' iterm)* ']' ;
object           : non_empty_object | empty_object ;
non_empty_object : '{' object_item (',' object_item)* ','? '}' ;
object_item      : ( ref | scalar | VAR ) ':' iterm ;
empty_object     : '{' '}' ;
set              : non_empty_set ;
non_empty_set    : '{' iterm (',' iterm)* '}' ;
scalar           : (STRING | NUMBER | TRUE | FALSE | NULL) ;
infix_operator   : MUL_OPERATOR | ADD_OPERATOR | REL_OPERATOR | EQL_OPERATOR | bin_operator | unification ;
bin_operator     : '&' | PIPE ;
unification      : '=' ;

DEFAULT          : 'default' ;
EMPTY_ARRAY      : '[' ']' ;
MUL_OPERATOR     : '*' | '/' | '%' ;
ADD_OPERATOR     : '+' | '-' ;
REL_OPERATOR     : '>=' | '<=' | '<' | '>' ;
EQL_OPERATOR     : '==' | '!=' ;
PIPE             : '|' ;
TRUE             : 'true' ;
FALSE            : 'false' ;
NULL             : 'null' ;
NOT              : 'not' ;
VAR              : ( ALPHA | '_' ) (ALPHA | DIGIT | '_')* ;
STRING           : (JSON_STRING | RAW_STRING) ;

RAW_STRING       : '`' ~('`')* '`' ;
JSON_STRING      :  '"' (ESC | ~["\\])* '"' ; /* https://tutorial-academy.com/json-parser-antlr4-eclipse/ */
fragment ESC     :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX     : [0-9a-fA-F] ;

COMMENT          : '#' ~[\n]* '\n'+ -> skip ;

NUMBER           :   '-'? INT '.' [0-9]+ EXP? |   '-'? INT EXP |   '-'? INT ;
fragment INT     : '0' | [1-9] [0-9]* ;
fragment EXP     : [Ee] [+\-]? INT ;
ALPHA            : [A-Za-z] ;
DIGIT            : [0-9] ;
WS               : [ \t\n\r;]+ -> skip ;
