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
package com.felipebz.zpa.tooling

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.SemanticAnalysisPipeline
import com.felipebz.zpa.squid.SemanticScanCapabilities
import com.felipebz.zpa.squid.createProjectAnalysis
import com.felipebz.zpa.symbols.local.LocalHighlighting
import com.felipebz.zpa.symbols.local.LocalSemanticFacts
import java.nio.file.Path

/**
 * Runs ZPA's semantic decoration pipeline and exposes a read-only diagnostic snapshot.
 *
 * This service is intended for developer tooling and diagnostics. It is not the semantic
 * API for custom rules and deliberately converts project semantic state to immutable DTOs.
 */
public class SemanticInspectionService {
    /**
     * [sourceCode] is the text [root] was parsed from: with it the local resolution, and so the highlighting pairs, are
     * the scanner's for the same file and project.
     */
    @OptIn(ZpaExperimentalApi::class)
    @JvmOverloads
    public fun analyze(
        root: AstNode,
        fileId: FileId,
        projectAnalysisContext: ProjectAnalysisContext,
        sourceCode: String? = null
    ): SemanticInspectionResult {
        val visitors = SemanticAnalysisPipeline.forTooling(projectAnalysisContext).create(fileId)
        val context = PlSqlVisitorContext(root, sourceCode?.let { InspectedFile(fileId, it) }, null)
        // The scan's project analysis, as the scanner installs it.
        PlSqlAstWalker(visitors.all).walk(context, SemanticScanCapabilities(createProjectAnalysis(projectAnalysisContext)))
        return SemanticInspectionResult(
            fileId,
            root,
            visitors.symbolVisitor.symbolTable,
            projectAnalysisSummary(projectAnalysisContext),
            projectAnalysisContext
        ).also {
            val facts = context.localSemanticFacts
            it.localSemanticFacts = facts
            it.highlightingPairs = facts?.let { local -> LocalHighlighting.pairs(local, LocalHighlighting.indexOf(projectAnalysisContext), sourceCode) }.orEmpty()
        }
    }

    private class InspectedFile(private val fileId: FileId, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = fileId.value
        override fun path(): Path = Path.of("")
        override fun type() = PlSqlFile.Type.MAIN
    }
}

/** A completed semantic analysis that can be queried for diagnostic information per AST node. */
public class SemanticInspectionResult internal constructor(
    private val fileId: FileId,
    private val root: AstNode,
    @Suppress("DEPRECATION")
    public val symbolTable: SymbolTable,
    public val projectAnalysis: ProjectAnalysisSummary,
    private val projectAnalysisContext: ProjectAnalysisContext
) {
    /** The local semantic resolution the pipeline's pre-pass left; internal, for diagnostics and tests. */
    internal var localSemanticFacts: LocalSemanticFacts? = null

    /** The file's highlighting pairs, the ones the scanner gives for the same file and project; empty when not analyzed. */
    @ZpaExperimentalApi
    public var highlightingPairs: List<HighlightingPair> = emptyList()
        internal set

    /** The local resolution's regions, declarations and references, built on first access; null when not analyzed. */
    @OptIn(ZpaExperimentalApi::class)
    public val symbolTree: SymbolTreeNode? by lazy {
        localSemanticFacts?.let { SymbolTreeBuilder(fileId, it, highlightingPairs).build() }
    }

    /** Everything the Project System captured for this file, built on first access. */
    public val projectOverview: ProjectOverviewNode by lazy {
        ProjectOverviewBuilder(fileId, root, localSemanticFacts, projectAnalysisContext).build()
    }

    public fun inspect(node: AstNode): SemanticNodeInspection =
        SemanticInspectionMapper.inspect(fileId, node, projectAnalysis, localSemanticFacts)
}

/** Immutable summary of the project preparation used by one semantic inspection. */
public data class ProjectAnalysisSummary(
    public val state: String,
    public val attemptedFileCount: Int,
    public val successfulFileCount: Int,
    public val declarationCount: Int,
    public val failures: List<ProjectPreparationFailureSummary>
)

public data class ProjectPreparationFailureSummary(
    public val fileId: String,
    public val exceptionType: String
)

public data class SemanticSourceRange(
    public val fileId: String,
    public val startLine: Int,
    public val startColumn: Int,
    public val endLine: Int,
    public val endColumn: Int
)

public data class SemanticNodeSummary(
    public val grammarType: String,
    public val sourceValue: String?,
    public val sourceRange: SemanticSourceRange?
)

public data class LegacySymbolSummary(
    public val name: String,
    public val kind: String,
    public val type: String,
    public val datatype: String
)

public data class LegacySemanticSummary(
    public val symbol: LegacySymbolSummary?,
    public val type: String?,
    public val datatype: String?
)

public data class SemanticNodeInspection(
    public val node: SemanticNodeSummary,
    public val projectAnalysis: ProjectAnalysisSummary,
    public val legacy: LegacySemanticSummary?,
    public val projectTypeResolution: ProjectTypeResolutionSummary?,
    /** The declared project type of the local declaration the node names or declares; null when there is none. */
    public val declaredProjectType: ProjectDeclarationSummary?,
    public val projectRecordMemberResolution: ProjectRecordMemberResolutionSummary?,
    public val projectRecordFieldTypeResolution: ProjectRecordFieldTypeResolutionSummary?,
    public val projectRecordMemberPathResolution: ProjectRecordMemberPathResolutionSummary?
)

public enum class ProjectTypeResolutionState {
    RESOLVED,
    AMBIGUOUS,
    NOT_FOUND_IN_PROJECT,
    INCOMPLETE_INDEX,
    NOT_PREPARED
}

public data class ProjectTypeResolutionSummary(
    public val state: ProjectTypeResolutionState,
    public val reference: String,
    public val declaration: ProjectDeclarationSummary?,
    public val candidates: List<ProjectDeclarationSummary>,
    public val knownCandidates: List<ProjectDeclarationSummary>,
    public val preparationFailures: List<ProjectPreparationFailureSummary>
)

public data class ProjectDeclarationSummary(
    public val kind: String,
    public val role: String,
    public val qualifiedName: String,
    public val fileId: String,
    public val sourceRange: SemanticSourceRange,
    public val shape: String?
)

public data class SemanticTypeRefSummary(
    public val kind: String,
    public val name: String,
    public val anchor: String?,
    public val sourceRange: SemanticSourceRange
)

public data class ProjectRecordFieldSummary(
    public val name: String,
    public val ordinal: Int,
    public val typeRef: SemanticTypeRefSummary,
    public val sourceRange: SemanticSourceRange
)

public enum class ProjectRecordMemberResolutionState {
    RESOLVED,
    NOT_FOUND,
    AMBIGUOUS,
    INCOMPLETE,
    /** A method set of an object type; the overloads are not listed. */
    METHOD,
    /** A built-in method of a collection type (COUNT, EXTEND, …). */
    COLLECTION_METHOD,
    UNSUPPORTED_TYPE
}

public data class ProjectRecordMemberResolutionSummary(
    public val state: ProjectRecordMemberResolutionState,
    public val containingDeclaration: ProjectDeclarationSummary,
    public val field: ProjectRecordFieldSummary?,
    public val candidates: List<ProjectRecordFieldSummary>
)

public enum class ProjectRecordFieldTypeResolutionState {
    BUILT_IN,
    PROJECT,
    UNSUPPORTED
}

public data class ProjectRecordFieldTypeResolutionSummary(
    public val state: ProjectRecordFieldTypeResolutionState,
    public val field: ProjectRecordFieldSummary,
    public val typeRef: SemanticTypeRefSummary,
    public val resolution: ProjectTypeResolutionSummary?,
    public val semanticType: String? = null
)

public data class ProjectRecordMemberPathSegmentSummary(
    public val ordinal: Int,
    public val field: ProjectRecordFieldSummary,
    public val fieldTypeResolution: ProjectRecordFieldTypeResolutionSummary
)

public enum class ProjectRecordMemberPathResolutionState {
    COMPLETED,
    STOPPED
}

public data class ProjectRecordMemberPathResolutionSummary(
    public val state: ProjectRecordMemberPathResolutionState,
    public val segments: List<ProjectRecordMemberPathSegmentSummary>,
    public val nextMember: String?,
    public val nextMemberOrdinal: Int?,
    public val stopReason: String?
)

private object SemanticInspectionMapper {
    /**
     * The declared type of the local declaration [node] names (certain, or the single provisional candidate) or
     * declares. Only the name itself answers: a node of exactly one token. A larger node starting with the name (a
     * statement, a member expression) has a type of its own, so it gets null.
     */
    fun declaredProjectType(facts: LocalSemanticFacts, node: AstNode): ProjectTypeDeclaration? {
        val token = node.tokenOrNull ?: return null
        if (node.lastTokenOrNull !== token) return null
        val declaration = facts.nameDeclaration(token.line, token.column)
            ?: facts.model.declarations.firstOrNull { it.nameRange.startLine == token.line && it.nameRange.startColumn == token.column }
            ?: return null
        return facts.declaredType(declaration)
    }

    fun inspect(
        fileId: FileId,
        node: AstNode,
        projectAnalysis: ProjectAnalysisSummary,
        facts: LocalSemanticFacts?
    ): SemanticNodeInspection {
        val semanticNode = node as? SemanticAstNode
        return SemanticNodeInspection(
            node = nodeSummary(fileId, node),
            projectAnalysis = projectAnalysis,
            legacy = semanticNode?.let(::legacySummary),
            projectTypeResolution = semanticNode?.projectTypeResolution?.let(::projectTypeResolution),
            declaredProjectType = facts?.let { declaredProjectType(it, node) }?.let(::declaration),
            projectRecordMemberResolution = semanticNode?.projectRecordMemberResolution?.let(::recordMemberResolution),
            projectRecordFieldTypeResolution = semanticNode?.projectRecordFieldTypeResolution?.let(::fieldTypeResolution),
            projectRecordMemberPathResolution = semanticNode?.projectRecordMemberPathResolution?.let(::memberPathResolution)
        )
    }

    private fun nodeSummary(fileId: FileId, node: AstNode): SemanticNodeSummary {
        val firstToken = node.tokenOrNull
        val lastToken = node.lastTokenOrNull
        return SemanticNodeSummary(
            grammarType = node.name,
            sourceValue = firstToken?.originalValue,
            sourceRange = if (firstToken != null && lastToken != null) {
                SemanticSourceRange(
                    fileId.value,
                    firstToken.line,
                    firstToken.column,
                    lastToken.endLine,
                    lastToken.endColumn
                )
            } else {
                null
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun legacySummary(node: SemanticAstNode): LegacySemanticSummary? {
        val symbol = node.symbol
        val datatype = node.plSqlDatatype
        if (symbol == null && datatype is UnknownDatatype) return null

        return LegacySemanticSummary(
            symbol = symbol?.let {
                LegacySymbolSummary(it.name, it.kind.name, it.type.toString(), it.datatype.toString())
            },
            type = datatype.type.toString(),
            datatype = datatype.toString()
        )
    }

    private fun projectTypeResolution(resolution: ProjectTypeResolution): ProjectTypeResolutionSummary {
        return when (resolution) {
            is ProjectTypeResolution.Resolved -> ProjectTypeResolutionSummary(
                ProjectTypeResolutionState.RESOLVED,
                resolution.reference.name.toString(),
                declaration(resolution.declaration),
                emptyList(),
                emptyList(),
                emptyList()
            )
            is ProjectTypeResolution.Ambiguous -> ProjectTypeResolutionSummary(
                ProjectTypeResolutionState.AMBIGUOUS,
                resolution.reference.name.toString(),
                null,
                resolution.candidates.map(::declaration),
                emptyList(),
                emptyList()
            )
            is ProjectTypeResolution.NotFoundInProject -> ProjectTypeResolutionSummary(
                ProjectTypeResolutionState.NOT_FOUND_IN_PROJECT,
                resolution.reference.name.toString(),
                null,
                emptyList(),
                emptyList(),
                emptyList()
            )
            is ProjectTypeResolution.IncompleteIndex -> ProjectTypeResolutionSummary(
                ProjectTypeResolutionState.INCOMPLETE_INDEX,
                resolution.reference.name.toString(),
                null,
                emptyList(),
                resolution.knownCandidates.map(::declaration),
                resolution.failures.map(::preparationFailure)
            )
            is ProjectTypeResolution.NotPrepared -> ProjectTypeResolutionSummary(
                ProjectTypeResolutionState.NOT_PREPARED,
                resolution.reference.name.toString(),
                null,
                emptyList(),
                emptyList(),
                emptyList()
            )
        }
    }

    private fun declaration(projectDeclaration: ProjectTypeDeclaration): ProjectDeclarationSummary =
        ProjectDeclarationSummary(
            kind = projectDeclaration.kind.name,
            role = projectDeclaration.role.name,
            qualifiedName = projectDeclaration.qualifiedName.toString(),
            fileId = projectDeclaration.fileId.value,
            sourceRange = sourceRange(projectDeclaration.sourceRange),
            shape = when (projectDeclaration) {
                is com.felipebz.zpa.project.PackageTypeDeclaration -> projectDeclaration.shape?.name
                is com.felipebz.zpa.project.StandaloneTypeDeclaration -> projectDeclaration.shape?.name
                else -> null
            }
        )

    private fun recordMemberResolution(
        resolution: ProjectRecordMemberResolution
    ): ProjectRecordMemberResolutionSummary = when (resolution) {
        is ProjectRecordMemberResolution.Resolved -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.RESOLVED,
            declaration(resolution.declaration),
            field(resolution.field),
            emptyList()
        )
        is ProjectRecordMemberResolution.NotFound -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.NOT_FOUND,
            declaration(resolution.declaration),
            null,
            emptyList()
        )
        is ProjectRecordMemberResolution.Ambiguous -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.AMBIGUOUS,
            declaration(resolution.declaration),
            null,
            resolution.candidates.map(::field)
        )
        is ProjectRecordMemberResolution.Incomplete -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.INCOMPLETE,
            declaration(resolution.declaration),
            null,
            emptyList()
        )
        is ProjectRecordMemberResolution.Method -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.METHOD,
            declaration(resolution.declaration),
            null,
            emptyList()
        )
        is ProjectRecordMemberResolution.CollectionMethod -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.COLLECTION_METHOD,
            declaration(resolution.declaration),
            null,
            emptyList()
        )
        is ProjectRecordMemberResolution.UnsupportedType -> ProjectRecordMemberResolutionSummary(
            ProjectRecordMemberResolutionState.UNSUPPORTED_TYPE,
            declaration(resolution.declaration),
            null,
            emptyList()
        )
    }

    @OptIn(ZpaExperimentalApi::class)
    private fun fieldTypeResolution(
        resolution: ProjectRecordFieldTypeResolution
    ): ProjectRecordFieldTypeResolutionSummary {
        val semanticResolution = resolution.resolution
        return ProjectRecordFieldTypeResolutionSummary(
            state = when (semanticResolution) {
                is TypeRefSemanticResolution.BuiltIn -> ProjectRecordFieldTypeResolutionState.BUILT_IN
                is TypeRefSemanticResolution.Project -> ProjectRecordFieldTypeResolutionState.PROJECT
                is TypeRefSemanticResolution.Unsupported -> ProjectRecordFieldTypeResolutionState.UNSUPPORTED
            },
            field = field(resolution.field),
            typeRef = typeRef(semanticResolution.reference),
            resolution = (semanticResolution as? TypeRefSemanticResolution.Project)
                ?.resolution?.let(::projectTypeResolution),
            semanticType = (semanticResolution as? TypeRefSemanticResolution.BuiltIn)
                ?.type?.name
        )
    }

    private fun memberPathResolution(
        resolution: ProjectRecordMemberPathResolution
    ): ProjectRecordMemberPathResolutionSummary {
        val segments = resolution.segments.mapIndexed { index, segment ->
            ProjectRecordMemberPathSegmentSummary(
                index,
                field(segment.field),
                fieldTypeResolution(segment.fieldTypeResolution)
            )
        }
        return when (resolution) {
            is ProjectRecordMemberPathResolution.Completed -> ProjectRecordMemberPathResolutionSummary(
                ProjectRecordMemberPathResolutionState.COMPLETED,
                segments,
                null,
                null,
                null
            )
            is ProjectRecordMemberPathResolution.Stopped -> ProjectRecordMemberPathResolutionSummary(
                ProjectRecordMemberPathResolutionState.STOPPED,
                segments,
                resolution.nextMember.toString(),
                resolution.nextMemberOrdinal,
                resolution.reason.name
            )
        }
    }

    private fun field(projectField: ProjectRecordField): ProjectRecordFieldSummary =
        ProjectRecordFieldSummary(
            name = projectField.name.toString(),
            ordinal = projectField.ordinal,
            typeRef = typeRef(projectField.typeRef),
            sourceRange = sourceRange(projectField.sourceRange)
        )

    private fun typeRef(reference: TypeRef): SemanticTypeRefSummary =
        SemanticTypeRefSummary(
            kind = when (reference) {
                is NamedTypeRef -> "Named"
                is AnchoredTypeRef -> "Anchored"
                is RefTypeRef -> "Ref"
            },
            // A remote anchor prints its database link with the name: `t.c@link`.
            name = reference.name.toString() + ((reference as? AnchoredTypeRef)?.link?.let { "@$it" } ?: ""),
            anchor = (reference as? AnchoredTypeRef)?.anchor?.name,
            sourceRange = sourceRange(reference.sourceRange)
        )

    private fun sourceRange(range: SourceRange): SemanticSourceRange =
        SemanticSourceRange(
            range.fileId.value,
            range.startLine,
            range.startColumn,
            range.endLine,
            range.endColumn
        )

    private fun preparationFailure(failure: ProjectIndexPreparationFailure) =
        ProjectPreparationFailureSummary(failure.fileId.value, failure.exceptionType)
}

private fun projectAnalysisSummary(context: ProjectAnalysisContext): ProjectAnalysisSummary = when (val state = context.state) {
    ProjectAnalysisContext.State.NotPrepared -> ProjectAnalysisSummary(
        state.kind.name,
        0,
        0,
        0,
        emptyList()
    )
    is ProjectAnalysisContext.State.Prepared -> ProjectAnalysisSummary(
        state.kind.name,
        state.result.attemptedFileCount,
        state.result.successfulFileCount,
        state.result.index.declarations.size,
        state.result.failures.map { ProjectPreparationFailureSummary(it.fileId.value, it.exceptionType) }
    )
}
