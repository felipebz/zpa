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
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.local.*

/**
 * Binds object method and constructor calls through the project index: `v.m(…)`, `self.m(…)`, `t.s(…)`, `t(…)`, and
 * a parameterless `v.m` that names a method. A binding is kept only where the local call analysis has none or agrees;
 * a disagreement is recorded in [LocalSemanticFacts.methodCallMismatches] and sets nothing.
 */
internal class ProjectMethodCallVisitor(
    private val typeResolver: ProjectTypeResolver,
    private val memberResolver: ProjectRecordMemberResolver,
    private val index: ProjectSymbolIndex,
    private val fileId: FileId,
    private val members: ProjectRecordMemberResolutionVisitor
) : PlSqlCheck() {
    private val binder = ProjectMethodCallBinder(memberResolver)

    init {
        subscribeTo(PlSqlGrammar.METHOD_CALL, PlSqlGrammar.MEMBER_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        // A call on an expression receiver is bound on the receiver's type.
        if (ProjectReceiverMemberVisitor.isReceiverItem(node)) return
        val call = if (node.type == PlSqlGrammar.METHOD_CALL) methodCall(node) else parameterless(node)
        call ?: return
        val facts = context.localSemanticFacts
        val first = node.token
        if (facts != null && !agrees(facts, facts.localCall(first.line, first.column), call)) {
            facts.methodCallMismatches += "${first.line}:${first.column}"
            return
        }
        (node as SemanticAstNode).projectMethodCall = call
    }

    private fun methodCall(node: AstNode): ProjectMethodCall? {
        val lists = node.getChildren(PlSqlGrammar.ARGUMENTS)
        if (lists.size != 1) return null
        val arguments = callArguments(lists.single())
        val kind = if (node.parent.type == PlSqlGrammar.CALL_STATEMENT) ProjectCallKind.PROCEDURE else ProjectCallKind.FUNCTION
        val called = node.firstChild
        return when {
            called.type == PlSqlGrammar.MEMBER_EXPRESSION -> methods(called)?.let { (set, receiver) -> binder.bind(set, receiver, kind, arguments) }
            called.type == PlSqlGrammar.VARIABLE_NAME && kind == ProjectCallKind.FUNCTION -> objectType(called)?.let { binder.construct(it, arguments) }
            else -> null
        }
    }

    private fun parameterless(node: AstNode): ProjectMethodCall? {
        if (node.parent.type == PlSqlGrammar.METHOD_CALL) return null
        val kind = if (node.parent.type == PlSqlGrammar.CALL_STATEMENT) ProjectCallKind.PROCEDURE else ProjectCallKind.FUNCTION
        return methods(node)?.let { (set, receiver) -> binder.bind(set, receiver, kind, ProjectCallArguments.none()) }
    }

    /** The method set a two-part `x.m` names, through a value, `SELF` or the type name. */
    private fun methods(member: AstNode): Pair<ProjectRecordMemberResolution.Method, ProjectMethodReceiver>? {
        members.decorate(member)
        val children = member.children
        if (children.size != 3 || children[0].type != PlSqlGrammar.VARIABLE_NAME || children[2].type != PlSqlGrammar.IDENTIFIER_NAME) return null
        (member as SemanticAstNode).projectRecordMemberResolution?.let { resolution ->
            val set = resolution as? ProjectRecordMemberResolution.Method ?: return null
            val self = OracleIdentifier.fromSource(children[0].tokenOriginalValue) == SELF
            return set to if (self) ProjectMethodReceiver.SELF else ProjectMethodReceiver.VALUE
        }
        // A base the local side names as anything but a type is not a type-name receiver.
        val facts = context.localSemanticFacts ?: return null
        val base = facts.nameDeclaration(children[0].token.line, children[0].token.column)
        if (base != null && base.kind !in TYPE_KINDS) return null
        val type = objectType(children[0]) ?: return null
        val set = memberResolver.resolve(type, OracleIdentifier.fromSource(children[2].tokenOriginalValue)) as? ProjectRecordMemberResolution.Method
            ?: return null
        return set to ProjectMethodReceiver.TYPE_NAME
    }

    /**
     * The schema object type a one-segment name denotes: the local side proves the name absent, or binds it to that
     * type's specification in this file, and the project holds nothing else of that name.
     */
    private fun objectType(name: AstNode): StandaloneTypeDeclaration? {
        val identifiers = name.getChildren(PlSqlGrammar.IDENTIFIER_NAME)
        if (identifiers.size != 1 || name.children.size != 1) return null
        val facts = context.localSemanticFacts ?: return null
        val token = name.token
        val outcome = facts.firstOutcome(token.line, token.column) ?: return null
        val written = QualifiedName(listOf(OracleIdentifier.fromSource(token.originalValue)))
        val range = SourceRange(fileId, token.line, token.column, token.line, token.column + token.originalValue.length)
        val owner = name.getFirstAncestorOrNull(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
            ?.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
            ?.let { unit -> QualifiedName(unit.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).map { OracleIdentifier.fromSource(it.tokenOriginalValue) }) }
        val type = ((typeResolver.resolve(NamedTypeRef(written, range), ProjectTypeLookupContext(owner)) as? ProjectTypeResolution.Resolved)
            ?.declaration as? StandaloneTypeDeclaration)?.takeIf { it.shape == ProjectTypeShape.OBJECT } ?: return null
        if (index.findOccurrences(type.name).any { it.declaration !== type }) return null
        val local = LocalReferenceOutcomes.isAbsent(outcome) ||
            (type.fileId == fileId && facts.bindsUnitAt(outcome, LocalRangeKey.of(type.sourceRange)))
        return type.takeIf { local }
    }

    private fun agrees(facts: LocalSemanticFacts, local: LocalCallResolution?, call: ProjectMethodCall): Boolean {
        if (local == null) return true
        // The project's method element starts at its first modifier or kind keyword, the local heading at FUNCTION or
        // PROCEDURE: the same method ends at the same token.
        fun sameAs(candidate: ProjectMethodCallCandidate?, declaration: com.felipebz.zpa.symbols.local.LocalDeclarationId): Boolean {
            val range = candidate?.method?.sourceRange?.takeIf { it.fileId == fileId } ?: return false
            val local = facts.declarationRange(declaration)
            return local.endLine == range.endLine && local.endColumn == range.endColumn &&
                (local.startLine > range.startLine || (local.startLine == range.startLine && local.startColumn >= range.startColumn))
        }
        val retained = local.candidates.singleOrNull { it.retained }?.declaration
        return when (local.status) {
            LocalCallResolution.Status.INCOMPLETE, LocalCallResolution.Status.INVALID_NOTATION,
            LocalCallResolution.Status.MEMBER_FROM_STATIC -> true
            // Every applicable candidate is proven incompatible: only a result that selects nothing agrees.
            LocalCallResolution.Status.NO_COMPATIBLE_CANDIDATE -> call.status == ProjectMethodCall.Status.SEVERAL_APPLICABLE ||
                call.status == ProjectMethodCall.Status.AMBIGUOUS || call.status == ProjectMethodCall.Status.UNDECIDED
            LocalCallResolution.Status.RESOLVED, LocalCallResolution.Status.STATIC_ON_INSTANCE -> {
                val same = when (local.status) {
                    LocalCallResolution.Status.RESOLVED -> call.status == ProjectMethodCall.Status.RESOLVED
                    else -> call.status == ProjectMethodCall.Status.STATIC_ON_INSTANCE
                }
                (same && retained != null && sameAs(call.selected, retained)) || call.status == ProjectMethodCall.Status.UNDECIDED ||
                    // The local side narrows by datatypes; a structural ambiguity that holds its candidate is no conflict.
                    ((call.status == ProjectMethodCall.Status.AMBIGUOUS || call.status == ProjectMethodCall.Status.SEVERAL_APPLICABLE) && retained != null &&
                        call.candidates.any { it.applicability == ProjectCallCandidate.Applicability.APPLICABLE && sameAs(it, retained) })
            }
            LocalCallResolution.Status.AMBIGUOUS -> call.status == ProjectMethodCall.Status.AMBIGUOUS ||
                call.status == ProjectMethodCall.Status.SEVERAL_APPLICABLE || call.status == ProjectMethodCall.Status.UNDECIDED
            LocalCallResolution.Status.NO_APPLICABLE_CANDIDATE -> call.status == ProjectMethodCall.Status.NO_APPLICABLE_CANDIDATE ||
                call.status == ProjectMethodCall.Status.WRONG_KIND || call.status == ProjectMethodCall.Status.UNDECIDED
        }
    }

    private companion object {
        val SELF = OracleIdentifier.fromSource("SELF")
        val TYPE_KINDS = setOf(
            LocalDeclarationKind.SUBTYPE, LocalDeclarationKind.RECORD_TYPE, LocalDeclarationKind.COLLECTION_TYPE,
            LocalDeclarationKind.REF_CURSOR_TYPE, LocalDeclarationKind.OBJECT_TYPE
        )
    }
}
