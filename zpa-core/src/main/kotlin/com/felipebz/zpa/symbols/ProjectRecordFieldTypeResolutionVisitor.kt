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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.NamedTypeRef
import com.felipebz.zpa.project.ProjectRecordFieldTypeResolution
import com.felipebz.zpa.project.ProjectRecordMemberResolution
import com.felipebz.zpa.project.ProjectTypeLookupContext
import com.felipebz.zpa.project.ProjectTypeResolver

/** Resolves only the type reference of a field whose RECORD member identity is known. */
internal class ProjectRecordFieldTypeResolutionVisitor(
    private val resolver: ProjectTypeResolver
) : PlSqlCheck() {

    init {
        subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val memberExpression = node as? SemanticAstNode ?: return
        val member = memberExpression.projectRecordMemberResolution
            as? ProjectRecordMemberResolution.Resolved ?: return
        val field = member.field

        memberExpression.projectRecordFieldTypeResolution = when (val typeRef = field.typeRef) {
            is NamedTypeRef -> ProjectRecordFieldTypeResolution.Named(
                field,
                resolver.resolve(typeRef, ProjectTypeLookupContext(member.declaration.owner))
            )
            else -> ProjectRecordFieldTypeResolution.Unsupported(field, typeRef)
        }
    }
}
