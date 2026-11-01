//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. There is no replacement: it was never
 * meant as API.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. There is no replacement: it " +
    "was never meant as API."
)
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
