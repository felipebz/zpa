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
package com.felipebz.zpa.project

/**
 * Project resolution information for the type of an already identified RECORD field.
 * This wrapper keeps unsupported type-reference forms explicit while preserving the
 * complete ProjectTypeResolution for named references.
 */
internal sealed interface ProjectRecordFieldTypeResolution {
    val field: ProjectRecordField

    data class Named(
        override val field: ProjectRecordField,
        val resolution: ProjectTypeResolution
    ) : ProjectRecordFieldTypeResolution {
        init {
            require(field.typeRef == resolution.reference) {
                "Named field type resolution must refer to the field type"
            }
        }
    }

    data class Unsupported(
        override val field: ProjectRecordField,
        val typeRef: TypeRef
    ) : ProjectRecordFieldTypeResolution {
        init {
            require(field.typeRef == typeRef) {
                "Unsupported field type resolution must refer to the field type"
            }
            require(typeRef !is NamedTypeRef) {
                "Named field type references must use Named resolution"
            }
        }
    }
}
