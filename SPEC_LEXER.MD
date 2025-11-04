# BegEnd — Leksička specifikacija
BegEnd je jednostavan proceduralni jezik razvijen za edukativne potrebe.
Sintaksa je minimalistička, bazirana na blokovima begin...end.

Koristi operator -> za dodelu vrednosti, print() za ispis, i podržava aritmetičke izraze, grananja (if/or/else), petlje (for) i funkcije sa tipom int.
Cilj dizajna jezika je jednostavno čitljiv i lako analizabilan kod koji prikazuje osnovne principe leksičke i sintaksne analize.

## Kratak opis leksičkog analizatora i nalepi tekst iznad
Leksički analizator za jezik BegEnd sekvencijalno čita izvorni kod znak po znak i grupiše ih u tokene prema pravilima iz specifikacije (ključne reči, identifikatori, celobrojni literali, operatori i zagrade).  
Implementiran je ručno u Kotlinu, bez eksternih biblioteka, kao deterministički skener sa jednim lookahead-om koji prepoznaje i dvokarakterni operator `->` (token `ASSIGN_ARROW`) i razlikuje ključne reči od identifikatora. 

Beli znakovi (razmak, tab, novi red) ignorišu se van tokena, a za svaki proizvedeni token čuvaju se lekséma, tip i pozicija (linija:kolona) radi dijagnostike.  

Na neprepoznat znak analizator odmah prijavljuje leksičku grešku sa preciznom lokacijom i prekida dalje skeniranje problematičnog fajla.  
Program se pokreće nad jednim ili više `.bg` fajlova i na izlazu ispisuje sekvencu tokena (ili grešku), koja zatim predstavlja ulaz sintaksnom analizatoru.
## Ključne reči
begin, end, function, return,
if, or, else,
for, goes, from, to,
print,
int
## Tokeni (nazivi su “kontrakt” za parser tim)
BEGIN, END, FUNCTION, RETURN,
IF, OR, ELSE,
FOR, GOES, FROM, TO,
PRINT, INT,
IDENT, INT_LITERAL,
PLUS(’+’), MINUS(’-’), STAR(’*’), SLASH(’/’), PERCENT(’%’),
ASSIGN_ARROW(’->’),
EQ(’=’),
LPAREN(’(’), RPAREN(’)’),
LBRACKET(’[’), RBRACKET(’]’),
COMMA(’,’), COLON(’:’),
EOF
## Pravila leksike
- **Identifikatori**: `[A-Za-z_][A-Za-z0-9_]*`, razlikuju se od ključnih reči.
- **Int literali**: decimalni nenegativni brojevi (`0`, `1`, `2`, …`).
- **Whitespace**: razmaci, tabovi, novi redovi ignorišu se van tokena.
- **Komentari/stringovi**: nema ih u ovoj verziji jezika.
- **Greške**: nepoznat znak baca grešku sa linijom i kolonom (`Leksička greška na <linija>:<kolona>`).

## Primeri fajlova
- Pozitivni: `hello.bg`, `arrays.bg`, `fizz.bg`, `assign.bg`, `arith.bg`
- Negativni: `bad_char.bg`, `bad_unicode.bg`
- 