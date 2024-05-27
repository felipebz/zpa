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
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ScopeImpl(override val outer: Scope? = null,
                node: AstNode? = null,
                override val firstToken: Token? = null,
                override val lastToken: Token? = null,
                override val isAutonomousTransaction: Boolean = false,
                override val hasExceptionHandler: Boolean = false,
                override val isOverridingMember: Boolean = false,
                identifier: String? = null,
                type: AstNodeType? = null,
                private val globalScope: Scope? = null,
                override val plSqlFile: PlSqlFile? = null) : Scope {

    private val mutex = ReentrantLock()

    init {
        outer?.addInnerScope(this)
    }

    override val tree: AstNode? = node

    override val type: AstNodeType? = type ?: tree?.type

    override val symbols = mutableListOf<Symbol>()

    override val identifier: String? =
        try {
            identifier ?: node?.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)?.tokenOriginalValue
        } catch (e: Exception) {
            ""
        }

    override val path: List<String> by lazy {
        val path = ArrayList<String>()
        var scope: Scope? = this
        while (scope != null) {
            val scopeIdentifier = scope.identifier
            if (scopeIdentifier != null) {
                path.add(scopeIdentifier)
            }
            scope = scope.outer
        }
        path
    }
    override val innerScopes = mutableListOf<Scope>()

    override val isGlobal: Boolean = globalScope != null ||
        (outer as? ScopeImpl)?.globalScope != null ||
        (outer?.type in arrayOf(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_TYPE) && outer?.isGlobal == true)

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
            scope.symbols.filterTo(result) { it.called(name) && (kinds.isEmpty() || kinds.contains(it.kind)) }
            scope = scope.outer
        }

        return result
    }

    override fun addSymbol(symbol: Symbol) {
        symbols.add(symbol)
    }

    override fun addInnerScope(scope: Scope) {
        mutex.withLock {
            innerScopes.add(scope)
        }
    }

    override fun getSymbol(name: String, vararg kinds: Symbol.Kind): Symbol? {
        return getSymbol(name, emptyList(), *kinds)
    }

    override fun getSymbol(name: String, path: List<String>, vararg kinds: Symbol.Kind): Symbol? {
        var scope: Scope? = this
        while (scope != null) {
            for (s in scope.symbols) {
                if (s.called(name) && (path.isEmpty() || pathContainedIn(path, scope)) && (kinds.isEmpty() || kinds.contains(s.kind))) {
                    return s
                }
            }
            scope = scope.outer
        }

        return null
    }

    // check if path is a prefix of this scope's path
    private fun pathContainedIn(path: List<String>, scope: Scope): Boolean {
        if (path.size > scope.path.size) {
            return false
        }
        for (i in path.indices) {
            if (!path[i].equals(scope.path[i], ignoreCase = true)) {
                return false
            }
        }
        return true
    }

    override fun toString() = "Scope name=${identifier ?: "<unnamed>"} type=$type path=$path"

}
