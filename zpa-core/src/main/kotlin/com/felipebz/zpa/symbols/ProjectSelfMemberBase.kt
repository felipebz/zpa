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
import com.felipebz.zpa.symbols.local.LocalSemanticSwitches

/**
 * The project base of a member expression whose first segment is the implicit `SELF` of a method, which has no legacy
 * symbol: the object type that encloses the method, looked up by its written name.
 */
internal class ProjectSelfMemberBase(
    private val typeResolver: ProjectTypeResolver,
    private val memberResolver: ProjectRecordMemberResolver,
    private val fileId: FileId
) {
    fun of(context: PlSqlVisitorContext, first: AstNode, firstMember: OracleIdentifier): StandaloneTypeDeclaration? {
        if (!LocalSemanticSwitches.selfMemberBase) return null
        val facts = context.localSemanticFacts ?: return null
        val token = first.token
        val self = facts.implicitSelf(token.line, token.column) ?: return null
        val range = SourceRange(fileId, token.line, token.column, token.line, token.column + token.originalValue.length)
        val type = ((typeResolver.resolve(NamedTypeRef(self.typeName, range)) as? ProjectTypeResolution.Resolved)?.declaration
            as? StandaloneTypeDeclaration)?.takeIf { it.shape == ProjectTypeShape.OBJECT } ?: return null
        val project = memberResolver.resolve(type, firstMember)
        val agrees = when (val local = self.member) {
            is LocalSelfMember.Attribute -> project is ProjectRecordMemberResolution.Resolved &&
                project.field.sourceRange.fileId == fileId && LocalRangeKey.of(project.field.sourceRange) == local.declaration
            LocalSelfMember.Undeclared -> project !is ProjectRecordMemberResolution.Resolved
            LocalSelfMember.Other -> true
        }
        if (!agrees) {
            facts.selfMismatches += "${token.line}:${token.column}"
            return null
        }
        return type
    }
}
