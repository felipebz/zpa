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
package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlFile
import java.util.*

interface Scope {
    val isAutonomousTransaction: Boolean
    val isOverridingMember: Boolean
    val symbols: List<Symbol>
    val tree: AstNode?
    val outer: Scope?
    val identifier: String?
    val hasExceptionHandler: Boolean
    val path: List<String>
    val innerScopes: List<Scope>
    val type: AstNodeType?
    val isGlobal: Boolean
    val plSqlFile: PlSqlFile?
    val firstToken: Token?
    val lastToken: Token?

    /**
     * @param kind of the symbols to look for
     * @return the symbols corresponding to the given kind
     */
    fun getSymbols(kind: Symbol.Kind): List<Symbol>

    fun getSymbolsAcessibleInScope(name: String, vararg kinds: Symbol.Kind): Deque<Symbol>
    fun addSymbol(symbol: Symbol)
    fun getSymbol(name: String, vararg kinds: Symbol.Kind): Symbol?
    fun getSymbol(name: String, path: List<String>, vararg kinds: Symbol.Kind): Symbol?
    fun addInnerScope(scope: Scope)
}
