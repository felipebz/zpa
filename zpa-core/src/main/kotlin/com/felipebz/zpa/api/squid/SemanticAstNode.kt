//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype

open class SemanticAstNode(type: AstNodeType, name: String, token: Token?) : AstNode(type, name, token) {
    constructor(token: Token) : this(token.type, token.type.name, token)

    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use
     * PlSqlVisitorContext.nameBinding instead.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use " +
        "PlSqlVisitorContext.nameBinding instead."
    )
    @Suppress("DEPRECATION")
    var symbol: Symbol? = null
        set(symbol) {
            field = symbol

            val child = if (children.count() == 1) children[0] else return
            (child as SemanticAstNode).symbol = symbol
        }

    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. For a name use
     * PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node.
     * Length, precision and scale are read from the DATATYPE node of the declaration.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. For a name use " +
        "PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar " +
        "node. Length, precision and scale are read from the DATATYPE node of the declaration."
    )
    @Suppress("DEPRECATION")
    var plSqlDatatype: PlSqlDatatype = UnknownDatatype
        get() = this.symbol?.datatype ?: field

    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. For a name use
     * PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar node.
     * Length, precision and scale are read from the DATATYPE node of the declaration.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. For a name use " +
        "PlSqlVisitorContext.valueCategory, for a DATATYPE node typeCategory; a literal's kind is its grammar " +
        "node. Length, precision and scale are read from the DATATYPE node of the declaration."
    )
    @Suppress("DEPRECATION")
    val plSqlType: PlSqlType
        get() = plSqlDatatype.type

    val allTokensToString: String
        get() = tokens.joinToString(" ") { it.originalValue }

    @Suppress("DEPRECATION")
    override fun toString(): String {
        return buildString {
            append(name)
            if (tokenOrNull != null) {
                append(" value='").append(token.value).append("'")
                append(" line=").append(token.line)
                append(" column=").append(token.column)
            }
            if (plSqlDatatype !is UnknownDatatype) {
                append(" datatype=")
                append(plSqlDatatype)
            }
        }
    }
}
