package begend.parser

import begend.lexer.Token

/**
 * Čvor sintaksnog stabla.
 * Svaki čvor ima ID i može da ispiše svoju decu.
 */
sealed class Node(val id: Int, val name: String) {
    companion object {
        private var nextId = 1
        fun getNextId(): Int = nextId++
    }

    abstract fun getChildren(): List<Node>

    abstract fun <T> accept(visitor: NodeVisitor<T>): T
    
    fun printTree(indent: Int = 0): String {
        val indentStr = "  ".repeat(indent)
        val sb = StringBuilder("$indentStr$name (ID: $id)")
        
        val children = getChildren()
        if (children.isNotEmpty()) {
            sb.append("\n")
            children.forEachIndexed { index, child ->
                sb.append(child.printTree(indent + 1))
                if (index < children.size - 1) sb.append("\n")
            }
        }
        
        return sb.toString()
    }
    
    open fun toJson(): String {
        val children = getChildren()
        val childrenJson = if (children.isNotEmpty()) {
            children.joinToString(", ", prefix = "\"children\": [", postfix = "]") { it.toJson() }
        } else {
            ""
        }
        
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"id\": $id, ")
        sb.append("\"name\": \"$name\"")
        if (childrenJson.isNotEmpty()) {
            sb.append(", $childrenJson")
        }
        sb.append("}")
        return sb.toString()
    }
}

// Program node
data class ProgramNode(
    val functions: List<FunctionNode>,
    val enums: List<EnumNode>,
    val statements: List<StatementNode>
) : Node(Node.getNextId(), "Program") {
    override fun getChildren(): List<Node> = functions + enums + statements
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

// Enum declaration
data class EnumNode(
    val nameToken: Token,
    val values: List<EnumValueNode>
) : Node(Node.getNextId(), "Enum") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(nameToken, "EnumName"),
        NodeListWrapper(values, "Values")
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class EnumValueNode(
    val nameToken: Token
) : Node(Node.getNextId(), "EnumValue") {
    override fun getChildren(): List<Node> = listOf(TerminalNode(nameToken, "ValueName"))
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

// Function declaration
data class FunctionNode(
    val nameToken: Token,
    val params: List<ParamNode>,
    val returnType: Token,
    val body: StatementNode
) : Node(Node.getNextId(), "Function") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(nameToken, "FunctionName"),
        NodeListWrapper(params, "Params"),
        TerminalNode(returnType, "ReturnType"),
        body
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class ParamNode(
    val nameToken: Token,
    val type: Token
) : Node(Node.getNextId(), "Param") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(nameToken, "ParamName"),
        TerminalNode(type, "ParamType")
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

// Statements
sealed class StatementNode(id: Int, name: String) : Node(id, name) {
    override fun getChildren(): List<Node> = emptyList()
}

data class VarDeclNode(
    val type: Token,
    val dimensions: List<ExpressionNode>,
    val vars: List<VarDeclItemNode>
) : StatementNode(Node.getNextId(), "VarDecl") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(type, "Type"),
        NodeListWrapper(dimensions, "Dimensions"),
        NodeListWrapper(vars, "Variables")
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class VarDeclItemNode(
    val nameToken: Token
) : Node(Node.getNextId(), "VarDeclItem") {
    override fun getChildren(): List<Node> = listOf(TerminalNode(nameToken, "VarName"))
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class AssignmentNode(
    val expr: ExpressionNode,
    val target: ExpressionNode
) : StatementNode(Node.getNextId(), "Assignment") {
    override fun getChildren(): List<Node> = listOf(expr, target)
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class PrintNode(
    val expr: ExpressionNode
) : StatementNode(Node.getNextId(), "Print") {
    override fun getChildren(): List<Node> = listOf(expr)
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class ReadNode(
    val target: ExpressionNode
) : StatementNode(Node.getNextId(), "Read") {
    override fun getChildren(): List<Node> = listOf(target)
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class IfNode(
    val condition: ExpressionNode,
    val thenBranch: StatementNode,
    val orIfBranches: List<OrIfBranchNode>,
    val elseBranch: StatementNode?
) : StatementNode(Node.getNextId(), "If") {
    override fun getChildren(): List<Node> {
        val result = mutableListOf<Node>()
        result.add(condition)
        result.add(thenBranch)
        result.addAll(orIfBranches)
        if (elseBranch != null) result.add(elseBranch)
        return result
    }
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class OrIfBranchNode(
    val condition: ExpressionNode,
    val body: StatementNode
) : Node(Node.getNextId(), "OrIf") {
    override fun getChildren(): List<Node> = listOf(condition, body)
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class ForNode(
    val varName: Token,
    val fromExpr: ExpressionNode,
    val toExpr: ExpressionNode,
    val body: StatementNode
) : StatementNode(Node.getNextId(), "For") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(varName, "VarName"),
        fromExpr,
        toExpr,
        body
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class ReturnNode(
    val expr: ExpressionNode?
) : StatementNode(Node.getNextId(), "Return") {
    override fun getChildren(): List<Node> = if (expr != null) listOf(expr) else emptyList()
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class BlockNode(
    val statements: List<StatementNode>
) : StatementNode(Node.getNextId(), "Block") {
    override fun getChildren(): List<Node> = statements
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class ExpressionStatementNode(
    val expr: ExpressionNode
) : StatementNode(Node.getNextId(), "ExpressionStatement") {
    override fun getChildren(): List<Node> = listOf(expr)
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

// Expressions
sealed class ExpressionNode(id: Int, name: String) : Node(id, name)

data class BinaryExprNode(
    val left: ExpressionNode,
    val op: Token,
    val right: ExpressionNode
) : ExpressionNode(Node.getNextId(), "BinaryExpr") {
    override fun getChildren(): List<Node> = listOf(
        left,
        TerminalNode(op, "Operator"),
        right
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class UnaryExprNode(
    val op: Token,
    val expr: ExpressionNode
) : ExpressionNode(Node.getNextId(), "UnaryExpr") {
    override fun getChildren(): List<Node> = listOf(
        TerminalNode(op, "Operator"),
        expr
    )
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class LiteralNode(
    val token: Token
) : ExpressionNode(Node.getNextId(), "Literal") {
    override fun getChildren(): List<Node> = listOf(TerminalNode(token, "Value"))
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class VariableNode(
    val nameToken: Token,
    val indices: List<ExpressionNode>
) : ExpressionNode(Node.getNextId(), "Variable") {
    override fun getChildren(): List<Node> {
        val result = mutableListOf<Node>()
        result.add(TerminalNode(nameToken, "VarName"))
        if (indices.isNotEmpty()) {
            result.add(NodeListWrapper(indices, "Indices"))
        }
        return result
    }
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

data class CallNode(
    val nameToken: Token,
    val args: List<ExpressionNode>
) : ExpressionNode(Node.getNextId(), "Call") {
    override fun getChildren(): List<Node> {
        val result = mutableListOf<Node>()
        result.add(TerminalNode(nameToken, "FunctionName"))
        if (args.isNotEmpty()) {
            result.add(NodeListWrapper(args, "Arguments"))
        }
        return result
    }
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
}

// Helper nodes
data class TerminalNode(
    val token: Token,
    val label: String
) : Node(Node.getNextId(), label) {
    override fun getChildren(): List<Node> = emptyList()
    override fun <T> accept(visitor: NodeVisitor<T>): T = visitor.visit(this)
    
    override fun toJson(): String {
        return """{"id": $id, "name": "$name", "token": {"type": "${token.type}", "lexeme": "${token.lexeme}", "line": ${token.line}, "col": ${token.col}}}"""
    }
}

data class NodeListWrapper<T : Node>(
    val nodes: List<T>,
    val label: String
) : Node(Node.getNextId(), label) {
    override fun getChildren(): List<Node> = nodes
    override fun <TResult> accept(visitor: NodeVisitor<TResult>): TResult = visitor.visit(this)
}

