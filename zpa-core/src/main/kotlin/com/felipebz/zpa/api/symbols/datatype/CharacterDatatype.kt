//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols.datatype

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.PlSqlType

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
class CharacterDatatype : PlSqlDatatype {

    override val type = PlSqlType.CHARACTER

    override val name: String
        get() = if (this.length == null)
            "VARCHAR2"
        else
            "VARCHAR2(${this.length})"

    val length: Int?

    constructor() {
        length = null
    }

    constructor(length: Int?) {
        this.length = if (length != null && length > 0) length else null
    }

    constructor(node: AstNode? = null) {
        val constraint = node?.firstChildOrNull?.getFirstChildOrNull(PlSqlGrammar.CHARACTER_DATATYPE_CONSTRAINT)
        length = constraint
            ?.getFirstChildOrNull(PlSqlGrammar.LITERAL)
            ?.tokenValue?.toInt()
    }

    override fun toString(): String {
        return "Character{length=$length}"
    }

}
