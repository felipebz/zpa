/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.local.LocalRangeKey
import com.felipebz.zpa.symbols.local.LocalTypeGate

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
        val facts = context.localSemanticFacts
        // Without local facts (a failed prepass), nothing proves the name local: the project resolves it.
        val gate = facts?.localTypeGate(reference.sourceRange) ?: LocalTypeGate.NotLocal
        if (gate == LocalTypeGate.Local) return

        val owner = enclosingPackageName(node)
        val resolution = resolver.resolve(
            reference,
            ProjectTypeLookupContext(owner)
        )
        // A declaration both sides hold: the project must resolve to the one the local side binds; otherwise the local
        // declaration decides.
        if (gate is LocalTypeGate.Held && resolution is ProjectTypeResolution.Resolved &&
            (resolution.declaration.fileId != fileId || LocalRangeKey.of(resolution.declaration.sourceRange) != gate.declaration)
        ) {
            return
        }
        if (gate is LocalTypeGate.EnclosingType && resolution is ProjectTypeResolution.Resolved &&
            (resolution.declaration !is StandaloneTypeDeclaration || resolution.declaration.qualifiedName != gate.name)
        ) {
            return
        }
        (node as SemanticAstNode).projectTypeResolution = resolution
        if (resolution is ProjectTypeResolution.Resolved && isDirectDeclaredType(node)) {
            declaredTypeSymbol(node)?.projectTypeDeclaration = resolution.declaration
            val last = node.lastToken
            facts?.recordDeclaredType(SourceRange(fileId, node.token.line, node.token.column, last.endLine, last.endColumn), resolution.declaration)
        }
    }

    /** Only direct declared types, never collection elements or record fields' enclosing type. */
    private fun isDirectDeclaredType(datatype: AstNode): Boolean = when (datatype.parent.type) {
        PlSqlGrammar.VARIABLE_DECLARATION,
        PlSqlGrammar.PARAMETER_DECLARATION,
        PlSqlGrammar.CURSOR_PARAMETER_DECLARATION,
        PlSqlGrammar.CUSTOM_SUBTYPE,
        PlSqlGrammar.ITERAND_DECLARATION,
        PlSqlGrammar.CREATE_FUNCTION,
        PlSqlGrammar.FUNCTION_DECLARATION -> true
        else -> false
    }

    @Suppress("DEPRECATION")
    private fun declaredTypeSymbol(datatype: AstNode): Symbol? {
        val identifier = datatype.parent.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)
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
}
