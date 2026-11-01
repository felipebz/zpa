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
class NumericDatatype : PlSqlDatatype {

    override val type = PlSqlType.NUMERIC

    override val name: String
        get() = if (this.length == null)
            "NUMBER()"
        else if (this.scale == null)
            "NUMBER(${this.length})"
        else
            "NUMBER(${this.length}, ${this.scale})"

    val length: Int?
    val scale: Int?

    constructor() {
        length = null
        scale = null
    }

    constructor(length: Int?, scale: Int?) {
        this.length = if (length != null && length > 0) length else null
        this.scale = scale
    }

    constructor(node: AstNode? = null) {
        val constraint = node?.firstChildOrNull?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_DATATYPE_CONSTRAINT)
        if (constraint != null) {
            val precisionNode = constraint.getFirstChildOrNull(PlSqlGrammar.NUMERIC_PRECISION)
            length = if (precisionNode != null && precisionNode.hasDirectChildren(PlSqlGrammar.LITERAL)) {
                precisionNode.tokenValue.toInt()
            } else {
                null
            }

            val scaleNode = constraint.getFirstChildOrNull(PlSqlGrammar.NUMERIC_SCALE)
            scale = if (scaleNode != null && scaleNode.hasDirectChildren(PlSqlGrammar.LITERAL)) {
                scaleNode.tokenValue.toInt()
            } else {
                null
            }
        } else {
            length = null
            scale = null
        }
    }

    override fun toString(): String {
        return "Numeric{length=$length, scale=$scale}"
    }

}
