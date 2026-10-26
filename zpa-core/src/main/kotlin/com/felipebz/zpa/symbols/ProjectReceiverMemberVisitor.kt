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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectTypedCallResolution
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.ProjectReceiverMemberVisitor.Companion.NOT_A_VALUE
import com.felipebz.zpa.symbols.local.*

/**
 * Members written after an expression receiver (`v(1).a`, `f(1).m()`, `pk.g().fld`, `a(1).b(2).c`): each item of an
 * `OBJECT_REFERENCE` is resolved on the type of the expression before it, with the member, path and method resolvers
 * the other member visitors use.
 *
 * A receiver has a type only when it is certain:
 * - an element of a collection: the name before the parentheses is proven a value (a variable or parameter, a package
 *   variable, a field or an attribute) whose type is a project collection with a resolved element type;
 * - the result of a call bound to one candidate: the local call resolution, the project's typed resolution, or a method
 *   call binding. An ambiguous call has no result type (PLS-00307 on Oracle, even when every candidate returns the same
 *   type).
 *
 * Without parentheses, a method or function name followed by a member is rejected by Oracle (PLS-00225), and receivers
 * inside SQL are not modeled: both keep the other visitors' result. The items are decorated when the file is left, once
 * every written type has its project resolution.
 */
internal class ProjectReceiverMemberVisitor(
    private val typeResolver: TypeRefSemanticResolver,
    private val memberResolver: ProjectRecordMemberResolver,
    private val fieldTypeResolver: ProjectRecordFieldTypeResolver,
    private val pathResolver: ProjectRecordMemberPathResolver,
    private val index: ProjectSymbolIndex,
    private val members: ProjectRecordMemberResolutionVisitor,
    private val paths: ProjectRecordMemberPathResolutionVisitor,
    private val selfBase: ProjectSelfMemberBase
) : PlSqlCheck() {
    private val binder = ProjectMethodCallBinder(memberResolver)
    private val receivers = ArrayList<AstNode>()
    private val datatypes = HashMap<Pair<Int, Int>, SemanticAstNode>()
    private var calls: Map<Pair<Int, Int>, LocalCallAnalysis> = emptyMap()

    init {
        subscribeTo(PlSqlGrammar.OBJECT_REFERENCE, PlSqlGrammar.DATATYPE)
    }

    override fun visitFile(node: AstNode) {
        receivers.clear()
        datatypes.clear()
        calls = emptyMap()
    }

    override fun visitNode(node: AstNode) {
        if (node.type == PlSqlGrammar.DATATYPE) {
            datatypes[node.token.line to node.token.column] = node as SemanticAstNode
        } else if (!inSql(node)) {
            receivers += node
        }
    }

    override fun leaveFile(node: AstNode) {
        calls = context.localSemanticFacts?.calls.orEmpty().associateBy { it.reference.range.startLine to it.reference.range.startColumn }
        receivers.forEach(::decorate)
    }

    /** What a receiver is. */
    enum class Form { ELEMENT, CALL_RESULT, METHOD_RESULT, OTHER }

    /** The outcome of a receiver: its type, or why it has none. */
    sealed interface Base {
        val form: Form

        class Typed(val declaration: ProjectTypeDeclaration, override val form: Form) : Base
        class None(val reason: String, override val form: Form) : Base
    }

    /** The items of [reference] (an `OBJECT_REFERENCE`), each resolved on the type before it, while the types are known. */
    private fun decorate(reference: AstNode) {
        var current = (base(reference.firstChild) as? Base.Typed)?.declaration ?: return
        val items = reference.children.drop(1)
        for ((position, item) in items.withIndex()) {
            if (item.type == PlSqlPunctuator.DOT) continue
            val last = position == items.lastIndex
            current = item(item, current, last, reference) ?: return
        }
    }

    /** Decorates one item on [receiver]; the type of its value when it is known and something may follow it. */
    private fun item(item: AstNode, receiver: ProjectTypeDeclaration, last: Boolean, reference: AstNode): ProjectTypeDeclaration? = when (item.type) {
        PlSqlGrammar.VARIABLE_NAME -> {
            val resolution = memberResolver.resolve(receiver, name(item))
            (item as SemanticAstNode).projectRecordMemberResolution = resolution
            when (resolution) {
                is ProjectRecordMemberResolution.Resolved -> fieldType(item, resolution)
                is ProjectRecordMemberResolution.Method -> {
                    // A method named without parentheses is a call only as the last item (PLS-00225 before a member).
                    if (last) item.projectMethodCall = binder.bind(resolution, ProjectMethodReceiver.VALUE, kind(reference), ProjectCallArguments.none())
                    null
                }
                else -> null
            }
        }
        PlSqlGrammar.MEMBER_EXPRESSION -> {
            val names = memberNames(item)
            if (names == null) null else {
                val path = pathResolver.resolve(receiver, names)
                (item as SemanticAstNode).projectRecordMemberPathResolution = path
                (path as? ProjectRecordMemberPathResolution.Completed)?.let { segmentType(it.segments.last()) }
            }
        }
        PlSqlGrammar.METHOD_CALL -> callItem(item, receiver, last, reference)
        else -> null
    }

    /** `m(…)` or `f(i)` on [receiver]: a method call, or an element of a collection member. */
    private fun callItem(item: AstNode, receiver: ProjectTypeDeclaration, last: Boolean, reference: AstNode): ProjectTypeDeclaration? {
        val lists = item.getChildren(PlSqlGrammar.ARGUMENTS)
        if (lists.isEmpty()) return null
        var type = firstCallItem(item, lists.first(), receiver, last && lists.size == 1, reference)
        for (list in lists.drop(1)) type = elementOf(type ?: return null, list)
        return type
    }

    private fun firstCallItem(item: AstNode, arguments: AstNode, receiver: ProjectTypeDeclaration, last: Boolean, reference: AstNode): ProjectTypeDeclaration? {
        val head = item.firstChild
        // The member the parentheses follow, and the type it is a member of.
        val (owner, member) = when (head.type) {
            PlSqlGrammar.VARIABLE_NAME -> receiver to head
            PlSqlGrammar.MEMBER_EXPRESSION -> {
                val names = memberNames(head) ?: return null
                val path = pathResolver.resolve(receiver, names)
                (head as SemanticAstNode).projectRecordMemberPathResolution = path
                when (path) {
                    is ProjectRecordMemberPathResolution.Completed ->
                        return elementOf(segmentType(path.segments.last()), arguments)
                    is ProjectRecordMemberPathResolution.Stopped -> {
                        if (path.nextMemberOrdinal != names.lastIndex || path.reason != ProjectRecordMemberPathResolution.StopReason.MEMBER_IS_METHOD) return null
                        (segmentType(path.segments.last()) ?: return null) to null
                    }
                }
            }
            else -> return null
        }
        val name = member?.let(::name) ?: memberNames(head)!!.last()
        val resolution = memberResolver.resolve(owner, name)
        member?.let { (it as SemanticAstNode).projectRecordMemberResolution = resolution }
        return when (resolution) {
            is ProjectRecordMemberResolution.Method -> {
                val call = binder.bind(resolution, ProjectMethodReceiver.VALUE, if (last) kind(reference) else ProjectCallKind.FUNCTION, callArguments(arguments))
                (item as SemanticAstNode).projectMethodCall = call
                resultOf(call)
            }
            is ProjectRecordMemberResolution.Resolved -> elementOf(member?.let { fieldType(it, resolution) } ?: fieldTypeOf(resolution), arguments)
            else -> null
        }
    }

    /** The type of the value [receiver] denotes, or why it has none. */
    fun base(receiver: AstNode): Base = when (receiver.type) {
        PlSqlGrammar.METHOD_CALL -> call(receiver)
        else -> Base.None("receiver form ${receiver.type}", Form.OTHER)
    }

    /**
     * `x(…)`: an element of a collection value, the result of a bound call, or an element of a call's result. Further
     * argument lists (`f(1)(2)`) index the value before them.
     */
    private fun call(node: AstNode): Base {
        val lists = node.getChildren(PlSqlGrammar.ARGUMENTS)
        if (lists.isEmpty()) return Base.None("no argument list", Form.OTHER)
        var base = firstList(node, lists.first())
        for (list in lists.drop(1)) {
            val collection = (base as? Base.Typed)?.declaration ?: return base
            base = elementOf(collection, list)?.let { Base.Typed(it, Form.ELEMENT) } ?: Base.None("element of a non-collection", Form.ELEMENT)
        }
        return base
    }

    private fun firstList(node: AstNode, arguments: AstNode): Base {
        val callee = node.firstChild
        if (callee.type == PlSqlGrammar.METHOD_CALL) {
            val inner = call(callee) as? Base.Typed ?: return Base.None("element of an untyped call", Form.ELEMENT)
            return elementOf(inner.declaration, arguments)?.let { Base.Typed(it, Form.ELEMENT) } ?: Base.None("element of a non-collection", Form.ELEMENT)
        }
        if (callee.type != PlSqlGrammar.VARIABLE_NAME && callee.type != PlSqlGrammar.MEMBER_EXPRESSION) return Base.None("callee form ${callee.type}", Form.OTHER)
        when (val value = collectionValue(callee)) {
            is Base.Typed -> return elementOf(value.declaration, arguments)?.let { Base.Typed(it, Form.ELEMENT) }
                ?: Base.None("element type unresolved or not one index", Form.ELEMENT)
            is Base.None -> if (value.reason != NOT_A_VALUE) return value
        }
        return result(node, callee)
    }

    /** The collection type of the value [callee] names; [NOT_A_VALUE] when it is not proven a value. */
    private fun collectionValue(callee: AstNode): Base {
        val type = when (callee.type) {
            PlSqlGrammar.VARIABLE_NAME -> {
                val facts = context.localSemanticFacts ?: return Base.None("no local facts", Form.ELEMENT)
                val token = callee.token
                val outcome = facts.firstOutcome(token.line, token.column) ?: return Base.None("no local outcome", Form.ELEMENT)
                when {
                    // A package variable of the specification in another file, or an attribute of the enclosing type named alone.
                    LocalReferenceOutcomes.isSpecificationMember(outcome) ->
                        enclosingPackage(callee)?.let { packageValueType(it, name(callee)) } ?: selfBase.of(context, callee, name(callee))
                    else -> {
                        val declaration = facts.nameDeclaration(token.line, token.column) ?: return Base.None(NOT_A_VALUE, Form.ELEMENT)
                        if (declaration.kind != LocalDeclarationKind.VARIABLE && declaration.kind != LocalDeclarationKind.PARAMETER) return Base.None(NOT_A_VALUE, Form.ELEMENT)
                        facts.declaredType(declaration)
                    }
                }
            }
            else -> memberValueType(callee) ?: return Base.None(NOT_A_VALUE, Form.ELEMENT)
        } ?: return Base.None("value type unresolved", Form.ELEMENT)
        return if (isCollection(type)) Base.Typed(type, Form.ELEMENT) else Base.None("value is not a collection", Form.ELEMENT)
    }

    /** A field, an attribute or a package variable named by a qualified [callee]; null when it is not proven one. */
    private fun memberValueType(callee: AstNode): ProjectTypeDeclaration? {
        val children = callee.children
        if (children.size == 3 && children[0].type == PlSqlGrammar.VARIABLE_NAME && children[2].type == PlSqlGrammar.IDENTIFIER_NAME) {
            members.decorate(callee)
            (callee as SemanticAstNode).projectRecordMemberResolution?.let { resolution ->
                return (resolution as? ProjectRecordMemberResolution.Resolved)?.let(::fieldTypeOf)
            }
            // `pk.v`: a package the local side proves absent, whose member of that name is only a package variable.
            val facts = context.localSemanticFacts ?: return null
            val outcome = facts.firstOutcome(callee.token.line, callee.token.column) ?: return null
            if (!LocalReferenceOutcomes.isAbsent(outcome)) return null
            return packageValueType(QualifiedName(listOf(name(children[0]))), name(children[2]))
        }
        val path = paths.pathOf(callee) as? ProjectRecordMemberPathResolution.Completed ?: return null
        return segmentType(path.segments.last())
    }

    /** The type of the package variable [owner].[name] when the project holds only that one declaration of the name. */
    private fun packageValueType(owner: QualifiedName, name: OracleIdentifier): ProjectTypeDeclaration? {
        val value = index.findOccurrences(owner.append(name)).map { it.declaration }.singleOrNull() as? PackageValueDeclaration ?: return null
        return resolve(value.typeRef, value.owner)
    }

    /** The result type of a call bound to one candidate. */
    private fun result(node: AstNode, callee: AstNode): Base {
        (node as SemanticAstNode).projectMethodCall?.let { call ->
            return resultOf(call)?.let { Base.Typed(it, Form.METHOD_RESULT) } ?: Base.None("method call ${call.status}", Form.METHOD_RESULT)
        }
        val analysis = calls[callee.token.line to callee.token.column] ?: return Base.None("no call analysis", Form.CALL_RESULT)
        return when (analysis.outcome) {
            LocalCallOutcome.LOCAL_CAPTURE -> {
                val local = analysis.local ?: return Base.None("local callee not a subprogram set", Form.CALL_RESULT)
                val selected = local.selected ?: return Base.None("local call ${local.status}", Form.CALL_RESULT)
                val facts = context.localSemanticFacts ?: return Base.None("no local facts", Form.CALL_RESULT)
                val returnType = (facts.model.declaration(selected).details as? LocalSubprogram)?.returnType ?: return Base.None("local procedure", Form.CALL_RESULT)
                val datatype = datatypes[returnType.range.startLine to returnType.range.startColumn]
                val resolution = datatype?.projectTypeResolution as? ProjectTypeResolution.Resolved ?: return Base.None("local return type not a project type", Form.CALL_RESULT)
                Base.Typed(resolution.declaration, Form.CALL_RESULT)
            }
            LocalCallOutcome.PROJECT -> {
                val typed = analysis.typed ?: return Base.None("no typed resolution", Form.CALL_RESULT)
                if (typed.status != ProjectTypedCallResolution.Status.RESOLVED) return Base.None("project call ${typed.status}", Form.CALL_RESULT)
                val id = typed.selected.get().declaration.id
                projectResult(index.occurrencesFor(FileId(id.fileId)).getOrNull(id.ordinal)?.declaration)
            }
            else -> Base.None("call ${analysis.outcome}", Form.CALL_RESULT)
        }
    }

    /** The return type of a project function a call selected. */
    private fun projectResult(declaration: ProjectDeclaration?): Base {
        val (returnType, owner) = when (declaration) {
            is PackageFunctionDeclaration -> declaration.returnType to declaration.owner
            is StandaloneFunctionDeclaration -> declaration.returnType to null
            else -> return Base.None("project callee not a function", Form.CALL_RESULT)
        }
        return resolve(returnType, owner)?.let { Base.Typed(it, Form.CALL_RESULT) } ?: Base.None("project return type unresolved", Form.CALL_RESULT)
    }

    /** The result type of a method call bound to one candidate: its return type, or the type a constructor builds. */
    private fun resultOf(call: ProjectMethodCall): ProjectTypeDeclaration? {
        if (call.status != ProjectMethodCall.Status.RESOLVED) return null
        val selected = call.selected ?: return null
        val method = selected.method ?: return selected.type
        if (method.returnsSelf) return selected.type
        return method.returnType?.let { resolve(it, null) }
    }

    /** The element type of [collection], indexed by one positional actual. */
    private fun elementOf(collection: ProjectTypeDeclaration?, arguments: AstNode): ProjectTypeDeclaration? {
        val argument = arguments.getChildren(PlSqlGrammar.ARGUMENT).singleOrNull() ?: return null
        if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) return null
        return when (collection) {
            is PackageTypeDeclaration -> collection.elementType?.takeIf { collection.shape == ProjectTypeShape.COLLECTION }?.let { resolve(it, collection.owner) }
            is StandaloneTypeDeclaration -> collection.elementType?.takeIf { collection.shape == ProjectTypeShape.COLLECTION }?.let { resolve(it, null) }
            else -> null
        }
    }

    private fun fieldType(item: AstNode, resolution: ProjectRecordMemberResolution.Resolved): ProjectTypeDeclaration? {
        val fieldType = fieldTypeResolver.resolve(resolution)
        (item as SemanticAstNode).projectRecordFieldTypeResolution = fieldType
        return declarationOf(fieldType.resolution)
    }

    private fun fieldTypeOf(resolution: ProjectRecordMemberResolution.Resolved): ProjectTypeDeclaration? =
        declarationOf(fieldTypeResolver.resolve(resolution).resolution)

    private fun segmentType(segment: ProjectRecordMemberPathSegment): ProjectTypeDeclaration? = declarationOf(segment.fieldTypeResolution.resolution)

    private fun resolve(reference: TypeRef, owner: QualifiedName?): ProjectTypeDeclaration? =
        declarationOf(typeResolver.resolve(reference, ProjectTypeLookupContext(owner)))

    private fun declarationOf(resolution: TypeRefSemanticResolution): ProjectTypeDeclaration? =
        ((resolution as? TypeRefSemanticResolution.Project)?.resolution as? ProjectTypeResolution.Resolved)?.declaration

    private fun isCollection(type: ProjectTypeDeclaration) =
        (type as? PackageTypeDeclaration)?.shape == ProjectTypeShape.COLLECTION || (type as? StandaloneTypeDeclaration)?.shape == ProjectTypeShape.COLLECTION

    /** The names of a `name.name…` member expression, or null for another shape. */
    private fun memberNames(node: AstNode): List<OracleIdentifier>? {
        val children = node.children
        if (children.size < 3 || children.size % 2 == 0 || children.first().type != PlSqlGrammar.VARIABLE_NAME) return null
        for (index in 1 until children.size) {
            if (children[index].type != (if (index % 2 == 1) PlSqlPunctuator.DOT else PlSqlGrammar.IDENTIFIER_NAME)) return null
        }
        return (0 until children.size step 2).map { name(children[it]) }
    }

    private fun kind(reference: AstNode) = if (reference.parent.type == PlSqlGrammar.CALL_STATEMENT) ProjectCallKind.PROCEDURE else ProjectCallKind.FUNCTION

    private fun name(node: AstNode) = OracleIdentifier.fromSource(node.tokenOriginalValue)

    private fun enclosingPackage(node: AstNode): QualifiedName? =
        node.getFirstAncestorOrNull(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)
            ?.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
            ?.let { unit -> QualifiedName(unit.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).map { OracleIdentifier.fromSource(it.tokenOriginalValue) }) }

    companion object {
        private const val NOT_A_VALUE = "not proven a value"
        private val SQL_STATEMENTS = setOf(
            DmlGrammar.SELECT_EXPRESSION, DmlGrammar.INSERT_EXPRESSION, DmlGrammar.UPDATE_EXPRESSION,
            DmlGrammar.DELETE_EXPRESSION, DmlGrammar.MERGE_EXPRESSION
        )

        /** Inside a SQL statement, outside an `INTO` clause (whose targets are PL/SQL). */
        private fun inSql(node: AstNode): Boolean {
            var current: AstNode? = node.parentOrNull
            while (current != null) {
                if (current.type == DmlGrammar.INTO_CLAUSE) return false
                if (current.type in SQL_STATEMENTS) return true
                current = current.parentOrNull
            }
            return false
        }

        /** An item after the first child of an `OBJECT_REFERENCE`, or the callee of one: a member of an expression receiver. */
        fun isReceiverItem(node: AstNode): Boolean {
            var current = node
            while (current.parentOrNull?.type == PlSqlGrammar.METHOD_CALL && current.parent.firstChild === current) current = current.parent
            val parent = current.parentOrNull ?: return false
            return parent.type == PlSqlGrammar.OBJECT_REFERENCE && parent.firstChild !== current
        }
    }
}

/** The structural arguments of an `ARGUMENTS` node: positional, or named by `=>`. */
internal fun callArguments(arguments: AstNode): ProjectCallArguments =
    ProjectCallArguments.of(arguments.getChildren(PlSqlGrammar.ARGUMENT).map { argument ->
        if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) ProjectCallArgument.named(OracleIdentifier.fromSource(argument.firstChild.tokenOriginalValue))
        else ProjectCallArgument.positional()
    })
