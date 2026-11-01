//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/** Semantic TypeRef facts for the type of an already identified RECORD field. */
internal data class ProjectRecordFieldTypeResolution(
    val field: ProjectRecordField,
    val resolution: TypeRefSemanticResolution
) {
    init {
        require(field.typeRef == resolution.reference) {
            "Field type resolution must refer to the field type"
        }
    }
}
