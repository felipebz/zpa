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
package com.felipebz.zpa.project

/** Shared real-source fixtures for the internal regression suite and the corpus differential runner. */
internal object ProjectCallTestFixtures {
    fun identifier(source: String) = OracleIdentifier.fromSource(source)
    fun name(vararg segments: String) = QualifiedName(segments.map(::identifier))
    fun member(owner: String = "p", local: String = "run") =
        ProjectCallTarget.packageMember(name(owner), identifier(local))

    fun arguments(vararg labels: String?) = ProjectCallArguments.of(labels.map {
        if (it == null) ProjectCallArgument.positional() else ProjectCallArgument.named(identifier(it))
    })

    fun context(files: List<Pair<String, String>>, concurrent: Boolean = false, failed: Boolean = false): ProjectAnalysisContext {
        val sources = files.map { (file, source) -> ProjectSource(FileId(file)) { source } }.toMutableList()
        if (failed) sources += ProjectSource(FileId("failed.sql")) { throw IllegalStateException("reader failed") }
        return ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources, concurrent))
    }

    fun context(source: String) = context(listOf("spec.sql" to source))
    fun index(context: ProjectAnalysisContext) = (context.state as ProjectAnalysisContext.State.Prepared).result.index
    fun packageSource(body: String, owner: String = "p") = "CREATE PACKAGE $owner AS $body END ${owner.substringAfterLast('.')};"
    fun standalone(name: String = "x.y") = "CREATE PROCEDURE $name IS BEGIN NULL; END;"

    data class NamespaceCase(
        val label: String,
        val sources: List<String>,
        val status: ProjectCallCandidates.Status,
        val uncertainties: Set<ProjectCallUncertainty> = emptySet(),
        val candidateCount: Int = 0,
        val specificationCount: Int = 0
    )

    fun namespaceCases(): List<NamespaceCase> {
        val empty = packageSource("PROCEDURE other;", "x")
        val member = packageSource("PROCEDURE y;", "x")
        val gap = packageSource("PROCEDURE y(t TABLE) PROCEDURE coverage_recovery;", "x")
        val retainedGap = packageSource("PROCEDURE y; PROCEDURE y(t TABLE) PROCEDURE coverage_recovery;", "x")
        val unknown = packageSource("PROCEDURE ;", "x")
        val body = "CREATE PACKAGE BODY x AS PROCEDURE other IS BEGIN NULL; END; END x;"
        val standalone = standalone()
        val competition = ProjectCallUncertainty.NAMESPACE_COMPETITION
        val duplicate = ProjectCallUncertainty.DUPLICATE_SPECIFICATION
        val partial = ProjectCallUncertainty.PARTIAL_DECLARATIONS
        val result = mutableListOf(
            NamespaceCase("N1", emptyList(), ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT),
            NamespaceCase("N2", listOf(standalone), ProjectCallCandidates.Status.FOUND, candidateCount = 1),
            NamespaceCase("N3", listOf(empty), ProjectCallCandidates.Status.NOT_FOUND_IN_PROJECT, specificationCount = 1),
            NamespaceCase("N4", listOf(empty, standalone), ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(competition), 1, 1),
            NamespaceCase("N5", listOf(member), ProjectCallCandidates.Status.FOUND, candidateCount = 1, specificationCount = 1),
            NamespaceCase("N6", listOf(member, standalone), ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(competition), 2, 1),
            NamespaceCase("N9", listOf(body), ProjectCallCandidates.Status.UNKNOWN_PACKAGE),
            NamespaceCase("N9b", listOf(body, standalone), ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(competition), 1)
        )
        for ((suffix, specs) in listOf("neither" to listOf(empty, empty), "either" to listOf(member, empty), "both" to listOf(member, member))) {
            val count = specs.count { it == member }
            result += NamespaceCase("N7-$suffix", specs, ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(duplicate), count, 2)
            result += NamespaceCase("N7b-$suffix", specs + standalone, ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(competition, duplicate), count + 1, 2)
        }
        for ((suffix, spec, count) in listOf(Triple("skipped", gap, 0), Triple("retained", retainedGap, 1), Triple("unknown", unknown, 0))) {
            result += NamespaceCase("N8-$suffix", listOf(spec), ProjectCallCandidates.Status.PARTIAL_DECLARATIONS, setOf(partial), count, 1)
            result += NamespaceCase("N8b-$suffix", listOf(spec, standalone), ProjectCallCandidates.Status.AMBIGUOUS_TARGET, setOf(competition, partial), count + 1, 1)
        }
        return result
    }

    data class ZeroCase(val label: String, val sources: List<String>, val oldStatus: String, val adaptedStatus: String,
                        val failed: Boolean = false, val unprepared: Boolean = false)

    fun zeroCases() = listOf(
        ZeroCase("Z1", listOf(packageSource("PROCEDURE other;"), packageSource("PROCEDURE other;")), "AMBIGUOUS_TARGET", "AMBIGUOUS_TARGET"),
        ZeroCase("Z2", listOf(packageSource("PROCEDURE setup(t TABLE) PROCEDURE coverage_recovery;")), "NOT_FOUND", "INCOMPLETE"),
        ZeroCase("Z3", listOf(packageSource("PROCEDURE setup; PROCEDURE setup(t TABLE) PROCEDURE coverage_recovery;")), "RESOLVED", "INCOMPLETE"),
        ZeroCase("Z4", listOf(packageSource("PROCEDURE other;")), "NOT_FOUND", "NOT_FOUND"),
        ZeroCase("Z5", listOf(packageSource("FUNCTION setup RETURN NUMBER;")), "NOT_CALLABLE", "NOT_CALLABLE"),
        ZeroCase("Z6", listOf(packageSource("PROCEDURE setup(a NUMBER);")), "NOT_CALLABLE", "NOT_CALLABLE"),
        ZeroCase("Z7", listOf(packageSource("PROCEDURE setup(a NUMBER DEFAULT 1);")), "INCOMPLETE", "INCOMPLETE", failed = true),
        ZeroCase("Z8", listOf(packageSource("PROCEDURE setup; PROCEDURE setup(a NUMBER DEFAULT 1);")), "AMBIGUOUS", "AMBIGUOUS"),
        ZeroCase("Z9-body", listOf("CREATE PACKAGE BODY p AS PROCEDURE setup IS BEGIN NULL; END; END p;"), "UNKNOWN_TARGET", "UNKNOWN_TARGET"),
        ZeroCase("Z9-empty", emptyList(), "UNKNOWN_TARGET", "UNKNOWN_TARGET"),
        ZeroCase("Z10", listOf("CREATE PACKAGE p AS\n${'$'}IF DBMS_DB_VERSION.VER_LE_12 ${'$'}THEN\n PROCEDURE setup;\n${'$'}ELSE\n PROCEDURE setup(a NUMBER);\n${'$'}END\nEND p;"), "RESOLVED", "INCOMPLETE"),
        ZeroCase("Z11", emptyList(), "NOT_PREPARED", "NOT_PREPARED", unprepared = true)
    )

    fun oldStatus(result: ProjectPackageProcedureResolution) = when (result) {
        is ProjectPackageProcedureResolution.Resolved -> "RESOLVED"
        ProjectPackageProcedureResolution.NotFound -> "NOT_FOUND"
        ProjectPackageProcedureResolution.NotCallable -> "NOT_CALLABLE"
        ProjectPackageProcedureResolution.AmbiguousCall -> "AMBIGUOUS"
        ProjectPackageProcedureResolution.AmbiguousTarget -> "AMBIGUOUS_TARGET"
        ProjectPackageProcedureResolution.UnknownTarget -> "UNKNOWN_TARGET"
        ProjectPackageProcedureResolution.Incomplete -> "INCOMPLETE"
        ProjectPackageProcedureResolution.NotPrepared -> "NOT_PREPARED"
    }

    fun adaptedStatus(result: ProjectCallResolution) = when (result.status) {
        ProjectCallResolution.Status.NOT_PREPARED -> "NOT_PREPARED"
        ProjectCallResolution.Status.INCOMPLETE_INDEX, ProjectCallResolution.Status.PARTIAL_DECLARATIONS -> "INCOMPLETE"
        ProjectCallResolution.Status.AMBIGUOUS_TARGET -> "AMBIGUOUS_TARGET"
        ProjectCallResolution.Status.UNKNOWN_PACKAGE -> "UNKNOWN_TARGET"
        ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT -> "NOT_FOUND"
        ProjectCallResolution.Status.WRONG_KIND, ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE -> "NOT_CALLABLE"
        ProjectCallResolution.Status.RESOLVED -> "RESOLVED"
        ProjectCallResolution.Status.AMBIGUOUS -> "AMBIGUOUS"
        ProjectCallResolution.Status.INVALID_NOTATION -> error("Zero actuals cannot have invalid notation")
    }

    fun fingerprint(result: ProjectCallResolution): String = listOf(
        result.status, result.uncertainties.sortedBy { it.name }, result.packageEvidence,
        result.packageSpecifications.map { "${it.fileId.value}:${it.ordinal}" },
        result.candidates.map { candidate -> listOf(
            "${candidate.declaration.fileId.value}:${candidate.declaration.ordinal}", candidate.declaration.declaration.role,
            candidate.applicability, candidate.bindings.map { it.argumentOrdinal to it.parameter.ordinal },
            candidate.omittedParameters.map { it.ordinal }, candidate.missingParameters.map { it.ordinal }, candidate.rejectedArgumentOrdinal
        ) },
        result.selected?.declaration?.let { "${it.fileId.value}:${it.ordinal}" },
        result.failures.map { it.toString() }
    ).toString()
}
