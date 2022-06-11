grammar FhirUrl;

@header {
    package ca.uhn.fhir.federator;
}


 //PARSER

s : a QM c | a ;
a : f SLASH i SLASH HISTORY SLASH n | f SLASH i | f SLASH o | f SLASH i SLASH o | f | o ;
c : c AMP p | p ;
p : k EQ d;
k : q COLON u | q | e COLON r | h | e COLON u | e DOT t | e;
d : d COMMA v | v ;
v : f SLASH i | w | f COLON e | f COLON e COLON b;
q : SPECIAL;
e : IDENTIFIER;
r : f DOT t;
h : g COLON j;
f : IDENTIFIER;
i : IDENTIFIER;
t : IDENTIFIER;
g : HAS;
j : f COLON l COLON m;
l : IDENTIFIER;
m : h | e COLON r | e;
o : OPERATOR;
u : IDENTIFIER;
b : IDENTIFIER;
w : IDENTIFIER | URL PIPE IDENTIFIER | URN PIPE IDENTIFIER;
n : IDENTIFIER;



//LEXER
fragment NSEPARATOR : [a-zA-Z] ;//(DOT | COLON | SLASH | QM | AMP | EQ | COMMA) ;
fragment UPPERCASE  : [A-Z] ;
fragment STRING : [a-zA-Z0-9\-.]+;
fragment DIGITS : [0-9]+ ;
fragment XSTRING : [A-Za-z0-9\-_%]+ ;
fragment ALLOWEDINURN : [A-Za-z0-9.;\-_%]+ ;

PIPE : '|';
DOT : '.';
COLON: ':';
SLASH: '/';
QM: '?';
AMP: '&';
EQ: '=';
COMMA: ',';
HAS: '_has';
HISTORY: '_history';
SPECIAL: '_'(NSEPARATOR)+;
OPERATOR: '$'(NSEPARATOR)+;
//IDENTIFIER: (((STRING '://' STRING? ((STRING ('.' STRING)*) | (DIGITS '.' DIGITS '.' DIGITS '.' DIGITS)) (':' DIGITS)? ('/' (STRING ('/' STRING)*))? )? | 'urn:' STRING ':' ALLOWEDINURN ) PIPE)? XSTRING ;
IDENTIFIER: XSTRING ;
URL: STRING '://' STRING? ((STRING ('.' STRING)*) | (DIGITS '.' DIGITS '.' DIGITS '.' DIGITS)) (':' DIGITS)? ('/' (STRING ('/' STRING)*))?  ;
URN: 'urn:' STRING ':' ALLOWEDINURN ;
