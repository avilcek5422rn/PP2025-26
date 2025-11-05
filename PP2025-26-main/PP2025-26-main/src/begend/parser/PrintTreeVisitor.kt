package begend.parser

/**
 * Visitor za ispis sintaksnog stabla u hijerarhijskom formatu.
 */
class PrintTreeVisitor : NodeVisitor<String> {
    private var indent = 0
    
    private fun indentStr(): String = "  ".repeat(indent)
    
    private fun visitNode(node: Node): String {
        val sb = StringBuilder("${indentStr()}${node.name} (ID: ${node.id})")
        val children = node.getChildren()
        if (children.isNotEmpty()) {
            indent++
            sb.append("\n")
            children.forEachIndexed { index, child ->
                sb.append(child.accept(this))
                if (index < children.size - 1) sb.append("\n")
            }
            indent--
        }
        return sb.toString()
    }
    
    override fun visit(node: ProgramNode): String = visitNode(node)
    override fun visit(node: EnumNode): String = visitNode(node)
    override fun visit(node: EnumValueNode): String = visitNode(node)
    override fun visit(node: FunctionNode): String = visitNode(node)
    override fun visit(node: ParamNode): String = visitNode(node)
    override fun visit(node: VarDeclNode): String = visitNode(node)
    override fun visit(node: VarDeclItemNode): String = visitNode(node)
    override fun visit(node: AssignmentNode): String = visitNode(node)
    override fun visit(node: PrintNode): String = visitNode(node)
    override fun visit(node: ReadNode): String = visitNode(node)
    override fun visit(node: IfNode): String = visitNode(node)
    override fun visit(node: OrIfBranchNode): String = visitNode(node)
    override fun visit(node: ForNode): String = visitNode(node)
    override fun visit(node: ReturnNode): String = visitNode(node)
    override fun visit(node: BlockNode): String = visitNode(node)
    override fun visit(node: ExpressionStatementNode): String = visitNode(node)
    override fun visit(node: BinaryExprNode): String = visitNode(node)
    override fun visit(node: UnaryExprNode): String = visitNode(node)
    override fun visit(node: LiteralNode): String = visitNode(node)
    override fun visit(node: VariableNode): String = visitNode(node)
    override fun visit(node: CallNode): String = visitNode(node)
    override fun visit(node: TerminalNode): String = "${indentStr()}${node.name} (ID: ${node.id})"
    override fun <TNode : Node> visit(node: NodeListWrapper<TNode>): String = visitNode(node)
}

