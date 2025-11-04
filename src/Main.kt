package begend

import begend.lexer.*
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val inputs: List<Pair<String, String>> =
        if (args.isNotEmpty()) {
            args.map { path ->
                val text = try {
                    Files.readString(Paths.get(path))
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
        } catch (t: Throwable) {
            // Prijavi leksičku grešku bez rušenja celog procesa
            System.err.println("Leksička greška u '$name': ${t.message}")
        }
        println()
    }
}