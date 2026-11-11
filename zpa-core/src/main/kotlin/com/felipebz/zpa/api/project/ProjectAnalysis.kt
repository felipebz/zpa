//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.squid.SemanticFileNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.symbols.packageNameOf

/** Read-only project information available to a custom check during one analysis run. */
@ZpaExperimentalApi
class ProjectAnalysis private constructor(contextHolder: Any) {

    internal val projectAnalysisContext = contextHolder as? ProjectAnalysisContext
        ?: throw IllegalArgumentException("Unexpected project analysis context")
    /**
     * The immutable project snapshot of this analysis run, backed by the already prepared project
     * index (nothing is read or prepared again). The same instance is returned for the whole run;
     * a new run issues a new snapshot with its own declaration-ID scope. For a run without a
     * prepared project the snapshot reports [ProjectPreparationState.NOT_PREPARED].
     */
    val snapshot: ProjectSnapshot = ProjectSnapshot.create(projectAnalysisContext)

    /**
     * External metadata supplied for this run, with its compilation-schema rule; [ProjectExternalContext.none] when
     * the run has none. Use it with [snapshot]: `snapshot.describeExternalType(reference, owner,
     * externalContext.metadata, externalContext.compilationSchema(unitName).orElse(null))`.
     */
    val externalContext: ProjectExternalContext get() = projectAnalysisContext.externalContext

    private val specificationResolver = ProjectSubprogramSpecificationResolver(projectAnalysisContext)
    private val sequenceReferenceResolver = ProjectSequenceReferenceResolver(projectAnalysisContext)

    /**
     * Resolves the package specification corresponding to an immediate PACKAGE BODY declaration.
     * Nodes that are not associated package-body declarations return NOT_APPLICABLE.
     */
    @ZpaExperimentalApi
    fun resolvePackageSpecification(node: AstNode): PackageSpecificationResolution {
        if (projectAnalysisContext.state.kind == ProjectAnalysisContext.State.Kind.NOT_PREPARED) {
            return PackageSpecificationResolution.notPrepared()
        }
        val body = SemanticFileNode.of(node)?.projectSubprogramDeclaration(node)
            ?: return PackageSpecificationResolution.notApplicable()

        return when (val resolution = specificationResolver.resolve(body)) {
            is ProjectSubprogramSpecificationResolution.Resolved ->
                PackageSpecificationResolution.resolved(
                    resolution.body.toPublicView(),
                    resolution.specification.toPublicView()
                )
            is ProjectSubprogramSpecificationResolution.NotFound -> PackageSpecificationResolution.notFound()
            is ProjectSubprogramSpecificationResolution.Ambiguous -> PackageSpecificationResolution.ambiguous()
            is ProjectSubprogramSpecificationResolution.Incomplete -> PackageSpecificationResolution.incomplete()
            is ProjectSubprogramSpecificationResolution.NotPrepared -> PackageSpecificationResolution.notPrepared()
        }
    }

    /**
     * Resolves a public package procedure that can be invoked without arguments. The node is
     * used only to establish the current package for unqualified references and must come from
     * the current analysis callback.
     */
    @ZpaExperimentalApi
    fun resolvePackageProcedure(
        node: AstNode,
        reference: PackageProcedureReference
    ): PackageProcedureResolution {
        if (projectAnalysisContext.state.kind == ProjectAnalysisContext.State.Kind.NOT_PREPARED) {
            return PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_PREPARED)
        }
        val packageNode = when (node.type) {
            PlSqlGrammar.CREATE_PACKAGE,
            PlSqlGrammar.CREATE_PACKAGE_BODY -> node
            else -> node.getFirstAncestorOrNull(
                PlSqlGrammar.CREATE_PACKAGE,
                PlSqlGrammar.CREATE_PACKAGE_BODY
            )
        } ?: return PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_APPLICABLE)
        val currentPackage = packageNameOf(packageNode)
            ?: return PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_APPLICABLE)

        val packageName = reference.getPackageName().orElse(null)
        val owner = reference.getOwner().orElse(null)
        val targetPackage = when {
            packageName == null -> currentPackage
            owner != null -> QualifiedName(
                listOf(
                    OracleIdentifier.fromSource(owner),
                    OracleIdentifier.fromSource(packageName)
                )
            )
            else -> QualifiedName(
                currentPackage.segments.dropLast(1) + OracleIdentifier.fromSource(packageName)
            )
        }
        val resolution = ProjectPackageProcedureResolver(projectAnalysisContext).resolve(
            targetPackage = targetPackage,
            procedureName = OracleIdentifier.fromSource(reference.procedureName)
        )
        return when (resolution) {
            is ProjectPackageProcedureResolution.Resolved ->
                PackageProcedureResolution.resolved(resolution.declaration.toPublicView())
            ProjectPackageProcedureResolution.NotFound ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_FOUND)
            ProjectPackageProcedureResolution.NotCallable ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_CALLABLE)
            ProjectPackageProcedureResolution.AmbiguousCall ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.AMBIGUOUS)
            ProjectPackageProcedureResolution.AmbiguousTarget ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.AMBIGUOUS_TARGET)
            ProjectPackageProcedureResolution.UnknownTarget ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.UNKNOWN_TARGET)
            ProjectPackageProcedureResolution.Incomplete ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.INCOMPLETE)
            ProjectPackageProcedureResolution.NotPrepared ->
                PackageProcedureResolution.of(PackageProcedureResolution.Status.NOT_PREPARED)
        }
    }

    /**
     * Resolves a sequence-shaped member expression using only positive project and file-local
     * semantic evidence. Unresolved or ambiguous references return UNKNOWN.
     */
    @ZpaExperimentalApi
    fun resolveSequenceReference(node: AstNode): SequenceReferenceResolution =
        when (sequenceReferenceResolver.resolve(node)) {
            ProjectSequenceReferenceResolution.RESOLVED_SEQUENCE ->
                SequenceReferenceResolution.RESOLVED_SEQUENCE
            ProjectSequenceReferenceResolution.RESOLVED_NON_SEQUENCE ->
                SequenceReferenceResolution.RESOLVED_NON_SEQUENCE
            ProjectSequenceReferenceResolution.UNKNOWN -> SequenceReferenceResolution.UNKNOWN
        }

    @OptIn(ZpaExperimentalApi::class)
    private fun PackageSubprogramDeclaration.toPublicView(): PackageSubprogram {
        val kind = when (this) {
            is PackageProcedureDeclaration -> PackageSubprogramKind.PROCEDURE
            is PackageFunctionDeclaration -> PackageSubprogramKind.FUNCTION
        }
        return PackageSubprogram(
            kind,
            parameters.map { PackageParameter(it.ordinal, it.nocopy, it.defaultPresent) },
            (this as? PackageFunctionDeclaration)?.deterministic == true
        )
    }

    internal companion object {
        @JvmSynthetic
        fun create(contextHolder: Any): ProjectAnalysis = ProjectAnalysis(contextHolder)

        @JvmSynthetic
        fun notPrepared(): ProjectAnalysis = create(ProjectAnalysisContext.NOT_PREPARED)
    }
}
