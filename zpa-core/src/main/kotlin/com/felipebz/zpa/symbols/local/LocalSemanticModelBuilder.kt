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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.project.extraction.TokenStream
import com.felipebz.zpa.project.extraction.TypeReferences
import java.util.*

/** Source facts shared by the local extraction code: identifiers and ranges of AST nodes, never the nodes. */
internal object LocalSyntax {
    fun identifier(node: AstNode): OracleIdentifier = OracleIdentifier.fromSource(node.token.originalValue)

    fun range(fileId: FileId, node: AstNode): SourceRange =
        SourceRange(fileId, node.token.line, node.token.column, node.lastToken.endLine, node.lastToken.endColumn)
}

/**
 * Builds a [LocalSemanticModel] from an AST the parser already produced, in one traversal and without reparsing.
 * All construction state belongs to one call; the result keeps no AST node, token or source text.
 *
 * In the grammar, `BLOCK_STATEMENT` occurs only as the body of `ANONYMOUS_BLOCK` (a top-level block) and as a
 * `STATEMENT` alternative (a nested block); both form a region. Subprograms form a region only when they have a body.
 * Packages, package bodies, triggers (and compound-trigger timing points), object types and type bodies form regions
 * holding their declarations. `ALTER TYPE` is not traversed: its member specs are changes to an existing type, not
 * declarations of the file, and it holds no PL/SQL body.
 */
internal object LocalSemanticModelBuilder {
    private val SUBPROGRAMS = setOf(
        PlSqlGrammar.CREATE_PROCEDURE,
        PlSqlGrammar.CREATE_FUNCTION,
        PlSqlGrammar.PROCEDURE_DECLARATION,
        PlSqlGrammar.FUNCTION_DECLARATION
    )

    private val ITERATOR_CHOICES = setOf(
        PlSqlGrammar.BASIC_ITERATOR_CHOICE,
        PlSqlGrammar.INDEX_ITERATOR_CHOICE,
        PlSqlGrammar.SEQUENCE_ITERATOR_CHOICE
    )

    private val TRIGGER_ALTERNATIVES = setOf(
        PlSqlGrammar.SIMPLE_DML_TRIGGER,
        PlSqlGrammar.INSTEAD_OF_DML_TRIGGER,
        PlSqlGrammar.COMPOUND_DML_TRIGGER,
        PlSqlGrammar.SYSTEM_TRIGGER
    )

    /** Regions whose forward subprogram declarations are linked to their bodies. */
    private val FORWARD_SCOPES = setOf(
        LocalRegionKind.ANONYMOUS_BLOCK, LocalRegionKind.SUBPROGRAM, LocalRegionKind.PACKAGE_BODY,
        LocalRegionKind.TRIGGER, LocalRegionKind.TIMING_POINT
    )

    private val DEFAULT_CORRELATION_NAMES = LocalCorrelationNames(
        OracleIdentifier.fromSource("NEW"), OracleIdentifier.fromSource("OLD"), OracleIdentifier.fromSource("PARENT")
    )

    private val SELF = OracleIdentifier.fromSource("SELF")

    fun build(root: AstNode, fileId: FileId): LocalSemanticModel = Construction(fileId).build(root)

    private class RegionDraft(
        val id: LocalRegionId,
        val parent: RegionDraft?,
        val kind: LocalRegionKind,
        val syntax: String,
        val range: SourceRange
    ) {
        val declarations = ArrayList<LocalDeclarationId>()
        val children = ArrayList<LocalRegionId>()
        val gaps = ArrayList<LocalCoverageGap>()
    }

    private class DeclarationDraft(
        val id: LocalDeclarationId,
        val region: RegionDraft,
        val position: Int,
        val kind: LocalDeclarationKind,
        val name: OracleIdentifier,
        val range: SourceRange,
        val nameRange: SourceRange,
        val type: LocalTypeUse?,
        val isConstant: Boolean,
        val isNotNull: Boolean,
        val hasDefault: Boolean,
        /** Completed while the traversal goes on (body region, labeled region) and when forward links are made. */
        var details: LocalDeclarationDetails?
    )

    private class Construction(private val fileId: FileId) {
        private val regions = ArrayList<RegionDraft>()
        private val declarations = ArrayList<DeclarationDraft>()
        private var recovered = false

        fun build(root: AstNode): LocalSemanticModel {
            val file = open(null, LocalRegionKind.FILE, "FILE", fileRange(root))
            walk(root, file)
            linkForwardDeclarations()
            linkUnits()
            return freeze()
        }

        private fun open(parent: RegionDraft?, kind: LocalRegionKind, syntax: String, range: SourceRange): RegionDraft {
            val region = RegionDraft(LocalRegionId(regions.size), parent, kind, syntax, range)
            regions += region
            parent?.children?.add(region.id)
            return region
        }

        private fun declare(
            region: RegionDraft,
            kind: LocalDeclarationKind,
            name: OracleIdentifier,
            range: SourceRange,
            nameRange: SourceRange,
            type: LocalTypeUse? = null,
            isConstant: Boolean = false,
            isNotNull: Boolean = false,
            hasDefault: Boolean = false,
            details: LocalDeclarationDetails? = null
        ): DeclarationDraft {
            val draft = DeclarationDraft(
                LocalDeclarationId(declarations.size), region, region.declarations.size, kind, name, range, nameRange,
                type, isConstant, isNotNull, hasDefault, details
            )
            declarations += draft
            region.declarations += draft.id
            return draft
        }

        private fun declare(region: RegionDraft, kind: LocalDeclarationKind, item: AstNode, nameNode: AstNode, type: LocalTypeUse? = null,
                            isConstant: Boolean = false, isNotNull: Boolean = false, hasDefault: Boolean = false,
                            details: LocalDeclarationDetails? = null) =
            declare(region, kind, LocalSyntax.identifier(nameNode), range(item), range(nameNode), type, isConstant, isNotNull, hasDefault, details)

        private fun walk(node: AstNode, region: RegionDraft) {
            for (child in node.children) visit(child, region)
        }

        private fun visit(node: AstNode, region: RegionDraft) {
            when (node.type) {
                PlSqlGrammar.RECOVERY -> {
                    recovered = true
                    region.gaps += gap(LocalGapKind.PARSE_RECOVERY, node)
                }
                PlSqlGrammar.BLOCK_STATEMENT -> regionWithBody(node, region, LocalRegionKind.ANONYMOUS_BLOCK)
                PlSqlGrammar.FOR_STATEMENT -> regionWithBody(node, region, LocalRegionKind.FOR_LOOP)
                PlSqlGrammar.FORALL_STATEMENT -> forall(node, region)
                in ITERATOR_CHOICES -> regionWithBody(node, region, LocalRegionKind.ITERATOR)
                in SUBPROGRAMS, PlSqlGrammar.TYPE_CONSTRUCTOR -> subprogram(node, region)
                PlSqlGrammar.CREATE_PACKAGE -> unit(node, region, LocalDeclarationKind.PACKAGE, LocalRegionKind.PACKAGE_SPEC)
                PlSqlGrammar.CREATE_PACKAGE_BODY -> unit(node, region, LocalDeclarationKind.PACKAGE_BODY, LocalRegionKind.PACKAGE_BODY)
                PlSqlGrammar.CREATE_TYPE_BODY -> unit(node, region, LocalDeclarationKind.TYPE_BODY, LocalRegionKind.TYPE_BODY)
                PlSqlGrammar.CREATE_TYPE -> createType(node, region)
                PlSqlGrammar.CREATE_TRIGGER -> trigger(node, region)
                PlSqlGrammar.TYPE_ATTRIBUTE -> declare(
                    region, LocalDeclarationKind.ATTRIBUTE, node, node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME),
                    node.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, region) }, isNotNull = notNull(node)
                )
                PlSqlGrammar.ALTER_TYPE -> Unit
                PlSqlGrammar.ITERATOR -> iterator(node, region)
                // A label of a statement that forms no region: a declaration of the region holding the statement.
                PlSqlGrammar.LABEL -> declareLabel(node, region, null)
                PlSqlGrammar.DECLARE_SECTION -> node.children.forEach { declarationItem(it, region) }
                else -> {
                    val with = node.getFirstChildOrNull(DmlGrammar.WITH_CLAUSE)
                    if (with != null && with.children.any { it.type in SUBPROGRAMS }) sqlWith(node, with, region) else walk(node, region)
                }
            }
        }

        /**
         * A query whose `WITH` clause declares PL/SQL subprograms: they are declarations of a SQL_WITH region spanning
         * the query from `WITH`, so the query blocks that call them are inside it. Content before the clause stays in
         * [enclosing].
         */
        private fun sqlWith(query: AstNode, with: AstNode, enclosing: RegionDraft) {
            val withIndex = query.children.indexOf(with)
            query.children.take(withIndex).forEach { visit(it, enclosing) }
            val range = SourceRange(fileId, with.token.line, with.token.column, query.lastToken.endLine, query.lastToken.endColumn)
            val region = open(enclosing, LocalRegionKind.SQL_WITH, with.type.toString(), range)
            query.children.drop(withIndex).forEach { visit(it, region) }
        }

        /**
         * Opens the region formed by [node] and visits its content in it. The node's labels are declarations of the
         * enclosing region, linked to the new region; the region starts after them.
         */
        private fun regionWithBody(node: AstNode, enclosing: RegionDraft, kind: LocalRegionKind): RegionDraft? {
            val labels = node.children.filter { it.type === PlSqlGrammar.LABEL }.map { declareLabel(it, enclosing, null) }
            val content = node.children.filter { it.type !== PlSqlGrammar.LABEL }
            val first = content.firstOrNull() ?: return null
            val range = SourceRange(fileId, first.token.line, first.token.column, node.lastToken.endLine, node.lastToken.endColumn)
            val region = open(enclosing, kind, node.type.toString(), range)
            labels.forEach { it.details = LocalLabel(region.id) }
            content.forEach { visit(it, region) }
            return region
        }

        private fun declareLabel(label: AstNode, region: RegionDraft, labeled: LocalRegionId?): DeclarationDraft =
            declare(region, LocalDeclarationKind.LABEL, label, label.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME), details = LocalLabel(labeled))

        /** The iterands of a FOR loop or an iterator choice, as indices of the region the loop forms. */
        private fun iterator(node: AstNode, region: RegionDraft) {
            for (child in node.children) {
                if (child.type !== PlSqlGrammar.ITERAND_DECLARATION) {
                    visit(child, region)
                    continue
                }
                val datatype = child.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
                val mutability = when {
                    child.hasDirectChildren(PlSqlKeyword.MUTABLE) -> LocalMutability.MUTABLE
                    child.hasDirectChildren(PlSqlKeyword.IMMUTABLE) -> LocalMutability.IMMUTABLE
                    else -> null
                }
                declare(
                    region, LocalDeclarationKind.LOOP_INDEX, child, child.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME),
                    // Looked up where the loop is written, not among the loop's own indices.
                    type = datatype?.let { typeUse(it, region.parent ?: region) },
                    isNotNull = notNull(child),
                    details = LocalLoopIndex(mutability, datatype == null)
                )
            }
        }

        private fun forall(node: AstNode, enclosing: RegionDraft) {
            val labels = node.children.filter { it.type === PlSqlGrammar.LABEL }.map { declareLabel(it, enclosing, null) }
            val content = node.children.filter { it.type !== PlSqlGrammar.LABEL }
            val first = content.first()
            val region = open(enclosing, LocalRegionKind.FORALL, node.type.toString(),
                SourceRange(fileId, first.token.line, first.token.column, node.lastToken.endLine, node.lastToken.endColumn))
            labels.forEach { it.details = LocalLabel(region.id) }
            val index = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            declare(region, LocalDeclarationKind.LOOP_INDEX, index, index, details = LocalLoopIndex(null, true))
            content.filter { it !== index }.forEach { visit(it, region) }
        }

        /**
         * A procedure, function or object type constructor, declared in [enclosing]; with a body it forms a SUBPROGRAM
         * region holding its parameters and local declarations. Parameter types are looked up in [enclosing]; a RETURN
         * type in the subprogram's own region when it has one.
         */
        private fun subprogram(node: AstNode, enclosing: RegionDraft) {
            val standalone = node.type === PlSqlGrammar.CREATE_PROCEDURE || node.type === PlSqlGrammar.CREATE_FUNCTION
            val constructor = node.type === PlSqlGrammar.TYPE_CONSTRUCTOR
            val nameNode = if (standalone) node.getFirstChild(PlSqlGrammar.UNIT_NAME) else node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            val unitName = if (standalone) unitName(nameNode) else QualifiedName(LocalSyntax.identifier(nameNode))
            val form = when {
                node.hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION) -> LocalSubprogramForm.BODY
                node.hasDirectChildren(PlSqlGrammar.CALL_SPECIFICATION) -> LocalSubprogramForm.CALL_SPECIFICATION
                node.hasDirectChildren(PlSqlTokenType.WRAPPED_SOURCE) -> LocalSubprogramForm.WRAPPED
                else -> LocalSubprogramForm.HEADING_ONLY
            }
            val kind = if (constructor || node.type === PlSqlGrammar.CREATE_FUNCTION || node.type === PlSqlGrammar.FUNCTION_DECLARATION) {
                LocalDeclarationKind.FUNCTION
            } else {
                LocalDeclarationKind.PROCEDURE
            }
            val declaration = declare(enclosing, kind, unitName.last, range(node), range(nameNode))
            val body = if (form == LocalSubprogramForm.BODY) open(enclosing, LocalRegionKind.SUBPROGRAM, node.type.toString(), range(node)) else null

            val parameters = ArrayList<LocalParameter>()
            if (constructor) explicitConstructorSelf(node, enclosing, body)?.let { parameters += it }
            val parameterNodes = if (constructor) {
                node.getChildren(PlSqlGrammar.PARAMETER_DECLARATION)
            } else {
                node.getFirstChildOrNull(PlSqlGrammar.PARAMETER_DECLARATIONS)?.getChildren(PlSqlGrammar.PARAMETER_DECLARATION).orEmpty()
            }
            for (parameter in parameterNodes) parameters += parameter(parameter, parameters.size + 1, enclosing, body)
            val returnType = if (constructor) null else node.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, body ?: enclosing) }
            for (child in node.children) {
                when (child.type) {
                    PlSqlGrammar.UNIT_NAME, PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.PARAMETER_DECLARATIONS,
                    PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.DATATYPE -> Unit
                    else -> visit(child, body ?: enclosing)
                }
            }
            val methodKind = methodKind(node)
            val explicitSelf = parameters.any { it.name == SELF }
            val implicitSelf = when {
                methodKind == null || methodKind == LocalMethodKind.STATIC || explicitSelf -> null
                methodKind == LocalMethodKind.CONSTRUCTOR || kind == LocalDeclarationKind.PROCEDURE -> ParameterMode.IN_OUT
                else -> ParameterMode.IN
            }
            declaration.details = LocalSubprogram(
                unitName, form, frozenList(parameters), returnType, constructor, body?.id, null, null, methodKind, implicitSelf
            )
        }

        /** The method kind written around an object type member: its wrapper rule and keywords. */
        private fun methodKind(node: AstNode): LocalMethodKind? {
            if (node.type === PlSqlGrammar.TYPE_CONSTRUCTOR) return LocalMethodKind.CONSTRUCTOR
            val wrapper = node.parent ?: return null
            return when (wrapper.type) {
                PlSqlGrammar.MAP_ORDER_FUNCTION -> if (wrapper.hasDirectChildren(PlSqlKeyword.MAP)) LocalMethodKind.MAP else LocalMethodKind.ORDER
                PlSqlGrammar.TYPE_SUBPROGRAM -> if (wrapper.hasDirectChildren(PlSqlKeyword.STATIC)) LocalMethodKind.STATIC else LocalMethodKind.MEMBER
                else -> null
            }
        }

        /** `CONSTRUCTOR FUNCTION t(SELF IN OUT t, …)`: the grammar writes this `SELF` outside PARAMETER_DECLARATION. */
        private fun explicitConstructorSelf(node: AstNode, enclosing: RegionDraft, body: RegionDraft?): LocalParameter? {
            val children = node.children
            val open = children.indexOfFirst { it.type === PlSqlPunctuator.LPARENTHESIS }
            val self = children.getOrNull(open + 1)?.takeIf { open >= 0 && it.type === PlSqlKeyword.SELF } ?: return null
            val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)
            val parameter = LocalParameter(
                LocalSyntax.identifier(self), range(self), 1, ParameterMode.IN_OUT, false, false, typeUse(datatype, enclosing)
            )
            if (body != null) {
                val range = SourceRange(fileId, self.token.line, self.token.column, datatype.lastToken.endLine, datatype.lastToken.endColumn)
                declare(body, LocalDeclarationKind.PARAMETER, parameter.name, range, parameter.nameRange, parameter.type, details = parameter)
            }
            return parameter
        }

        /**
         * A package, package body or type body: a declaration of [enclosing] and, unless wrapped, a region of
         * [regionKind] holding its declaration-section items (or type body members) and its initialization section.
         */
        private fun unit(node: AstNode, enclosing: RegionDraft, kind: LocalDeclarationKind, regionKind: LocalRegionKind) {
            val nameNode = node.getFirstChild(PlSqlGrammar.UNIT_NAME)
            val wrapped = node.hasDirectChildren(PlSqlTokenType.WRAPPED_SOURCE)
            val declaration = declare(enclosing, kind, nameNode.lastChild.let(LocalSyntax::identifier), range(node), range(nameNode))
            val region = if (wrapped) null else open(enclosing, regionKind, node.type.toString(), range(node))
            if (region != null) node.children.filter { it !== nameNode }.forEach { visit(it, region) }
            declaration.details = LocalUnit(unitName(nameNode), if (wrapped) LocalUnitForm.WRAPPED else LocalUnitForm.SOURCE, region?.id, null, null)
        }

        /** `CREATE TYPE`: an object type forms an OBJECT_TYPE region; a collection type forms none. */
        private fun createType(node: AstNode, enclosing: RegionDraft) {
            val nameNode = node.getFirstChild(PlSqlGrammar.UNIT_NAME)
            val name = unitName(nameNode)
            val objectDefinition = node.getFirstChildOrNull(PlSqlGrammar.OBJECT_TYPE_DEFINITION)
            val collection = node.hasDirectChildren(PlSqlKeyword.TABLE, PlSqlKeyword.VARRAY, PlSqlKeyword.ARRAY) ||
                node.hasDirectChildren(PlSqlGrammar.NESTED_TABLE_DEFINITION, PlSqlGrammar.VARRAY_TYPE_DEFINITION)
            if (collection) {
                val definition = node.getFirstChildOrNull(PlSqlGrammar.NESTED_TABLE_DEFINITION)
                    ?: node.getFirstChildOrNull(PlSqlGrammar.VARRAY_TYPE_DEFINITION) ?: node
                val form = if (node.hasDirectChildren(PlSqlKeyword.VARRAY, PlSqlKeyword.ARRAY) ||
                    definition.type === PlSqlGrammar.VARRAY_TYPE_DEFINITION) LocalCollectionForm.VARRAY else LocalCollectionForm.NESTED_TABLE
                declare(
                    enclosing, LocalDeclarationKind.COLLECTION_TYPE, name.last, range(node), range(nameNode),
                    definition.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, enclosing) },
                    isNotNull = notNull(definition), details = LocalCollectionType(form, null, name)
                )
                return
            }
            val form = when {
                node.hasDirectChildren(PlSqlTokenType.WRAPPED_SOURCE) -> LocalUnitForm.WRAPPED
                objectDefinition == null -> LocalUnitForm.INCOMPLETE
                else -> LocalUnitForm.SOURCE
            }
            val declaration = declare(enclosing, LocalDeclarationKind.OBJECT_TYPE, name.last, range(node), range(nameNode))
            val region = objectDefinition?.let { open(enclosing, LocalRegionKind.OBJECT_TYPE, node.type.toString(), range(node)) }
            val supertype = objectDefinition?.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME)
            objectDefinition?.children?.filter { it !== supertype }?.forEach { visit(it, region!!) }
            declaration.details = LocalUnit(name, form, region?.id, null, supertype?.let(::unitName))
        }

        /**
         * A trigger: a declaration of [enclosing] and, with a PL/SQL body, a TRIGGER region starting at the body. The
         * heading (events, REFERENCING, WHEN) stays in [enclosing]. A compound trigger's timing points are nested
         * TIMING_POINT regions.
         */
        private fun trigger(node: AstNode, enclosing: RegionDraft) {
            val nameNode = node.getFirstChild(PlSqlGrammar.UNIT_NAME)
            val declaration = declare(enclosing, LocalDeclarationKind.TRIGGER, nameNode.lastChild.let(LocalSyntax::identifier), range(node), range(nameNode))
            val alternative = node.children.firstOrNull { it.type in TRIGGER_ALTERNATIVES }
            var body: RegionDraft? = null
            if (alternative != null) {
                val children = alternative.children
                val bodyStart = children.indexOfFirst {
                    it.type === PlSqlKeyword.DECLARE || it.type === PlSqlGrammar.STATEMENTS_SECTION || it.type === PlSqlGrammar.COMPOUND_TRIGGER_BLOCK
                }
                children.take(if (bodyStart < 0) children.size else bodyStart).forEach { visit(it, enclosing) }
                if (bodyStart >= 0) {
                    val first = children[bodyStart]
                    val region = open(enclosing, LocalRegionKind.TRIGGER, (if (first.type === PlSqlGrammar.COMPOUND_TRIGGER_BLOCK) first.type else node.type).toString(),
                        SourceRange(fileId, first.token.line, first.token.column, alternative.lastToken.endLine, alternative.lastToken.endColumn))
                    body = region
                    for (child in children.drop(bodyStart)) {
                        if (child.type === PlSqlGrammar.COMPOUND_TRIGGER_BLOCK) compoundTrigger(child, region) else visit(child, region)
                    }
                }
            }
            val correlation = when (alternative?.type) {
                null, PlSqlGrammar.SYSTEM_TRIGGER -> null
                else -> correlationNames(alternative.getFirstChildOrNull(PlSqlGrammar.REFERENCING_CLAUSE))
            }
            declaration.details = LocalTrigger(unitName(nameNode), body?.id, correlation)
        }

        private fun compoundTrigger(block: AstNode, region: RegionDraft) {
            for (child in block.children) {
                if (child.type === PlSqlGrammar.TIMING_POINT_SECTION) {
                    val timingPoint = open(region, LocalRegionKind.TIMING_POINT, child.type.toString(), range(child))
                    walk(child, timingPoint)
                } else {
                    visit(child, region)
                }
            }
        }

        private fun correlationNames(referencing: AstNode?): LocalCorrelationNames {
            if (referencing == null) return DEFAULT_CORRELATION_NAMES
            var new = DEFAULT_CORRELATION_NAMES.new
            var old = DEFAULT_CORRELATION_NAMES.old
            var parent = DEFAULT_CORRELATION_NAMES.parent
            var pending: AstNodeType? = null
            for (child in referencing.children) {
                when {
                    child.type === PlSqlKeyword.NEW || child.type === PlSqlKeyword.OLD || child.type === PlSqlKeyword.PARENT -> pending = child.type
                    child.type === PlSqlGrammar.IDENTIFIER_NAME -> {
                        val alias = LocalSyntax.identifier(child)
                        when (pending) {
                            PlSqlKeyword.NEW -> new = alias
                            PlSqlKeyword.OLD -> old = alias
                            PlSqlKeyword.PARENT -> parent = alias
                            else -> Unit
                        }
                        pending = null
                    }
                }
            }
            return LocalCorrelationNames(new, old, parent)
        }

        private fun unitName(unitNameNode: AstNode) =
            QualifiedName(unitNameNode.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map(LocalSyntax::identifier))

        private fun parameter(node: AstNode, ordinal: Int, enclosing: RegionDraft, body: RegionDraft?): LocalParameter {
            val out = node.hasDirectChildren(PlSqlKeyword.OUT)
            val mode = when {
                out && node.hasDirectChildren(PlSqlKeyword.IN) -> ParameterMode.IN_OUT
                out -> ParameterMode.OUT
                else -> ParameterMode.IN
            }
            val name = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            val default = node.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
            val parameter = LocalParameter(
                LocalSyntax.identifier(name), range(name), ordinal, mode, node.hasDirectChildren(PlSqlKeyword.NOCOPY),
                default != null, node.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, enclosing) }
            )
            if (body != null) {
                declare(body, LocalDeclarationKind.PARAMETER, node, name, type = parameter.type, hasDefault = default != null, details = parameter)
            }
            default?.let { visit(it, body ?: enclosing) }
            return parameter
        }

        private fun declarationItem(item: AstNode, region: RegionDraft) {
            when (item.type) {
                PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION -> return subprogram(item, region)
                PlSqlGrammar.CURSOR_DECLARATION -> return cursor(item, region)
                // Pragmas declare no name.
                PlSqlGrammar.PRAGMA_DECLARATION -> return walk(item, region)
                else -> Unit
            }
            val name = item.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
            val datatype = item.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
            val hasDefault = item.hasDirectChildren(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
            val declared = name != null && when (item.type) {
                PlSqlGrammar.VARIABLE_DECLARATION -> if (datatype == null) {
                    item.hasDirectChildren(PlSqlKeyword.EXCEPTION).also { if (it) declare(region, LocalDeclarationKind.EXCEPTION, item, name) }
                } else {
                    declare(
                        region, LocalDeclarationKind.VARIABLE, item, name, typeUse(datatype, region),
                        isConstant = item.hasDirectChildren(PlSqlKeyword.CONSTANT), isNotNull = notNull(item), hasDefault = hasDefault
                    )
                    true
                }
                PlSqlGrammar.CUSTOM_SUBTYPE -> datatype != null &&
                    declare(region, LocalDeclarationKind.SUBTYPE, item, name, typeUse(datatype, region), isNotNull = notNull(item)).let { true }
                PlSqlGrammar.EXCEPTION_DECLARATION -> declare(region, LocalDeclarationKind.EXCEPTION, item, name).let { true }
                PlSqlGrammar.RECORD_DECLARATION -> {
                    val fields = item.getChildren(PlSqlGrammar.RECORD_FIELD_DECLARATION).map { field ->
                        val fieldName = field.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
                        LocalRecordField(
                            LocalSyntax.identifier(fieldName), range(fieldName),
                            field.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, region) },
                            notNull(field), field.hasDirectChildren(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
                        )
                    }
                    declare(region, LocalDeclarationKind.RECORD_TYPE, item, name, details = LocalRecordType(frozenList(fields)))
                    true
                }
                PlSqlGrammar.TABLE_OF_DECLARATION, PlSqlGrammar.VARRAY_DECLARATION -> {
                    val definition = item.getFirstChildOrNull(PlSqlGrammar.NESTED_TABLE_DEFINITION)
                        ?: item.getFirstChildOrNull(PlSqlGrammar.VARRAY_TYPE_DEFINITION)
                    val element = definition?.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
                    val form = when {
                        item.type === PlSqlGrammar.VARRAY_DECLARATION -> LocalCollectionForm.VARRAY
                        datatype != null -> LocalCollectionForm.ASSOCIATIVE_ARRAY
                        else -> LocalCollectionForm.NESTED_TABLE
                    }
                    element != null && declare(
                        region, LocalDeclarationKind.COLLECTION_TYPE, item, name, typeUse(element, region),
                        isNotNull = notNull(definition), details = LocalCollectionType(form, datatype?.let { typeUse(it, region) })
                    ).let { true }
                }
                PlSqlGrammar.REF_CURSOR_DECLARATION -> declare(
                    region, LocalDeclarationKind.REF_CURSOR_TYPE, item, name,
                    details = LocalRefCursorType(datatype?.let { typeUse(it, region) })
                ).let { true }
                else -> false
            }
            // Never silently dropped: the region records that it holds a declaration it did not extract.
            if (!declared) region.gaps += gap(LocalGapKind.UNSUPPORTED_DECLARATION, item)
            // Default values and bounds may hold regions (qualified-expression iterators).
            walk(item, region)
        }

        /** A cursor; with parameters it forms a CURSOR region in which they and the query are visible. */
        private fun cursor(item: AstNode, region: RegionDraft) {
            val declaration = declare(region, LocalDeclarationKind.CURSOR, item, item.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME))
            val parameterNodes = item.getFirstChildOrNull(PlSqlGrammar.CURSOR_PARAMETER_DECLARATIONS)
                ?.getChildren(PlSqlGrammar.CURSOR_PARAMETER_DECLARATION).orEmpty()
            val parameters = if (parameterNodes.isEmpty()) null else open(region, LocalRegionKind.CURSOR, item.type.toString(), range(item))
            parameterNodes.forEachIndexed { index, parameter ->
                val default = parameter.getFirstChildOrNull(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT)
                declare(
                    parameters!!, LocalDeclarationKind.CURSOR_PARAMETER, parameter, parameter.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME),
                    parameter.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, region) },
                    hasDefault = default != null, details = LocalCursorParameter(index + 1)
                )
                default?.let { visit(it, parameters) }
            }
            val query = item.getFirstChildOrNull(DmlGrammar.SELECT_EXPRESSION)
            declaration.details = LocalCursor(parameters?.id, query == null, item.getFirstChildOrNull(PlSqlGrammar.DATATYPE)?.let { typeUse(it, region) })
            query?.let { visit(it, parameters ?: region) }
        }

        private fun typeUse(datatype: AstNode, lookupRegion: RegionDraft): LocalTypeUse {
            val tokens = datatype.tokens
            return LocalTypeUse(TypeReferences(TokenStream(fileId, tokens)).declaredType(0, tokens.size), range(datatype), lookupRegion.id)
        }

        private fun notNull(node: AstNode?) =
            node?.getFirstChildOrNull(PlSqlGrammar.DATATYPE_NULL_CONSTRAINT)?.hasDirectChildren(PlSqlKeyword.NOT) == true

        /**
         * Links a forward declaration and a body of the same region both ways when exactly one forward declaration
         * and exactly one body have the same kind, name, parameter names, modes and written parameter and return
         * types. Anything else (no match, several, an unrepresentable type) stays unlinked; order never decides.
         * One pass over each region's own declarations, grouped by a signature key.
         */
        private fun linkForwardDeclarations() {
            for (region in regions) {
                if (region.kind !in FORWARD_SCOPES) continue
                val bodies = subprogramsByKey(region, LocalSubprogramForm.BODY)
                for ((key, matches) in subprogramsByKey(region, LocalSubprogramForm.HEADING_ONLY)) {
                    val forward = matches.singleOrNull() ?: continue
                    val body = bodies[key]?.singleOrNull() ?: continue
                    forward.details = (forward.details as LocalSubprogram).copy(counterpart = body.id)
                    body.details = (body.details as LocalSubprogram).copy(counterpart = forward.id)
                }
            }
        }

        /**
         * Links specifications and bodies of packages and object types, then each specification heading to its
         * implementation in the linked body. A unit pair is linked only when the file holds exactly one specification
         * and exactly one body with the same written unit name; headings use the forward-link signature key (which
         * includes the method kind). Grouping by key in hash maps: linear, and order never decides.
         */
        private fun linkUnits() {
            val specifications = HashMap<String, MutableList<DeclarationDraft>>()
            val bodies = HashMap<String, MutableList<DeclarationDraft>>()
            for (draft in declarations) {
                val target = when (draft.kind) {
                    LocalDeclarationKind.PACKAGE, LocalDeclarationKind.OBJECT_TYPE -> specifications
                    LocalDeclarationKind.PACKAGE_BODY, LocalDeclarationKind.TYPE_BODY -> bodies
                    else -> continue
                }
                val unit = draft.details as LocalUnit
                val family = if (draft.kind == LocalDeclarationKind.PACKAGE || draft.kind == LocalDeclarationKind.PACKAGE_BODY) "package" else "type"
                target.getOrPut(family + "|" + unit.unitName.lookupKey()) { ArrayList() } += draft
            }
            for ((key, matches) in specifications) {
                val specification = matches.singleOrNull() ?: continue
                val body = bodies[key]?.singleOrNull() ?: continue
                val specificationUnit = specification.details as LocalUnit
                val bodyUnit = body.details as LocalUnit
                val specificationRegion = specificationUnit.region ?: continue
                val bodyRegion = bodyUnit.region ?: continue
                specification.details = LocalUnit(specificationUnit.unitName, specificationUnit.form, specificationRegion, bodyRegion, specificationUnit.supertype)
                body.details = LocalUnit(bodyUnit.unitName, bodyUnit.form, bodyRegion, specificationRegion, bodyUnit.supertype)
                linkHeadings(regions[specificationRegion.ordinal], regions[bodyRegion.ordinal])
            }
        }

        private fun linkHeadings(specification: RegionDraft, body: RegionDraft) {
            val headings = subprogramsByKey(specification, LocalSubprogramForm.HEADING_ONLY)
            val implementations = subprogramsByKey(body, LocalSubprogramForm.BODY)
            for ((key, matches) in headings) {
                val heading = matches.singleOrNull() ?: continue
                val implementation = implementations[key]?.singleOrNull() ?: continue
                heading.details = (heading.details as LocalSubprogram).copy(specificationCounterpart = implementation.id)
                implementation.details = (implementation.details as LocalSubprogram).copy(specificationCounterpart = heading.id)
            }
        }

        private fun subprogramsByKey(region: RegionDraft, form: LocalSubprogramForm): Map<String, List<DeclarationDraft>> {
            val result = HashMap<String, MutableList<DeclarationDraft>>()
            for (id in region.declarations) {
                val draft = declarations[id.ordinal]
                val subprogram = draft.details as? LocalSubprogram ?: continue
                if (subprogram.form != form) continue
                val key = signatureKey(draft.kind, subprogram) ?: continue
                result.getOrPut(key) { ArrayList() } += draft
            }
            return result
        }

        private fun signatureKey(kind: LocalDeclarationKind, subprogram: LocalSubprogram): String? {
            val key = StringBuilder(kind.name).append('|').append(subprogram.methodKind ?: "").append('|')
                .append(QualifiedName(subprogram.unitName.last).lookupKey())
            for (parameter in subprogram.parameters) {
                val type = parameter.type?.typeRef ?: return null
                key.append('|').append(QualifiedName(parameter.name).lookupKey()).append(' ').append(parameter.mode)
                    .append(' ').append(type.headerIdentityKey())
            }
            if (subprogram.returnsSelf) {
                key.append("|return SELF")
            } else if (kind == LocalDeclarationKind.FUNCTION) {
                key.append("|return ").append((subprogram.returnType?.typeRef ?: return null).headerIdentityKey())
            }
            return key.toString()
        }

        private fun gap(kind: LocalGapKind, node: AstNode) = LocalCoverageGap(kind, node.type.toString(), range(node))

        private fun range(node: AstNode) = LocalSyntax.range(fileId, node)

        private fun fileRange(root: AstNode): SourceRange {
            val content = root.children.filter { it.type !== GenericTokenType.EOF && it.hasToken() }
            if (content.isEmpty()) return SourceRange(fileId, 1, 0, 1, 0)
            val first = content.first().token
            val last = content.last().lastToken
            return SourceRange(fileId, first.line, first.column, last.endLine, last.endColumn)
        }

        private fun freeze(): LocalSemanticModel {
            val frozenRegions = regions.map { draft ->
                val extraction = if (draft.gaps.isNotEmpty()) LocalExtraction.PARTIAL else LocalExtraction.ALL_ITEMS_EXTRACTED
                LocalRegion(
                    draft.id, draft.parent?.id, draft.kind, draft.syntax, draft.range,
                    frozenList(draft.declarations), frozenList(draft.children), extraction, frozenList(draft.gaps)
                )
            }
            val frozenDeclarations = declarations.map {
                LocalDeclaration(
                    it.id, it.region.id, it.position, it.kind, it.name, it.range, it.nameRange, it.type,
                    it.isConstant, it.isNotNull, it.hasDefault, it.details
                )
            }
            val completeness = if (recovered) LocalCompleteness.PARSE_RECOVERY_OBSERVED else LocalCompleteness.NOT_ESTABLISHED
            return LocalSemanticModel(fileId, frozenList(frozenRegions), frozenList(frozenDeclarations), completeness)
        }
    }

    private fun <T> frozenList(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
}
