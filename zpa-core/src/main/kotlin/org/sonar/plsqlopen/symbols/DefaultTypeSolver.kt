/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.symbols

import com.sonar.sslr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

class DefaultTypeSolver {

    fun solve(node: AstNode?): PlSqlType {
        if (node != null) {
            if (node.type === PlSqlGrammar.DATATYPE) {
                return solveDatatype(node)
            } else if (node.type === PlSqlGrammar.LITERAL) {
                return solveLiteral(node)
            }
        }
        return PlSqlType.UNKNOWN
    }

    private fun solveDatatype(node: AstNode): PlSqlType {
        var type = PlSqlType.UNKNOWN
        if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE)) {
            type = PlSqlType.CHARACTER
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE)) {
            type = PlSqlType.NUMERIC
        } else if (node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE)) {
            type = PlSqlType.DATE
        } else if (node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE)) {
            type = PlSqlType.LOB
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE)) {
            type = PlSqlType.BOOLEAN
        } else {
            val anchoredDatatype = node.getFirstChild(PlSqlGrammar.ANCHORED_DATATYPE)
            if (anchoredDatatype != null && anchoredDatatype.lastChild.type === PlSqlKeyword.ROWTYPE) {
                type = PlSqlType.ROWTYPE
            }
        }
        return type
    }

    private fun solveLiteral(node: AstNode): PlSqlType {
        var type = PlSqlType.UNKNOWN
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node)) {
            type = PlSqlType.NULL
        } else if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL)) {
            type = PlSqlType.CHARACTER
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) {
            type = PlSqlType.NUMERIC
        } else if (node.hasDirectChildren(PlSqlTokenType.DATE_LITERAL)) {
            type = PlSqlType.DATE
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_LITERAL)) {
            type = PlSqlType.BOOLEAN
        }
        return type
    }

    private fun isEmptyString(node: AstNode): Boolean {
        val characterLiteral = node.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL) ?: return false
        val value = characterLiteral.tokenValue
        if (value == "''") {
            return true
        }
        if (value.startsWith('n', ignoreCase = true) || value.startsWith('q', ignoreCase = true)) {
            val actualStart = value.indexOf('\'') + 2
            val actualEnd = value.lastIndexOf('\'') - 1
            return actualStart == actualEnd
        }
        return false
    }

}
