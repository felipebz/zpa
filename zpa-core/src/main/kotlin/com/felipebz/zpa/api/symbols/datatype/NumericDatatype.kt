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
package com.felipebz.zpa.api.symbols.datatype

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.PlSqlType

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
        this.scale = if (scale != null && scale > 0) scale else null
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
