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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.project.OracleIdentifier

/**
 * An exact Oracle data dictionary name as a [ProjectIdentifier]: the name is kept verbatim (never upper-cased, never
 * split on dots), so `EMPLOYEES` equals the source spellings `employees` and `"EMPLOYEES"`, while `Emp` equals only
 * `"Emp"`. The dictionary cannot contain a double quote in a name, so such a name is rejected.
 */
@OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)
internal fun dictionaryIdentifier(name: String, what: String): ProjectIdentifier {
    require(name.isNotEmpty()) { "The $what name must not be empty" }
    require('"' !in name) { "The $what name must not contain a double quote: $name" }
    return ProjectIdentifier.wrap(OracleIdentifier.fromSource("\"$name\""))
}
