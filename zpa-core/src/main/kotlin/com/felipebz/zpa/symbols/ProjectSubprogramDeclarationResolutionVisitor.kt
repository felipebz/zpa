//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*

/**
 * Associates immediate PACKAGE BODY declaration nodes with their already extracted facts.
 * The source range is the declaration identity; no header is parsed a second time.
 */
internal class ProjectSubprogramDeclarationResolutionVisitor(
    projectSymbolIndex: ProjectSymbolIndex,
    private val fileId: FileId
) : PlSqlCheck() {

    private val bodyDeclarationsByKindAndRange = projectSymbolIndex.declarationsFor(fileId)
        .filterIsInstance<PackageSubprogramDeclaration>()
        .filter { it.fileId == fileId && it.role == DeclarationRole.BODY }
        .groupBy { it.kind to it.sourceRange }

    init {
        subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        if (!isImmediatePackageBodySubprogram(node)) return

        val kind = when (node.type) {
            PlSqlGrammar.PROCEDURE_DECLARATION -> ProjectDeclarationKind.PACKAGE_PROCEDURE
            PlSqlGrammar.FUNCTION_DECLARATION -> ProjectDeclarationKind.PACKAGE_FUNCTION
            else -> return
        }
        val range = node.sourceRange() ?: return
        val candidates = bodyDeclarationsByKindAndRange[kind to range].orEmpty()
        if (candidates.size == 1) {
            (node as SemanticAstNode).projectSubprogramDeclaration = candidates.single()
        }
    }

    private fun isImmediatePackageBodySubprogram(node: AstNode): Boolean =
        node.parentOrNull?.type == PlSqlGrammar.DECLARE_SECTION &&
            node.parentOrNull?.parentOrNull?.type == PlSqlGrammar.CREATE_PACKAGE_BODY

    private fun AstNode.sourceRange(): SourceRange? {
        val first = tokenOrNull ?: return null
        val last = lastTokenOrNull ?: return null
        return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
    }
}
