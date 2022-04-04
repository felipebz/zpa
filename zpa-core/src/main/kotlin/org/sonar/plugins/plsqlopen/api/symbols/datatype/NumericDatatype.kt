/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.symbols.datatype

import com.felipebz.flr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType

class NumericDatatype : PlSqlDatatype {

    override val type = PlSqlType.NUMERIC
    val length: Int?
    val precision: Int?

    constructor() {
        length = null
        precision = null
    }

    constructor(length: Int?, precision: Int?) {
        this.length = if (length != null && length > 0) length else null
        this.precision = if (precision != null && precision > 0) precision else null
    }

    constructor(node: AstNode? = null) {
        val constraint = node?.firstChildOrNull?.getFirstChildOrNull(PlSqlGrammar.NUMERIC_DATATYPE_CONSTRAINT)
        length = constraint
            ?.getFirstChildOrNull(PlSqlTokenType.INTEGER_LITERAL)
            ?.tokenValue?.toInt()
        precision = constraint
            ?.getLastChildOrNull(PlSqlTokenType.INTEGER_LITERAL)
            ?.tokenValue?.toInt()
    }

}
