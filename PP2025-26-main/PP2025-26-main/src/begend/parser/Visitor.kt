package begend.parser

import begend.lexer.Token


interface NodeVisitor<T> {
    // Top-level nodes
    fun visit(node: ProgramNode): T
    fun visit(node: EnumNode): T
    fun visit(node: EnumValueNode): T
    
    // Functions
    fun visit(node: FunctionNode): T
    fun visit(node: ParamNode): T
    
    // Statements
    fun visit(node: VarDeclNode): T
    fun visit(node: VarDeclItemNode): T
    fun visit(node: AssignmentNode): T
    fun visit(node: PrintNode): T
    fun visit(node: ReadNode): T
    fun visit(node: IfNode): T
    fun visit(node: OrIfBranchNode): T
    fun visit(node: ForNode): T
    fun visit(node: ReturnNode): T
    fun visit(node: BlockNode): T
    fun visit(node: ExpressionStatementNode): T
    
    // Expressions
    fun visit(node: BinaryExprNode): T
    fun visit(node: UnaryExprNode): T
    fun visit(node: LiteralNode): T
    fun visit(node: VariableNode): T
    fun visit(node: CallNode): T
    
    // Helper nodes
    fun visit(node: TerminalNode): T
    fun <TNode : Node> visit(node: NodeListWrapper<TNode>): T
}

