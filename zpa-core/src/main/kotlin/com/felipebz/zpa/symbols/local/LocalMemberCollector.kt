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
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.SourceRange

/**
 * A name written as a member of a value: after a call or an element access (`fonts(k).n`, `a(i).b.c`, `a(i).b(j).c`),
 * after `TREAT(… AS t)`, after a parenthesized expression (`(expr).n`), or in the SQL `CALL` command. `t.*` is not one. It is not a name lookup: it binds nothing.
 * [follows] is the range of the expression written before its dot.
 */
internal class LocalMember(val name: OracleIdentifier, val range: SourceRange, val follows: SourceRange) {
    override fun toString() = "$name@${range.startLine}:${range.startColumn} after ${follows.startLine}:${follows.startColumn}-${follows.endLine}:${follows.endColumn}"
}

/** Collects the [LocalMember]s of a file from its syntax tree, as AST-detached facts. */
internal object LocalMemberCollector {

    fun collect(root: AstNode, fileId: FileId): List<LocalMember> {
        val members = ArrayList<LocalMember>()
        for (chain in LocalSyntax.descendants(root, PlSqlGrammar.OBJECT_REFERENCE, PlSqlGrammar.MEMBER_EXPRESSION, DdlGrammar.CALL_COMMAND)) {
            when {
                chain.type === PlSqlGrammar.OBJECT_REFERENCE -> Chain(chain.children, 0, fileId, members).walk()
                chain.type === DdlGrammar.CALL_COMMAND -> Chain(chain.children, 1, fileId, members).walk()
                chain.firstChild.type === PlSqlGrammar.BRACKED_EXPRESSION -> Chain(chain.children, 0, fileId, members).walk()
            }
        }
        return members.sortedWith(compareBy({ it.range.startLine }, { it.range.startColumn }))
    }

    /** The parts after [children]`[head]`; a name after a dot is a member, a name after `%` an attribute. */
    private class Chain(
        private val children: List<AstNode>,
        private val head: Int,
        private val fileId: FileId,
        private val out: MutableList<LocalMember>
    ) {
        private val start = children[head].token!!
        private var end = children[head].lastToken!!

        fun walk() {
            for (index in head + 1 until children.size) {
                val child = children[index]
                if (children[index - 1].type === PlSqlPunctuator.DOT) part(child)
                if (child.type !== PlSqlPunctuator.DOT && child.hasToken()) end = child.lastToken!!
            }
        }

        /** A part after a dot: a name, a qualified name (`b.c`), or a call or element access (`b(j)`). */
        private fun part(node: AstNode) {
            when (node.type) {
                PlSqlGrammar.METHOD_CALL -> part(node.firstChild)
                PlSqlGrammar.MEMBER_EXPRESSION -> {
                    val children = node.children
                    part(children[0])
                    for (index in 1 until children.size) {
                        if (children[index - 1].type === PlSqlPunctuator.DOT) member(children[index])
                        if (children[index].type !== PlSqlPunctuator.DOT) end = children[index].lastToken ?: end
                    }
                }
                else -> member(node)
            }
        }

        private fun member(node: AstNode) {
            // Only a name is a member: not the `*` of `t.*`.
            if (!node.hasToken() || node.type is PlSqlPunctuator) return
            val token = node.token
            val first = start
            out += LocalMember(
                OracleIdentifier.fromSource(token.originalValue),
                SourceRange(fileId, token.line, token.column, token.endLine, token.endColumn),
                SourceRange(fileId, first.line, first.column, end.endLine, end.endColumn)
            )
            end = node.lastToken!!
        }
    }
}
