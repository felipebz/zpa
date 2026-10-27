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
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.local.LocalRangeKey
import com.felipebz.zpa.symbols.local.LocalSelfMember

/**
 * The project base of a member expression whose first segment names no local declaration and stands for the implicit
 * `SELF` of a method: `SELF` itself, whose base is the enclosing object type, or an attribute of that type named alone,
 * whose base is the attribute's declared type.
 */
internal class ProjectSelfMemberBase(
    private val typeResolver: ProjectTypeResolver,
    private val memberResolver: ProjectRecordMemberResolver,
    private val fieldTypeResolver: ProjectRecordFieldTypeResolver,
    private val fileId: FileId
) {
    fun of(context: PlSqlVisitorContext, first: AstNode, firstMember: OracleIdentifier): ProjectTypeDeclaration? {
        val facts = context.localSemanticFacts ?: return null
        val token = first.token
        val self = facts.implicitSelf(token.line, token.column) ?: return attribute(facts, first)
        val range = SourceRange(fileId, token.line, token.column, token.line, token.column + token.originalValue.length)
        val type = ((typeResolver.resolve(NamedTypeRef(self.typeName, range)) as? ProjectTypeResolution.Resolved)?.declaration
            as? StandaloneTypeDeclaration)?.takeIf { it.shape == ProjectTypeShape.OBJECT } ?: return null
        val project = memberResolver.resolve(type, firstMember)
        val agrees = when (val local = self.member) {
            is LocalSelfMember.Attribute -> project is ProjectRecordMemberResolution.Resolved &&
                project.field.sourceRange.fileId == fileId && LocalRangeKey.of(project.field.sourceRange) == local.declaration
            LocalSelfMember.Undeclared -> project !is ProjectRecordMemberResolution.Resolved && project !is ProjectRecordMemberResolution.Method
            LocalSelfMember.Other -> true
        }
        return type.takeIf { agrees }
    }

    /**
     * An attribute named alone: decided a specification member from the project (the local side keeps a static method's
     * attribute as a gap, and a local declaration first), resolved on the enclosing type and typed as a path field is.
     */
    private fun attribute(facts: com.felipebz.zpa.symbols.local.LocalSemanticFacts, first: AstNode): ProjectTypeDeclaration? {
        val token = first.token
        val typeName = facts.enclosingTypeOfSpecificationMember(token.line, token.column) ?: return null
        val range = SourceRange(fileId, token.line, token.column, token.line, token.column + token.originalValue.length)
        val type = ((typeResolver.resolve(NamedTypeRef(typeName, range)) as? ProjectTypeResolution.Resolved)?.declaration
            as? StandaloneTypeDeclaration)?.takeIf { it.shape == ProjectTypeShape.OBJECT } ?: return null
        val attribute = memberResolver.resolve(type, OracleIdentifier.fromSource(token.originalValue)) as? ProjectRecordMemberResolution.Resolved
            ?: return null
        val typed = fieldTypeResolver.resolve(attribute).resolution as? TypeRefSemanticResolution.Project ?: return null
        return (typed.resolution as? ProjectTypeResolution.Resolved)?.declaration
    }
}
