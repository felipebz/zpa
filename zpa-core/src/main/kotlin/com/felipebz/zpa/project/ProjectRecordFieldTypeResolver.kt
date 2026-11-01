//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

/** Resolves only the type reference of an already identified project RECORD field or OBJECT attribute. */
internal class ProjectRecordFieldTypeResolver(
    private val resolver: TypeRefSemanticResolver
) {
    fun resolve(member: ProjectRecordMemberResolution.Resolved): ProjectRecordFieldTypeResolution {
        val field = member.field
        return ProjectRecordFieldTypeResolution(
            field,
            resolver.resolve(field.typeRef, ProjectTypeLookupContext((member.declaration as? PackageTypeDeclaration)?.owner))
        )
    }
}
