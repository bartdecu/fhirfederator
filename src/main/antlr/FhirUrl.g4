grammar FhirUrl;

@header {
    package ca.uhn.fhir.federator;
}


 //PARSER

s : a | a QM c;
a : f | o | f SLASH o | f SLASH i SLASH o;
c : p | c AMP p;
p : k EQ d;
k : q | e COLON r | h | e COLON u | e DOT t | e;
d : v | d COMMA v;
v : f SLASH i | IDENTIFIER | f COLON e;
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



//LEXER
fragment NSEPARATOR : [a-zA-Z] ;//(DOT | COLON | SLASH | QM | AMP | EQ | COMMA) ;
fragment UPPERCASE  : [A-Z] ;
fragment STRING : [a-zA-Z]+;
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
SPECIAL: '_'(NSEPARATOR)+;
OPERATOR: '$'(NSEPARATOR)+;
IDENTIFIER: (((STRING '://' STRING? ((STRING ('.' STRING)*) | (DIGITS '.' DIGITS '.' DIGITS '.' DIGITS)) (':' DIGITS)? ('/' (STRING ('/' STRING)*))? )? | ('urn:' STRING ':' ALLOWEDINURN )?) PIPE)? XSTRING ;


