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
import com.sonar.sslr.api.AstNodeType
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.util.*

class ScopeImpl(private val outer: Scope?,
                private val node: AstNode?,
                override val isAutonomousTransaction: Boolean,
                private val hasExceptionHandler: Boolean) : Scope {
    private var identifier: String? = null
    private val nameTypes = arrayOf<AstNodeType>(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)

    override val symbols = mutableListOf<Symbol>()

    override fun tree() = node

    override fun outer() = outer

    override fun identifier(): String? {
        if (identifier == null && node != null) {
            identifier = ""
            val identifierNode = node.getFirstChild(*nameTypes)
            if (identifierNode != null) {
                this.identifier = identifierNode.tokenOriginalValue
            }
        }
        return identifier
    }

    override fun hasExceptionHandler() = hasExceptionHandler

    /**
     * @param kind of the symbols to look for
     * @return the symbols corresponding to the given kind
     */
    override fun getSymbols(kind: Symbol.Kind) =
        symbols.filter { it.`is`(kind) }.toList()

    override fun getSymbolsAcessibleInScope(name: String, vararg kinds: Symbol.Kind): Deque<Symbol> {
        val result = ArrayDeque<Symbol>()
        var scope: Scope? = this
        while (scope != null) {
            scope.symbols.filterTo(result) { it.called(name) && (kinds.isEmpty() || kinds.contains(it.kind())) }
            scope = scope.outer()
        }

        return result
    }

    override fun addSymbol(symbol: Symbol) {
        symbols.add(symbol)
    }

    override fun getSymbol(name: String, vararg kinds: Symbol.Kind): Symbol? {
        var scope: Scope? = this
        while (scope != null) {
            for (s in scope.symbols) {
                if (s.called(name) && (kinds.isEmpty() || kinds.contains(s.kind()))) {
                    return s
                }
            }
            scope = scope.outer()
        }

        return null
    }
}
