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
package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.SourceRange

/** The facts of a label that the branching rules of `GOTO`, `EXIT` and `CONTINUE` need. */
internal object LocalLabelFacts {
    private val LOOPS = setOf(PlSqlGrammar.LOOP_STATEMENT, PlSqlGrammar.WHILE_STATEMENT, PlSqlGrammar.FOR_STATEMENT)

    /** Statements a `GOTO` cannot branch into from outside. */
    private val COMPOUNDS = setOf(PlSqlGrammar.LOOP_STATEMENT, PlSqlGrammar.WHILE_STATEMENT, PlSqlGrammar.EXCEPTION_HANDLER)

    /** Statements whose branches a `GOTO` can neither enter nor cross. */
    private val BRANCHING = setOf(PlSqlGrammar.IF_STATEMENT, PlSqlGrammar.CASE_STATEMENT)

    /** [region] is the range of the region declaring the label: the search for its compound stays strictly inside it. */
    fun of(label: AstNode, labeled: LocalRegionId?, region: SourceRange, fileId: FileId): LocalLabel {
        val statement = label.parent
        val compound = compound(statement) { strictlyInside(LocalSyntax.range(fileId, it), region) }
        return LocalLabel(labeled, LocalSyntax.range(fileId, statement), statement.type in LOOPS, compound?.let { LocalSyntax.range(fileId, it) })
    }

    /**
     * The innermost node of the label's region holding [statement] that a `GOTO` cannot enter from outside: a branch of
     * an `IF` (its THEN statements, an `ELSIF` or `ELSE` clause) or of a `CASE` (one WHEN or ELSE statement list), a loop
     * or an exception handler.
     */
    private fun compound(statement: AstNode, inRegion: (AstNode) -> Boolean): AstNode? {
        var node = statement
        var parent = node.parentOrNull
        while (parent != null && inRegion(parent)) {
            when {
                node.type === PlSqlGrammar.ELSIF_CLAUSE || node.type === PlSqlGrammar.ELSE_CLAUSE -> return node
                parent.type in BRANCHING -> return node
                parent.type in COMPOUNDS -> return parent
            }
            node = parent
            parent = node.parentOrNull
        }
        return null
    }

    /**
     * The node forming the region covers the whole region (or more, with its labels), so the walk stops there without
     * repeating which nodes form regions.
     */
    private fun strictlyInside(node: SourceRange, region: SourceRange): Boolean {
        val startsInside = node.startLine > region.startLine || node.startLine == region.startLine && node.startColumn >= region.startColumn
        val endsInside = node.endLine < region.endLine || node.endLine == region.endLine && node.endColumn <= region.endColumn
        return startsInside && endsInside && node != region
    }
}
