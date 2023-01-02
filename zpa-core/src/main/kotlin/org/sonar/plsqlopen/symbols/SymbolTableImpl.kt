/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable
import org.sonar.plugins.plsqlopen.api.symbols.datatype.PlSqlDatatype

class SymbolTableImpl : SymbolTable {

    override val symbols = ArrayList<Symbol>()
    override val scopes = LinkedHashSet<Scope>()

    fun addScope(scope: Scope) {
        scopes.add(scope)
    }

    override fun getScopeFor(symbol: AstNode): Scope? {
        for (scope in scopes) {
            if (scope.tree == symbol) {
                return scope
            }
        }
        return null
    }

    fun declareSymbol(name: AstNode, kind: Symbol.Kind, scope: Scope, plSqlDatatype: PlSqlDatatype): Symbol {
        val symbol = Symbol(name, kind, scope, plSqlDatatype)
        symbols.add(symbol)
        scope.addSymbol(symbol)
        return symbol
    }

}
