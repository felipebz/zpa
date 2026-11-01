//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
