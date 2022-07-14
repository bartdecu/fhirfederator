grammar FhirUrl;

@header {
    package ca.uhn.fhir.federator;
}


 //PARSER

s : a QM c | a ;
a : f SLASH i SLASH HISTORY SLASH n | f SLASH i | f SLASH o | f SLASH i SLASH o | f | o ;
c : c AMP p | p ;
p : k EQ d;
k : q COLON u | q | h | e COLON u |  e;
d : d COMMA v | v ;
v : r SLASH i | f SLASH i | w | f COLON e | f COLON e COLON b;
q : SPECIAL;
e : x COLON f DOT e | x DOT e | x;
x : TOKEN;
h : g COLON j;
f : TOKEN;
i : DECIMAL | TOKEN | IDENTIFIER;
g : HAS;
j : f COLON l COLON m;
l : TOKEN;
m : h | e;
o : OPERATOR;
u : TOKEN;
b : TOKEN;
w : i | URL | URN | URL PIPE i (PIPE i)? | URN PIPE i (PIPE i)? | DECIMAL PIPE URL? PIPE i;
n : IDENTIFIER;
r : PROFILE;



//LEXER
fragment NSEPARATOR : [a-zA-Z\-]+ ;
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
PROFILE: 'Profile';
SPECIAL: '_'NSEPARATOR;
OPERATOR: '$'NSEPARATOR;
//IDENTIFIER: (((STRING '://' STRING? ((STRING ('.' STRING)*) | (DIGITS '.' DIGITS '.' DIGITS '.' DIGITS)) (':' DIGITS)? ('/' (STRING ('/' STRING)*))? )? | 'urn:' STRING ':' ALLOWEDINURN ) PIPE)? XSTRING ;
DECIMAL: ([a-z] [a-z])? DIGITS (DOT DIGITS ('e' ( '-' )? DIGITS )? )? ;
TOKEN: NSEPARATOR ;
IDENTIFIER: XSTRING ;
URL: NSEPARATOR '://' STRING? ((STRING ('.' STRING)*) | (DIGITS '.' DIGITS '.' DIGITS '.' DIGITS)) (':' DIGITS)? ('/' (STRING ('/' STRING)*))? (QM XSTRING EQ TOKEN (SLASH STRING)? )? ;
URN: 'urn:' STRING ':' ALLOWEDINURN ;
