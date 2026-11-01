//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlFile
import java.util.*

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. For names use the read queries of
 * PlSqlVisitorContext: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty, valueCategory
 * and typeCategory. For properties of a unit (autonomous transaction, overriding member and the others) read the unit's
 * AST.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. For names use the read " +
    "queries of PlSqlVisitorContext: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
    "regionUncertainty, valueCategory and typeCategory. For properties of a unit (autonomous transaction, " +
    "overriding member and the others) read the unit's AST."
)
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
