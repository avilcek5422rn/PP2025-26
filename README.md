# BegEnd — Lexer 

Ovo je **leksički analizator** za jezik BegEnd (tvoj deo projekta). Sadrži:
- opis tokena i pravila,
- primere koda u BegEnd jeziku (`examples/*.bg`),
- CLI za pokretanje lexer-a nad fajlovima.

## Pokretanje (IntelliJ)
Run Config → `MainKt`  
**Program arguments**: putanje do `.bg` fajlova (npr. `examples/hello.bg examples/arrays.bg`).

## Pokretanje (CLI)
```bash
kotlinc src/*.kt -include-runtime -d begend-lexer.jar
java -jar begend-lexer.jar examples/hello.bg examples/arrays.bg examples/fizz.bg