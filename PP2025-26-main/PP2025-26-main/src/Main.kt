package begend

import begend.lexer.*
import begend.parser.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


fun formatJson(json: String): String {
    val result = StringBuilder()
    var indent = 0
    val indentStr = "  " // 2 space indent
    var inString = false
    var escapeNext = false
    
    var i = 0
    while (i < json.length) {
        val ch = json[i]
        
        if (escapeNext) {
            result.append(ch)
            escapeNext = false
            i++
            continue
        }
        
        when (ch) {
            '\\' -> {
                result.append(ch)
                escapeNext = true
            }
            '"' -> {
                result.append(ch)
                inString = !inString
            }
            '{', '[' -> {
                result.append(ch)
                if (!inString) {
                    indent++
                    if (i + 1 < json.length) {
                        val next = json[i + 1]
                        if (next != '}' && next != ']' && next != ' ') {
                            result.append('\n')
                            result.append(indentStr.repeat(indent))
                        }
                    }
                }
            }
            '}', ']' -> {
                if (!inString) {
                    indent--
                    if (i - 1 >= 0) {
                        val prev = json[i - 1]
                        if (prev != '{' && prev != '[' && prev != ',') {
                            result.append('\n')
                            result.append(indentStr.repeat(indent))
                        }
                    }
                }
                result.append(ch)
            }
            ',' -> {
                result.append(ch)
                if (!inString) {
                    result.append('\n')
                    result.append(indentStr.repeat(indent))
                } else {
                    result.append(' ')
                }
            }
            ':' -> {
                result.append(ch)
                if (!inString && i + 1 < json.length) {
                    result.append(' ')
                }
            }
            ' ', '\n', '\t', '\r' -> {
                // Preskače whitespace između tokena, ali zadržava unutar stringova
                if (inString) {
                    result.append(ch)
                }
            }
            else -> result.append(ch)
        }
        i++
    }
    
    return result.toString()
}

fun main(args: Array<String>) {
    val inputs: List<Pair<String, String>> =
        if (args.isNotEmpty()) {
            args.map { path ->
                val text = try {
                    val pathObj = Paths.get(path)
                    val file = when {
                        pathObj.isAbsolute && Files.exists(pathObj) -> pathObj
                        Files.exists(pathObj) -> pathObj
                        else -> {
                            // Probaj u trenutnom direktorijumu
                            val currentDir = System.getProperty("user.dir")
                            val file1 = Paths.get(currentDir, path).normalize()
                            if (Files.exists(file1)) {
                                file1
                            } else {
                                // Probaj u PP2025-26-main podfolderu
                                val file2 = Paths.get(currentDir, "PP2025-26-main", path).normalize()
                                if (Files.exists(file2)) {
                                    file2
                                } else {
                                    pathObj // Vraćamo originalnu putanju da bi dobili jasnu grešku
                                }
                            }
                        }
                    }
                    Files.readString(file)
                } catch (e: Exception) {
                    System.err.println("Greška: ne mogu da pročitam '$path' (${e.message})")
                    return@map path to ""
                }
                path to text
            }
        } else {
            // Inline demo uključuje ;, real/bool tipove i literale
            listOf("(inline)" to """
                int a; real r; bool ok;
                5 -> a; 2.5 -> r; true -> ok;
                if (a < 10 and not (r >= 2.0)) print(1); else print(0);
            """.trimIndent())
        }

    for ((name, program) in inputs) {
        if (program.isEmpty()) continue
        println("=== TOKENS for: $name ===")
        try {
            val lexer = Lexer(program)
            val tokens = lexer.allTokens()
            tokens.forEach { println("${it.type}\t'${it.lexeme}' @${it.line}:${it.col}") }
            println("Ukupno tokena (bez EOF): ${tokens.count { it.type != TokenType.EOF }}")
            
            // Sintaksna analiza
            println("\n=== SYNTAX TREE for: $name ===")
            try {
                val parser = Parser(tokens)
                val ast = parser.parse()
                
                // Korišćenje Visitor pattern za ispis stabla
                val printVisitor = PrintTreeVisitor()
                println(ast.accept(printVisitor))
                
                // JSON format - upis u program.json fajl
                val jsonVisitor = JsonVisitor()
                val jsonContent = ast.accept(jsonVisitor)
                val formattedJson = formatJson(jsonContent)
                val jsonFile = Paths.get("program.json")
                Files.writeString(jsonFile, formattedJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
                println("\n=== AST upisan u program.json ===")
            } catch (e: ParseError) {
                System.err.println("Sintaksna greška u '$name': ${e.message}")
                System.err.println("Poslednji učitan token: ${e.lastToken.type} '${e.lastToken.lexeme}' @${e.lastToken.line}:${e.lastToken.col}")
                System.err.println("Token na mestu greške: ${e.errorToken.type} '${e.errorToken.lexeme}' @${e.errorToken.line}:${e.errorToken.col}")
            }
        } catch (t: Throwable) {
            // Prijavi leksičku grešku bez rušenja celog procesa
            System.err.println("Leksička greška u '$name': ${t.message}")
        }
        println()
    }
}