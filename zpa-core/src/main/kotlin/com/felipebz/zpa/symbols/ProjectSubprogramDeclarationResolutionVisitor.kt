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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.DeclarationRole
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.PackageSubprogramDeclaration
import com.felipebz.zpa.project.ProjectDeclarationKind
import com.felipebz.zpa.project.ProjectSymbolIndex
import com.felipebz.zpa.project.SourceRange

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
