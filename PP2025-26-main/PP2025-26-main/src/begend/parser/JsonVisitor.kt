package begend.parser

/**
 * Visitor za konverziju AST-a u JSON format.
 */
class JsonVisitor : NodeVisitor<String> {
    override fun visit(node: ProgramNode): String = node.toJson()
    override fun visit(node: EnumNode): String = node.toJson()
    override fun visit(node: EnumValueNode): String = node.toJson()
    override fun visit(node: FunctionNode): String = node.toJson()
    override fun visit(node: ParamNode): String = node.toJson()
    override fun visit(node: VarDeclNode): String = node.toJson()
    override fun visit(node: VarDeclItemNode): String = node.toJson()
    override fun visit(node: AssignmentNode): String = node.toJson()
    override fun visit(node: PrintNode): String = node.toJson()
    override fun visit(node: ReadNode): String = node.toJson()
    override fun visit(node: IfNode): String = node.toJson()
    override fun visit(node: OrIfBranchNode): String = node.toJson()
    override fun visit(node: ForNode): String = node.toJson()
    override fun visit(node: ReturnNode): String = node.toJson()
    override fun visit(node: BlockNode): String = node.toJson()
    override fun visit(node: ExpressionStatementNode): String = node.toJson()
    override fun visit(node: BinaryExprNode): String = node.toJson()
    override fun visit(node: UnaryExprNode): String = node.toJson()
    override fun visit(node: LiteralNode): String = node.toJson()
    override fun visit(node: VariableNode): String = node.toJson()
    override fun visit(node: CallNode): String = node.toJson()
    override fun visit(node: TerminalNode): String = node.toJson()
    override fun <TNode : Node> visit(node: NodeListWrapper<TNode>): String = node.toJson()
}

