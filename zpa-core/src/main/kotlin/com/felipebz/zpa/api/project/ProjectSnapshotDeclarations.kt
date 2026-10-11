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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*
import java.util.*
import com.felipebz.zpa.project.ProjectDeclarationKind as FactKind
import com.felipebz.zpa.project.ProjectTypeShape as FactShape

/** Projects public declaration views from canonical index occurrences. */
@OptIn(ZpaExperimentalApi::class)
internal object ProjectSnapshotDeclarations {
    @JvmSynthetic
    fun views(scope: ProjectSnapshotScope, occurrences: List<IndexedDeclaration>): List<ProjectDeclarationView> =
        Collections.unmodifiableList(occurrences.map { view(scope, it) })

    @JvmSynthetic
    fun occurrencesFor(index: ProjectSymbolIndex, fileId: String): List<IndexedDeclaration> =
        // Blank IDs cannot name a file (they are rejected at preparation), so nothing matches.
        if (fileId.isBlank()) emptyList() else index.occurrencesFor(FileId(fileId))

    private fun view(scope: ProjectSnapshotScope, occurrence: IndexedDeclaration): ProjectDeclarationView =
        ProjectDeclarationId.create(scope, occurrence.fileId.value, occurrence.ordinal).let { id ->
            when (val fact = occurrence.declaration) {
                is StandaloneTypeDeclaration -> BackedStandaloneTypeView(id, fact)
                is PackageTypeDeclaration -> BackedPackageTypeView(id, fact)
                is PackageSubtypeDeclaration -> BackedSubtypeView(id, fact)
                is PackageProcedureDeclaration -> BackedPackageProcedureView(id, fact)
                is PackageFunctionDeclaration -> BackedPackageFunctionView(id, fact)
                is StandaloneProcedureDeclaration -> BackedStandaloneProcedureView(id, fact)
                is StandaloneFunctionDeclaration -> BackedStandaloneFunctionView(id, fact)
                is PackageValueDeclaration -> BackedPackageValueView(id, fact)
                else -> BackedDeclarationView(id, fact)
            }
        }

    /** Retains only the issued ID and the authoritative fact; never the snapshot or index. */
    private open class BackedDeclarationView(
        override val id: ProjectDeclarationId,
        fact: ProjectDeclaration
    ) : ProjectDeclarationView {
        private val fact: ProjectDeclaration = fact

        override val kind: ProjectDeclarationKind
            get() = when (fact.kind) {
            FactKind.PACKAGE -> ProjectDeclarationKind.PACKAGE
            FactKind.PACKAGE_BODY -> ProjectDeclarationKind.PACKAGE_BODY
            FactKind.STANDALONE_TYPE -> ProjectDeclarationKind.STANDALONE_TYPE
            FactKind.PACKAGE_TYPE -> ProjectDeclarationKind.PACKAGE_TYPE
            FactKind.PACKAGE_SUBTYPE -> ProjectDeclarationKind.PACKAGE_SUBTYPE
            FactKind.PACKAGE_PROCEDURE -> ProjectDeclarationKind.PACKAGE_PROCEDURE
            FactKind.PACKAGE_FUNCTION -> ProjectDeclarationKind.PACKAGE_FUNCTION
            FactKind.SEQUENCE -> ProjectDeclarationKind.SEQUENCE
            FactKind.STANDALONE_PROCEDURE -> ProjectDeclarationKind.STANDALONE_PROCEDURE
            FactKind.STANDALONE_FUNCTION -> ProjectDeclarationKind.STANDALONE_FUNCTION
            FactKind.PACKAGE_VARIABLE -> ProjectDeclarationKind.PACKAGE_VARIABLE
            FactKind.PACKAGE_CONSTANT -> ProjectDeclarationKind.PACKAGE_CONSTANT
        }

        override val role: ProjectDeclarationRole
            get() = when (fact.role) {
            DeclarationRole.SPECIFICATION -> ProjectDeclarationRole.SPECIFICATION
            DeclarationRole.BODY -> ProjectDeclarationRole.BODY
            DeclarationRole.STANDALONE -> ProjectDeclarationRole.STANDALONE
        }

        override val qualifiedName: ProjectQualifiedName
            get() = ProjectQualifiedName.wrap(fact.qualifiedName())

        override val fileId: String get() = fact.fileId.value

        override val sourceRange: ProjectSourceRange get() = fact.sourceRange.toPublic()

        override fun toString() = "$kind $qualifiedName [$id]"
    }

    private class BackedStandaloneTypeView(id: ProjectDeclarationId, fact: StandaloneTypeDeclaration) :
        BackedDeclarationView(id, fact), ProjectShapedTypeView {
        override val shape: Optional<ProjectTypeShape> = fact.shape.toPublicShape()
    }

    private class BackedPackageTypeView(id: ProjectDeclarationId, private val typeFact: PackageTypeDeclaration) :
        BackedDeclarationView(id, typeFact), ProjectPackageTypeView {
        override val shape: Optional<ProjectTypeShape> = typeFact.shape.toPublicShape()
        override val owner: ProjectQualifiedName get() = ProjectQualifiedName.wrap(typeFact.owner)
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(typeFact.name)
        override val recordFields: List<ProjectRecordFieldView> =
            Collections.unmodifiableList(typeFact.recordFields.map { BackedRecordFieldView(it) })
    }

    private class BackedSubtypeView(id: ProjectDeclarationId, private val subtypeFact: PackageSubtypeDeclaration) :
        BackedDeclarationView(id, subtypeFact), ProjectSubtypeView {
        override val owner: ProjectQualifiedName get() = ProjectQualifiedName.wrap(subtypeFact.owner)
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(subtypeFact.name)
        override val baseType: ProjectTypeReference get() = ProjectTypeReference.from(subtypeFact.baseType)
    }

    private class BackedPackageProcedureView(id: ProjectDeclarationId, private val procedure: PackageProcedureDeclaration) :
        BackedDeclarationView(id, procedure), ProjectPackageSubprogramView {
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(procedure.name)
        override val packageOwner: ProjectQualifiedName get() = ProjectQualifiedName.wrap(procedure.owner)
        override val parameters: List<ProjectParameterView> = procedure.parameters.toParameterViews()
    }

    private class BackedPackageFunctionView(id: ProjectDeclarationId, private val function: PackageFunctionDeclaration) :
        BackedDeclarationView(id, function), ProjectPackageSubprogramView, ProjectFunctionView {
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(function.name)
        override val packageOwner: ProjectQualifiedName get() = ProjectQualifiedName.wrap(function.owner)
        override val parameters: List<ProjectParameterView> = function.parameters.toParameterViews()
        override val returnType: ProjectTypeReference get() = ProjectTypeReference.from(function.returnType)
        override val isDeterministic: Boolean get() = function.deterministic
    }

    private class BackedPackageValueView(id: ProjectDeclarationId, private val valueFact: PackageValueDeclaration) :
        BackedDeclarationView(id, valueFact), ProjectPackageValueView {
        override val owner: ProjectQualifiedName get() = ProjectQualifiedName.wrap(valueFact.owner)
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(valueFact.name)
        override val isConstant: Boolean get() = valueFact.constant
        override val typeRef: ProjectTypeReference get() = ProjectTypeReference.from(valueFact.typeRef)
    }

    /** Standalone callables are never package members: no `packageOwner`, no schema inference. */
    private class BackedStandaloneProcedureView(id: ProjectDeclarationId, private val procedure: StandaloneProcedureDeclaration) :
        BackedDeclarationView(id, procedure), ProjectSubprogramView {
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(procedure.name.last)
        override val parameters: List<ProjectParameterView> = procedure.parameters.toParameterViews()
    }

    private class BackedStandaloneFunctionView(id: ProjectDeclarationId, private val function: StandaloneFunctionDeclaration) :
        BackedDeclarationView(id, function), ProjectFunctionView {
        override val localName: ProjectIdentifier get() = ProjectIdentifier.wrap(function.name.last)
        override val parameters: List<ProjectParameterView> = function.parameters.toParameterViews()
        override val returnType: ProjectTypeReference get() = ProjectTypeReference.from(function.returnType)
        override val isDeterministic: Boolean get() = function.deterministic
    }

    private fun List<ProjectParameter>.toParameterViews(): List<ProjectParameterView> =
        Collections.unmodifiableList(map { BackedParameterView(it) })

    private class BackedParameterView(private val parameter: ProjectParameter) : ProjectParameterView {
        override val name: ProjectIdentifier get() = ProjectIdentifier.wrap(parameter.name)
        override val ordinal: Int get() = parameter.ordinal
        override val mode: ProjectParameterMode
            get() = when (parameter.mode) {
                ParameterMode.IN -> ProjectParameterMode.IN
                ParameterMode.OUT -> ProjectParameterMode.OUT
                ParameterMode.IN_OUT -> ProjectParameterMode.IN_OUT
            }
        override val isNocopy: Boolean get() = parameter.nocopy
        override val typeRef: ProjectTypeReference get() = ProjectTypeReference.from(parameter.typeRef)
        override val isDefaultPresent: Boolean get() = parameter.defaultPresent
        override val sourceRange: ProjectSourceRange get() = parameter.sourceRange.toPublic()
    }

    private class BackedRecordFieldView(private val fieldFact: ProjectRecordField) : ProjectRecordFieldView {
        override val name: ProjectIdentifier get() = ProjectIdentifier.wrap(fieldFact.name)
        override val ordinal: Int get() = fieldFact.ordinal
        override val typeRef: ProjectTypeReference get() = ProjectTypeReference.from(fieldFact.typeRef)
        override val sourceRange: ProjectSourceRange get() = fieldFact.sourceRange.toPublic()
    }

    private fun FactShape?.toPublicShape(): Optional<ProjectTypeShape> = Optional.ofNullable(
        when (this) {
            null -> null
            FactShape.RECORD -> ProjectTypeShape.RECORD
            FactShape.OBJECT -> ProjectTypeShape.OBJECT
            FactShape.COLLECTION -> ProjectTypeShape.COLLECTION
            FactShape.REF_CURSOR -> ProjectTypeShape.REF_CURSOR
        }
    )
}

@OptIn(ZpaExperimentalApi::class)
@JvmSynthetic
internal fun SourceRange.toPublic() = ProjectSourceRange.create(fileId.value, startLine, startColumn, endLine, endColumn)
