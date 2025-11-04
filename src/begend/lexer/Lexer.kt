// Lexer.kt
package begend.lexer

enum class TokenType {
    BEGIN, END, FUNCTION, RETURN,
    IF, OR, ELSE,
    FOR, GOES, FROM, TO,
    PRINT, INT,
    IDENT, INT_LITERAL,

    PLUS, MINUS, STAR, SLASH, PERCENT,
    ASSIGN_ARROW, // ->
    EQ,           // =
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    COMMA, COLON,

    // --- NOVO: tipovi/literali
    REAL, BOOL, BOOL_LITERAL, REAL_LITERAL,

    // --- NOVO: I/O
    READ,

    // --- NOVO: logički (OR već postoji)
    AND, NOT,

    // --- NOVO: relacije i separator
    LT, LE, GT, GE, NE,
    SEMICOLON, // ;

    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val col: Int
)

class Lexer(private val input: String) {

    private var pos = 0
    private var line = 1
    private var col = 1

    private fun isAtEnd() = pos >= input.length
    private fun peek(): Char = if (isAtEnd()) '\u0000' else input[pos]
    private fun peekNext(): Char = if (pos + 1 >= input.length) '\u0000' else input[pos + 1]

    private fun advance(): Char {
        val ch = peek()
        pos++
        if (ch == '\n') { line++; col = 1 } else { col++ }
        return ch
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (input[pos] != expected) return false
        pos++
        col++
        return true
    }

    private fun skipWhitespace() {
        while (!isAtEnd()) {
            when (peek()) {
                ' ', '\t', '\r' -> advance()
                '\n' -> advance()
                '/' -> {
                    val next = peekNext()
                    if (next == '/') {
                        // linijski komentar
                        advance(); advance()
                        while (!isAtEnd() && peek() != '\n') advance()
                    } else if (next == '*') {
                        // blok komentar
                        advance(); advance()
                        while (!isAtEnd()) {
                            if (peek() == '*' && peekNext() == '/') {
                                advance(); advance()
                                break
                            }
                            advance()
                        }
                    } else return
                }
                else -> return
            }
        }
    }

    private fun isAlpha(ch: Char) = ch == '_' || ch.isLetter()
    private fun isAlnum(ch: Char) = isAlpha(ch) || ch.isDigit()

    private fun keywordOrIdent(startLine: Int, startCol: Int): Token {
        val start = pos
        while (!isAtEnd() && isAlnum(peek())) advance()
        val text = input.substring(start - 1, pos) // -1 jer smo već pojeli 1. slovo u caller-u

        val kwType = when (text) {
            "begin"    -> TokenType.BEGIN
            "end"      -> TokenType.END
            "function" -> TokenType.FUNCTION
            "return"   -> TokenType.RETURN
            "if"       -> TokenType.IF
            "or"       -> TokenType.OR
            "else"     -> TokenType.ELSE
            "for"      -> TokenType.FOR
            "goes"     -> TokenType.GOES
            "from"     -> TokenType.FROM
            "to"       -> TokenType.TO
            "print"    -> TokenType.PRINT
            "int"      -> TokenType.INT

            // --- NOVO: ključne reči/identifikatori
            "read"     -> TokenType.READ
            "real"     -> TokenType.REAL
            "bool"     -> TokenType.BOOL
            "and"      -> TokenType.AND
            "not"      -> TokenType.NOT
            "true"     -> TokenType.BOOL_LITERAL
            "false"    -> TokenType.BOOL_LITERAL

            else       -> null
        }
        return if (kwType != null)
            Token(kwType, text, startLine, startCol)
        else
            Token(TokenType.IDENT, text, startLine, startCol)
    }

    // --- IZMENJENO: podržani i real (decimalni) literali
    private fun number(startLine: Int, startCol: Int): Token {
        val start = pos
        // već je prvi digit pojeden pre ulaska u ovu funkciju, zato substring(start - 1, ...)
        while (!isAtEnd() && peek().isDigit()) advance()

        var isReal = false
        if (!isAtEnd() && peek() == '.') {
            isReal = true
            advance() // pojedi '.'
            if (isAtEnd() || !peek().isDigit()) {
                error("Leksička greška na $startLine:$startCol – očekivana cifra posle '.'")
            }
            while (!isAtEnd() && peek().isDigit()) advance()
        }

        val text = input.substring(start - 1, pos)
        return if (isReal)
            Token(TokenType.REAL_LITERAL, text, startLine, startCol)
        else
            Token(TokenType.INT_LITERAL,  text, startLine, startCol)
    }

    fun nextToken(): Token {
        skipWhitespace()
        if (isAtEnd()) return Token(TokenType.EOF, "", line, col)

        val startLine = line
        val startCol = col
        val ch = advance()

        return when (ch) {
            '+' -> Token(TokenType.PLUS, "+", startLine, startCol)
            '-' -> {
                if (peek() == '>') {
                    advance()
                    Token(TokenType.ASSIGN_ARROW, "->", startLine, startCol)
                } else Token(TokenType.MINUS, "-", startLine, startCol)
            }
            '*' -> Token(TokenType.STAR, "*", startLine, startCol)
            '/' -> Token(TokenType.SLASH, "/", startLine, startCol)
            '%' -> Token(TokenType.PERCENT, "%", startLine, startCol)
            '=' -> Token(TokenType.EQ, "=", startLine, startCol)
            '(' -> Token(TokenType.LPAREN, "(", startLine, startCol)
            ')' -> Token(TokenType.RPAREN, ")", startLine, startCol)
            '[' -> Token(TokenType.LBRACKET, "[", startLine, startCol)
            ']' -> Token(TokenType.RBRACKET, "]", startLine, startCol)
            ',' -> Token(TokenType.COMMA, ",", startLine, startCol)
            ':' -> Token(TokenType.COLON, ":", startLine, startCol)

            // --- NOVO: separator i relacije
            ';' -> Token(TokenType.SEMICOLON, ";", startLine, startCol)

            '<' -> if (!isAtEnd() && peek() == '=') {
                advance(); Token(TokenType.LE, "<=", startLine, startCol)
            } else Token(TokenType.LT, "<", startLine, startCol)

            '>' -> if (!isAtEnd() && peek() == '=') {
                advance(); Token(TokenType.GE, ">=", startLine, startCol)
            } else Token(TokenType.GT, ">", startLine, startCol)

            '!' -> if (!isAtEnd() && peek() == '=') {
                advance(); Token(TokenType.NE, "!=", startLine, startCol)
            } else error("Leksička greška na $startLine:$startCol – neočekivan znak '!'")

            else -> when {
                ch.isDigit()      -> number(startLine, startCol)
                isAlpha(ch)       -> keywordOrIdent(startLine, startCol)
                else -> error("Leksička greška na $startLine:$startCol – neočekivan znak '$ch'")
            }
        }
    }

    fun allTokens(): List<Token> {
        val list = mutableListOf<Token>()
        while (true) {
            val t = nextToken()
            list += t
            if (t.type == TokenType.EOF) break
        }
        return list
    }
}