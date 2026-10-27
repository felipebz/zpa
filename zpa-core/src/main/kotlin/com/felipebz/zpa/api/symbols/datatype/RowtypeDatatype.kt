/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols.datatype

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
class RowtypeDatatype : PlSqlDatatype {
    override val type = PlSqlType.ROWTYPE

    override val name: String? = null

    override fun toString(): String {
        return "Rowtype"
    }
}
