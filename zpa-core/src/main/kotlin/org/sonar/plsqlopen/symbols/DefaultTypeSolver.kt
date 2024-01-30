/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import com.felipebz.flr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import org.sonar.plugins.plsqlopen.api.symbols.datatype.*

open class DefaultTypeSolver {

    fun solve(node: AstNode, scope: Scope?): PlSqlDatatype {
        if (node.type === PlSqlGrammar.DATATYPE) {
            return solveDatatype(node, scope)
        } else if (node.type === PlSqlGrammar.LITERAL) {
            return solveLiteral(node)
        }
        return UnknownDatatype()
    }

    open fun solveDatatype(node: AstNode, scope: Scope?): PlSqlDatatype {
        var type: PlSqlDatatype = UnknownDatatype()
        if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE)) {
            type = CharacterDatatype(node)
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE)) {
            type = NumericDatatype(node)
        } else if (node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE)) {
            type = DateDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE)) {
            type = LobDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE)) {
            type = BooleanDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.ANCHORED_DATATYPE)) {
            val anchoredDatatype = node.firstChild
            if (anchoredDatatype.lastChild.type === PlSqlKeyword.ROWTYPE) {
                type = RowtypeDatatype()
            }
        } else {
            val datatype = node.firstChild
            type = scope?.getSymbol(datatype.tokenOriginalValue, Symbol.Kind.TYPE)?.datatype ?: UnknownDatatype()
        }
        return type
    }

    open fun solveLiteral(node: AstNode): PlSqlDatatype {
        var type: PlSqlDatatype = UnknownDatatype()
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node)) {
            type = NullDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL)) {
            type = CharacterDatatype(node)
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) {
            type = NumericDatatype(node)
        } else if (node.hasDirectChildren(PlSqlTokenType.DATE_LITERAL)) {
            type = DateDatatype()
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_LITERAL)) {
            type = BooleanDatatype()
        }
        return type
    }

    private fun isEmptyString(node: AstNode): Boolean {
        val characterLiteral = node.getFirstChildOrNull(PlSqlGrammar.CHARACTER_LITERAL) ?: return false
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
