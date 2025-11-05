package begend.parser

import begend.lexer.*

class ParseError(
    val lastToken: Token,
    val errorToken: Token,
    message: String
) : Exception(message)

class Parser(private val tokens: List<Token>) {
    private var current = 0
    private var lastLoadedToken: Token? = null

    private fun peek(): Token = tokens[current]
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun previous(): Token = tokens[current - 1]

    private fun advance(): Token {
        if (!isAtEnd()) {
            lastLoadedToken = peek()
            current++
        }
        return previous()
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(message)
    }

    private fun error(message: String): ParseError {
        val errorToken = if (isAtEnd()) previous() else peek()
        val lastToken = lastLoadedToken ?: tokens.firstOrNull() ?: errorToken
        return ParseError(lastToken, errorToken, message)
    }

    fun parse(): ProgramNode {
        val functions = mutableListOf<FunctionNode>()
        val enums = mutableListOf<EnumNode>()
        val statements = mutableListOf<StatementNode>()

        while (!isAtEnd()) {
            if (check(TokenType.BEGIN) && current + 1 < tokens.size && tokens[current + 1].type == TokenType.FUNCTION) {
                // "begin function"
                advance() // consume BEGIN
                advance() // consume FUNCTION
                functions.add(parseFunction(true))
            } else if (check(TokenType.FUNCTION)) {
                // "function" without begin
                advance()
                functions.add(parseFunction(false))
            } else if (check(TokenType.ENUM)) {
                // enum declaration
                advance()
                enums.add(parseEnum())
            } else {
                statements.add(parseStatement())
            }
        }

        return ProgramNode(functions, enums, statements)
    }

    private fun parseFunction(hadBegin: Boolean): FunctionNode {
        // FUNCTION has already been consumed
        val name = consume(TokenType.IDENT, "Očekivan identifikator za ime funkcije")
        val params = parseParams()
        consume(TokenType.COLON, "Očekivano ':' posle parametara")
        val returnType = consumeType()
        val body = parseStatement()
        
        // If function had "begin", it must end with "end function"
        // If body was a block, it already consumed "end", so we just need "function"
        // Otherwise, we need "end function"
        if (hadBegin) {
            if (body is BlockNode) {
                // Block already consumed "end", just need "function"
                consume(TokenType.FUNCTION, "Očekivana ključna reč 'function' posle 'end'")
            } else {
                // Need "end function"
                consume(TokenType.END, "Očekivana ključna reč 'end' posle tela funkcije")
                consume(TokenType.FUNCTION, "Očekivana ključna reč 'function' posle 'end'")
            }
        } else if (match(TokenType.END)) {
            // Function without begin, but has end
            consume(TokenType.FUNCTION, "Očekivana ključna reč 'function' posle 'end'")
        }

        return FunctionNode(nameToken = name, params, returnType, body)
    }

    private fun parseEnum(): EnumNode {
        // ENUM has already been consumed
        val name = consume(TokenType.IDENT, "Očekivan identifikator za ime enum-a")
        consume(TokenType.LBRACE, "Očekivano '{' posle imena enum-a")
        
        val values = mutableListOf<EnumValueNode>()
        
        if (!check(TokenType.RBRACE)) {
            do {
                val valueName = consume(TokenType.IDENT, "Očekivan identifikator za enum vrednost")
                values.add(EnumValueNode(valueName))
            } while (match(TokenType.COMMA))
        }
        
        consume(TokenType.RBRACE, "Očekivano '}' posle enum vrednosti")
        
        // Enum deklaracija može imati ; na kraju
        if (!check(TokenType.EOF) && !check(TokenType.FUNCTION) && !check(TokenType.ENUM) && 
            !check(TokenType.INT) && !check(TokenType.REAL) && !check(TokenType.BOOL)) {
            consume(TokenType.SEMICOLON, "Očekivano ';' posle enum deklaracije")
        }
        
        return EnumNode(nameToken = name, values)
    }

    private fun parseParams(): List<ParamNode> {
        val params = mutableListOf<ParamNode>()
        
        if (!check(TokenType.LPAREN)) {
            return params
        }
        
        advance() // consume LPAREN
        
        if (check(TokenType.RPAREN)) {
            advance()
            return params
        }

        do {
            val name = consume(TokenType.IDENT, "Očekivan identifikator za parametar")
            consume(TokenType.COLON, "Očekivano ':' posle imena parametra")
            val type = consumeType()
            params.add(ParamNode(nameToken = name, type))
        } while (match(TokenType.COMMA))

        consume(TokenType.RPAREN, "Očekivano ')' posle parametara")
        return params
    }

    private fun consumeType(): Token {
        return when {
            match(TokenType.INT) -> previous()
            match(TokenType.REAL) -> previous()
            match(TokenType.BOOL) -> previous()
            else -> throw error("Očekivan tip (int, real ili bool)")
        }
    }

    private fun parseStatement(): StatementNode {
        return when {
            // Check for "begin if" or "begin for"
            check(TokenType.BEGIN) && current + 1 < tokens.size -> {
                when (tokens[current + 1].type) {
                    TokenType.IF -> {
                        advance() // consume BEGIN
                        advance() // consume IF
                        parseIf()
                    }
                    TokenType.FOR -> {
                        advance() // consume BEGIN
                        advance() // consume FOR
                        parseFor()
                    }
                    else -> {
                        advance() // consume BEGIN
                        parseBlock()
                    }
                }
            }
            // Variable declaration
            match(TokenType.INT, TokenType.REAL, TokenType.BOOL) -> {
                val type = previous()
                parseVarDecl(type)
            }
            // Print statement
            match(TokenType.PRINT) -> {
                val expr = parsePrintCall()
                consume(TokenType.SEMICOLON, "Očekivano ';' posle 'print' naredbe")
                PrintNode(expr)
            }
            // Read statement
            match(TokenType.READ) -> {
                val target = parseReadCall()
                consume(TokenType.SEMICOLON, "Očekivano ';' posle 'read' naredbe")
                ReadNode(target)
            }
            // Return statement
            match(TokenType.RETURN) -> {
                val expr = if (!check(TokenType.SEMICOLON) && !check(TokenType.END) && !check(TokenType.EOF) && 
                            !check(TokenType.ELSE) && !check(TokenType.OR)) {
                    parseExpression()
                } else null
                // Return statement može imati ; ako nije poslednji u bloku
                // Ako je poslednji u bloku, END će biti sledeći token
                if (!check(TokenType.END) && !check(TokenType.EOF)) {
                    consume(TokenType.SEMICOLON, "Očekivano ';' posle 'return' naredbe")
                }
                ReturnNode(expr)
            }
            // If statement
            match(TokenType.IF) -> parseIf()
            // For loop
            match(TokenType.FOR) -> parseFor()
            // Block
            match(TokenType.BEGIN) -> parseBlock()
            // Assignment or expression statement
            else -> {
                val expr = parseExpression()
                if (match(TokenType.ASSIGN_ARROW)) {
                    val target = parseExpression()
                    consume(TokenType.SEMICOLON, "Očekivano ';' posle dodele vrednosti")
                    AssignmentNode(expr, target)
                } else {
                    consume(TokenType.SEMICOLON, "Očekivano ';' posle izraza")
                    ExpressionStatementNode(expr)
                }
            }
        }
    }

    private fun parseVarDecl(type: Token): VarDeclNode {
        val dimensions = mutableListOf<ExpressionNode>()
        
        // Parse array dimensions
        while (match(TokenType.LBRACKET)) {
            val dimExpr = if (match(TokenType.INT_LITERAL)) {
                LiteralNode(previous())
            } else {
                throw error("Očekivan celobrojni literal za dimenziju niza")
            }
            dimensions.add(dimExpr)
            consume(TokenType.RBRACKET, "Očekivano ']' posle dimenzije")
        }

        val vars = mutableListOf<VarDeclItemNode>()
        vars.add(VarDeclItemNode(nameToken = consume(TokenType.IDENT, "Očekivan identifikator za varijablu")))
        
        while (match(TokenType.COMMA)) {
            vars.add(VarDeclItemNode(nameToken = consume(TokenType.IDENT, "Očekivan identifikator za varijablu")))
        }

        consume(TokenType.SEMICOLON, "Očekivano ';' posle deklaracije varijable")
        
        return VarDeclNode(type, dimensions, vars)
    }

    private fun parsePrintCall(): ExpressionNode {
        consume(TokenType.LPAREN, "Očekivano '(' posle 'print'")
        val expr = parseExpression()
        consume(TokenType.RPAREN, "Očekivano ')' posle izraza")
        return expr
    }

    private fun parseReadCall(): ExpressionNode {
        consume(TokenType.LPAREN, "Očekivano '(' posle 'read'")
        val target = parseExpression()
        consume(TokenType.RPAREN, "Očekivano ')' posle izraza")
        return target
    }

    private fun parseIf(): IfNode {
        // IF (and possibly BEGIN) has already been consumed
        consume(TokenType.LPAREN, "Očekivano '(' posle 'if'")
        val condition = parseExpression()
        consume(TokenType.RPAREN, "Očekivano ')' posle uslova")
        
        val thenBranch = parseStatement()
        val orIfBranches = mutableListOf<OrIfBranchNode>()
        
        while (match(TokenType.OR)) {
            consume(TokenType.IF, "Očekivana ključna reč 'if' posle 'or'")
            consume(TokenType.LPAREN, "Očekivano '(' posle 'if'")
            val orCondition = parseExpression()
            consume(TokenType.RPAREN, "Očekivano ')' posle uslova")
            val orBody = parseStatement()
            orIfBranches.add(OrIfBranchNode(orCondition, orBody))
        }
        
        val elseBranch = if (match(TokenType.ELSE)) {
            parseStatement()
        } else null
        
        // END is optional for if statements
        if (match(TokenType.END)) {
            // END without keyword is fine for if
        }
        
        return IfNode(condition, thenBranch, orIfBranches, elseBranch)
    }

    private fun parseFor(): ForNode {
        // FOR (and possibly BEGIN) has already been consumed
        consume(TokenType.LPAREN, "Očekivano '(' posle 'for'")
        val varName = consume(TokenType.IDENT, "Očekivan identifikator za kontrolnu varijablu")
        consume(TokenType.GOES, "Očekivana ključna reč 'goes'")
        consume(TokenType.FROM, "Očekivana ključna reč 'from'")
        val fromExpr = parseExpression()
        consume(TokenType.TO, "Očekivana ključna reč 'to'")
        val toExpr = parseExpression()
        consume(TokenType.RPAREN, "Očekivano ')' posle izraza")
        
        val body = parseStatement()
        
        // For loops require "end for"
        if (match(TokenType.END)) {
            consume(TokenType.FOR, "Očekivana ključna reč 'for' posle 'end'")
        }
        
        return ForNode(varName, fromExpr, toExpr, body)
    }

    private fun parseBlock(): BlockNode {
        val statements = mutableListOf<StatementNode>()
        
        while (!check(TokenType.END) && !isAtEnd()) {
            statements.add(parseStatement())
        }
        
        consume(TokenType.END, "Očekivana ključna reč 'end'")
        
        return BlockNode(statements)
    }

    private fun parseExpression(): ExpressionNode {
        return parseLogicalOr()
    }

    private fun parseLogicalOr(): ExpressionNode {
        var expr = parseLogicalAnd()
        
        while (match(TokenType.OR)) {
            val op = previous()
            val right = parseLogicalAnd()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseLogicalAnd(): ExpressionNode {
        var expr = parseEquality()
        
        while (match(TokenType.AND)) {
            val op = previous()
            val right = parseEquality()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseEquality(): ExpressionNode {
        var expr = parseRelational()
        
        while (match(TokenType.EQ, TokenType.NE)) {
            val op = previous()
            val right = parseRelational()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseRelational(): ExpressionNode {
        var expr = parseAdditive()
        
        while (match(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE)) {
            val op = previous()
            val right = parseAdditive()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseAdditive(): ExpressionNode {
        var expr = parseMultiplicative()
        
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous()
            val right = parseMultiplicative()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseMultiplicative(): ExpressionNode {
        var expr = parseUnary()
        
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val op = previous()
            val right = parseUnary()
            expr = BinaryExprNode(expr, op, right)
        }
        
        return expr
    }

    private fun parseUnary(): ExpressionNode {
        if (match(TokenType.MINUS, TokenType.NOT)) {
            val op = previous()
            val right = parseUnary()
            return UnaryExprNode(op, right)
        }
        
        return parsePrimary()
    }

    private fun parsePrimary(): ExpressionNode {
        if (match(TokenType.INT_LITERAL, TokenType.REAL_LITERAL, TokenType.BOOL_LITERAL)) {
            return LiteralNode(previous())
        }
        
        if (match(TokenType.IDENT)) {
            val name = previous()
            
            // Check if it's a function call
            if (match(TokenType.LPAREN)) {
                val args = mutableListOf<ExpressionNode>()
                
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while (match(TokenType.COMMA))
                }
                
                consume(TokenType.RPAREN, "Očekivano ')' posle argumenata")
                return CallNode(nameToken = name, args)
            }
            
            // Check if it's an array access
            val indices = mutableListOf<ExpressionNode>()
            while (match(TokenType.LBRACKET)) {
                indices.add(parseExpression())
                consume(TokenType.RBRACKET, "Očekivano ']' posle indeksa")
            }
            
            return VariableNode(nameToken = name, indices)
        }
        
        if (match(TokenType.LPAREN)) {
            val expr = parseExpression()
            consume(TokenType.RPAREN, "Očekivano ')' posle izraza")
            return expr
        }
        
        throw error("Očekivan izraz")
    }
}

