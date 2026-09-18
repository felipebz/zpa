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
package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * A table reference from a SQL table source.
 *
 * Component properties contain the original spelling of each identifier. In
 * particular, quoted identifier delimiters and case are preserved. Separators
 * are not included in the individual properties; [databaseLink] contains all
 * of the link's identifier components joined with dots. This view describes
 * syntax only and does not resolve the referenced database object.
 */
@ZpaExperimentalApi
public interface TableReference : SyntaxView {

    /** The optional schema component, preserving its original spelling. */
    public val schema: String?

    /** The table-reference name, preserving its original spelling. */
    public val name: String

    /** The optional database-link suffix, preserving its original spelling. */
    public val databaseLink: String?
}
