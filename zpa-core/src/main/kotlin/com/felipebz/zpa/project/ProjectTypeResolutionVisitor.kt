package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol

/** Adds project resolution facts without changing the legacy datatype result. */
internal class ProjectTypeResolutionVisitor(
    private val resolver: ProjectTypeResolver,
    private val fileId: FileId
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.DATATYPE)
    }

    override fun visitNode(node: AstNode) {
        val reference = namedTypeReference(node) ?: return
        if (isResolvedLexically(reference, context.currentScope)) return

        val owner = node.getFirstAncestorOrNull(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
            ?.let(::packageName)
        val resolution = resolver.resolve(
            reference,
            ProjectTypeLookupContext(owner)
        )
        (node as SemanticAstNode).projectTypeResolution = resolution
        if (resolution is ProjectTypeResolution.Resolved) {
            declaredTypeSymbol(node)?.projectTypeDeclaration = resolution.declaration
        }
    }

    /** Only direct declared types, never collection elements or record fields' enclosing type. */
    private fun declaredTypeSymbol(datatype: AstNode): Symbol? {
        val declaration = datatype.parent
        when (declaration.type) {
            PlSqlGrammar.VARIABLE_DECLARATION,
            PlSqlGrammar.PARAMETER_DECLARATION,
            PlSqlGrammar.CURSOR_PARAMETER_DECLARATION,
            PlSqlGrammar.CUSTOM_SUBTYPE,
            PlSqlGrammar.ITERAND_DECLARATION,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION -> Unit
            else -> return null
        }
        val identifier = declaration.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)
        return (identifier as? SemanticAstNode)?.symbol
    }

    private fun namedTypeReference(node: AstNode): NamedTypeRef? {
        val customDatatype = node.getFirstChildOrNull(PlSqlGrammar.CUSTOM_DATATYPE) ?: return null
        val nameNode = customDatatype.getFirstChildOrNull(
            PlSqlGrammar.MEMBER_EXPRESSION,
            PlSqlGrammar.VARIABLE_NAME
        ) ?: return null
        val identifiers = nameNode.getDescendants(PlSqlGrammar.IDENTIFIER_NAME)
            .map { OracleIdentifier.fromSource(it.tokenOriginalValue) }
        if (identifiers.isEmpty()) return null

        val firstToken = nameNode.tokenOrNull ?: return null
        val lastToken = nameNode.lastTokenOrNull ?: return null
        return NamedTypeRef(
            QualifiedName(identifiers),
            SourceRange(fileId, firstToken.line, firstToken.column, lastToken.endLine, lastToken.endColumn)
        )
    }

    private fun isResolvedLexically(reference: NamedTypeRef, scope: Scope?): Boolean {
        if (scope == null) return false
        val path = reference.name.segments.dropLast(1)
            .map { it.originalSpelling }
            .reversed()
        return scope.getSymbol(reference.name.last.originalSpelling, path, Symbol.Kind.TYPE) != null
    }

    private fun packageName(node: AstNode): QualifiedName? {
        val unitName = node.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME) ?: return null
        val identifiers = unitName.getDescendants(PlSqlGrammar.IDENTIFIER_NAME)
            .map { OracleIdentifier.fromSource(it.tokenOriginalValue) }
        return identifiers.takeIf { it.isNotEmpty() }?.let(::QualifiedName)
    }
}
