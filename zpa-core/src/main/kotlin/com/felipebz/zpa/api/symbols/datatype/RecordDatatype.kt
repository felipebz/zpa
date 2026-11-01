//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols.datatype

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. For a name use
 * PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node. Length,
 * precision and scale are read from the DATATYPE node of the declaration.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. For a name use " +
    "PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node. " +
    "Length, precision and scale are read from the DATATYPE node of the declaration."
)
class RecordDatatype(node: AstNode? = null, currentScope: Scope?, val fields: List<Symbol>) : PlSqlDatatype {
    override val type = PlSqlType.RECORD

    override val name: String = currentScope?.let {
        if (it.identifier != null && it.type == PlSqlGrammar.CREATE_PACKAGE)
            it.identifier + "."
        else "" } +
        node?.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)?.tokenValue

    override fun toString(): String {
        return "Record"
    }
}
