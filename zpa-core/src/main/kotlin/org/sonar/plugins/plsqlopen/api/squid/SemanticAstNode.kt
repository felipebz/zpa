/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plsqlopen.sslr.Tree
import org.sonar.plsqlopen.sslr.TreeImpl

import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import org.sonar.plugins.plsqlopen.api.symbols.datatype.PlSqlDatatype
import org.sonar.plugins.plsqlopen.api.symbols.datatype.UnknownDatatype

class SemanticAstNode(type: AstNodeType, name: String, token: Token?) : AstNode(type, name, token) {
    constructor(token: Token) : this(token.type, token.type.name, token)

    private var internalTree: Tree? = null

    var symbol: Symbol? = null
        set(symbol) {
            field = symbol

            val child = if (children.count() == 1) children[0] else return
            (child as SemanticAstNode).symbol = symbol
        }

    var plSqlDatatype: PlSqlDatatype = UnknownDatatype
        get() = this.symbol?.datatype ?: field

    val plSqlType: PlSqlType
        get() = plSqlDatatype.type

    val tree: Tree
        get() {
            internalTree?.let { return it }

            var classType = PlSqlGrammarBuilder.classForType(super.type)
            if (classType == TreeImpl::class.java) {
                var node = this
                while (classType == TreeImpl::class.java && node.numberOfChildren == 1) {
                    node = node.firstChild as SemanticAstNode
                    classType = PlSqlGrammarBuilder.classForType(node.type)
                }
            }

            val instance = classType.getDeclaredConstructor(SemanticAstNode::class.java)
                .newInstance(this)
            internalTree = instance
            return instance
        }

    val allTokensToString: String
        get() = tokens.joinToString(" ") { it.originalValue }

    override fun toString(): String {
        return buildString {
            append(name)
            if (tokenOrNull != null) {
                append(" value='").append(token.value).append("'")
                append(" line=").append(token.line)
                append(" column=").append(token.column)
            }
            if (plSqlDatatype !is UnknownDatatype) {
                append(" datatype=")
                append(plSqlDatatype)
            }
        }
    }
}
