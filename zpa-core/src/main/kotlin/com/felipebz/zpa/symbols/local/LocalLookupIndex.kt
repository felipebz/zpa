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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.project.OracleIdentifier

/**
 * Lookup tables derived from a [LocalSemanticModel]: the declarations of each region by name, and the unit forming each
 * region. A pure function of the model, built once on the first lookup.
 */
internal class LocalLookupIndex(model: LocalSemanticModel) {
    private val byName: Array<Map<OracleIdentifier, List<LocalDeclaration>>> = Array(model.regions.size) { index ->
        model.region(LocalRegionId(index)).declarations.map(model::declaration).groupBy { it.name }
    }

    private val units: Array<LocalUnit?> = arrayOfNulls<LocalUnit>(model.regions.size).also { units ->
        for (declaration in model.declarations) {
            val unit = declaration.details as? LocalUnit ?: continue
            unit.region?.let { if (units[it.ordinal] == null) units[it.ordinal] = unit }
        }
    }

    /** The declarations of [region] named [name], in declaration order. */
    fun named(region: LocalRegionId, name: OracleIdentifier): List<LocalDeclaration> = byName[region.ordinal][name].orEmpty()

    /** The package, package body, object type or type body forming [region]. */
    fun unit(region: LocalRegionId): LocalUnit? = units[region.ordinal]
}
