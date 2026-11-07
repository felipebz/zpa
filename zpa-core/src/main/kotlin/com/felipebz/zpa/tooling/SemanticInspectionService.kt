//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.tooling

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationView
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution
import com.felipebz.zpa.api.project.ProjectShapedTypeView
import com.felipebz.zpa.api.project.ProjectTypeDeclarationView
import com.felipebz.zpa.api.semantics.FieldBinding
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.semantics.ValueCategory
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.SemanticAnalysisPipeline
import com.felipebz.zpa.squid.SemanticScanCapabilities
import com.felipebz.zpa.squid.createProjectAnalysis
import com.felipebz.zpa.symbols.local.*
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
            it.context = context.takeIf { facts != null }
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

    /** The analyzed file's context, whose local answers (value categories, type names, fields) the inspection reads. */
    internal var context: PlSqlVisitorContext? = null

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
        ProjectOverviewBuilder(fileId, root, localSemanticFacts, projectAnalysisContext, context).build()
    }

    public fun inspect(node: AstNode): SemanticNodeInspection =
        SemanticInspectionMapper.inspect(fileId, node, projectAnalysis, localSemanticFacts, context)
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
    /** For a `DATATYPE` writing a type name: the first declaration the name reaches, or why there is none. */
    public val typeName: TypeNameSummary?,
    /** For a `DATATYPE`: the category of the type it writes; null when there is none to give. */
    public val typeCategory: String?,
    /** For a value name, a member expression or a receiver: the category of the value; null when there is none to give. */
    public val valueCategory: String?,
    /** The declared project type of the local declaration the node names or declares; null when there is none. */
    public val declaredProjectType: ProjectDeclarationSummary?,
    /** The field each name of the node written after a value names: the node itself, or each member name it holds. */
    public val fields: List<FieldBindingSummary>
)

public enum class TypeNameState {
    /** A declaration of this file. */
    LOCAL,
    /** A declaration of another file of the project. */
    PROJECT,
    BUILT_IN,
    AMBIGUOUS,
    NOT_FOUND_IN_PROJECT,
    INCOMPLETE_INDEX,
    NOT_PREPARED,
    /** Text this file may hold differently (a `$IF`, a substitution), or a lookup the file cannot decide. */
    UNDECIDED
}

public data class TypeNameSummary(
    public val state: TypeNameState,
    public val reference: String,
    /** The project declaration: the one reached, or the one of the project a local declaration is. */
    public val declaration: ProjectDeclarationSummary?,
    /** For a [TypeNameState.LOCAL] name, the range of the declaration's name. */
    public val localDeclaration: SemanticSourceRange?,
    public val reasons: List<String>
)

public data class ProjectDeclarationSummary(
    public val kind: String,
    public val role: String,
    public val qualifiedName: String,
    public val fileId: String,
    public val sourceRange: SemanticSourceRange,
    public val shape: String?
)

public enum class FieldBindingState {
    /** A field of a record type or an attribute of an object type. */
    FIELD,
    /** Undecided: see the reasons. */
    UNKNOWN,
    /** Not a field (a method, a column, a name of no value), or not one the analysis covers. */
    NOT_A_FIELD
}

public data class FieldBindingSummary(
    public val name: String,
    public val sourceRange: SemanticSourceRange,
    public val state: FieldBindingState,
    /** For a [FieldBindingState.FIELD]: `RECORD_FIELD` or `ATTRIBUTE`, its zero-based ordinal and where it is declared. */
    public val kind: String?,
    public val ordinal: Int?,
    public val declaration: SemanticSourceRange?,
    public val reasons: List<String>
)

@OptIn(ZpaExperimentalApi::class)
private object SemanticInspectionMapper {
    /**
     * The declared type of the local declaration [node] names (certain, or the single provisional candidate) or
     * declares. Only the name itself answers: a node of exactly one token. A larger node starting with the name (a
     * statement, a member expression) has a type of its own, so it gets null.
     */
    fun declaredProjectType(facts: LocalSemanticFacts, categories: LocalValueCategories, node: AstNode): ProjectTypeDeclarationView? {
        val token = node.tokenOrNull ?: return null
        if (node.lastTokenOrNull !== token) return null
        val declaration = facts.nameDeclaration(token.line, token.column)
            ?: facts.model.declarations.firstOrNull { it.nameRange.startLine == token.line && it.nameRange.startColumn == token.column }
            ?: return null
        return categories.declaredProjectType(declaration)
    }

    fun inspect(
        fileId: FileId,
        node: AstNode,
        projectAnalysis: ProjectAnalysisSummary,
        facts: LocalSemanticFacts?,
        context: PlSqlVisitorContext?
    ): SemanticNodeInspection {
        val semanticNode = node as? SemanticAstNode
        val categories = context?.valueCategories
        val datatype = node.type === PlSqlGrammar.DATATYPE
        return SemanticNodeInspection(
            node = nodeSummary(fileId, node),
            projectAnalysis = projectAnalysis,
            legacy = semanticNode?.let(::legacySummary),
            typeName = categories?.takeIf { datatype }?.let { typeName(node, it.typeName(node), facts) },
            typeCategory = categories?.takeIf { datatype }?.typeCategory(node)?.takeUnless { it is ValueCategory.Unavailable }?.toString(),
            valueCategory = categories?.takeIf { !datatype }?.category(node)?.takeUnless { it is ValueCategory.Unavailable }?.toString(),
            declaredProjectType = facts?.let { f -> categories?.let { declaredProjectType(f, it, node) } }?.let(::declaration),
            fields = context?.let { fields(fileId, node, it) }.orEmpty()
        )
    }

    private fun typeName(node: AstNode, name: LocalTypeName, facts: LocalSemanticFacts?): TypeNameSummary? {
        val reference = node.tokens.joinToString("") { it.originalValue }
        return when (name) {
            is LocalTypeName.Local -> TypeNameSummary(
                TypeNameState.LOCAL, reference, name.project?.let(::declaration),
                facts?.model?.declaration(name.declaration)?.nameRange?.let(::sourceRange), emptyList()
            )
            is LocalTypeName.Project -> TypeNameSummary(TypeNameState.PROJECT, reference, declaration(name.declaration), null, emptyList())
            is LocalTypeName.BuiltIn -> TypeNameSummary(TypeNameState.BUILT_IN, reference, null, null, emptyList())
            is LocalTypeName.Unresolved -> TypeNameSummary(
                when (name.resolution.status) {
                    ProjectNamedTypeResolution.Status.AMBIGUOUS -> TypeNameState.AMBIGUOUS
                    ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX -> TypeNameState.INCOMPLETE_INDEX
                    ProjectNamedTypeResolution.Status.NOT_PREPARED -> TypeNameState.NOT_PREPARED
                    else -> TypeNameState.NOT_FOUND_IN_PROJECT
                },
                reference, null, null, emptyList()
            )
            is LocalTypeName.Undecided -> TypeNameSummary(TypeNameState.UNDECIDED, reference, null, null, name.reasons.map { it.name }.sorted())
            LocalTypeName.Unavailable -> null
        }
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

    private fun declaration(view: ProjectDeclarationView): ProjectDeclarationSummary =
        ProjectDeclarationSummary(
            kind = view.kind.name,
            role = view.role.name,
            qualifiedName = view.qualifiedName.toString(),
            fileId = view.fileId,
            sourceRange = view.sourceRange.let { SemanticSourceRange(it.fileId, it.startLine, it.startColumn, it.endLine, it.endColumn) },
            shape = (view as? ProjectShapedTypeView)?.shape?.orElse(null)?.name
        )

    /**
     * The fields the names of [node] written after a value name: [node] itself when it is such a name, else each name
     * of a member expression after its first (every name of a member item of an expression receiver). Empty when none
     * names a field or is undecided.
     */
    @OptIn(ZpaExperimentalApi::class)
    private fun fields(fileId: FileId, node: AstNode, context: PlSqlVisitorContext): List<FieldBindingSummary> {
        val names = when (node.type) {
            PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME -> listOf(node)
            PlSqlGrammar.MEMBER_EXPRESSION -> node.children.filter { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.IDENTIFIER_NAME }
                .let { if (LocalSyntax.isReceiverItem(node)) it else it.drop(1) }
            else -> emptyList()
        }
        val fields = names.map { name -> field(fileId, name, context.fieldBinding(name)) }
        return if (fields.all { it.state == FieldBindingState.NOT_A_FIELD }) emptyList() else fields
    }

    @OptIn(ZpaExperimentalApi::class)
    private fun field(fileId: FileId, name: AstNode, binding: FieldBinding): FieldBindingSummary {
        val range = SemanticSourceRange(fileId.value, name.token.line, name.token.column, name.lastToken.endLine, name.lastToken.endColumn)
        val text = name.tokens.joinToString("") { it.originalValue }
        return when (binding) {
            is FieldBinding.Field -> binding.declaration.let {
                FieldBindingSummary(
                    text, range, FieldBindingState.FIELD, it.kind.name, it.ordinal,
                    SemanticSourceRange(it.fileId ?: fileId.value, it.line, it.column, it.line, it.column), emptyList()
                )
            }
            is FieldBinding.Unknown -> FieldBindingSummary(text, range, FieldBindingState.UNKNOWN, null, null, null, binding.reasons.map { it.name }.sorted())
            is FieldBinding.Unavailable -> FieldBindingSummary(text, range, FieldBindingState.NOT_A_FIELD, null, null, null, listOf(binding.reason.name))
        }
    }

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
