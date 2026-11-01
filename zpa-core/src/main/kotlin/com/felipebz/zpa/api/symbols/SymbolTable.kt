//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. Use the read queries of
 * PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty,
 * valueCategory and typeCategory.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
    "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
    "regionUncertainty, valueCategory and typeCategory."
)
interface SymbolTable {

    val symbols: List<Symbol>

    val scopes: Set<Scope>

    fun getScopeFor(symbol: AstNode): Scope?
}
