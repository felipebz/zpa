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
import com.felipebz.zpa.api.project.ProjectCallCandidate.Applicability.*
import com.felipebz.zpa.api.project.ProjectCallKind.FUNCTION
import com.felipebz.zpa.api.project.ProjectCallKind.PROCEDURE
import com.felipebz.zpa.api.project.ProjectCallResolution.Status
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import com.felipebz.zpa.project.ProjectCallKind as InternalCallKind

/** Public structural resolution, checked call by call against the authoritative internal binder. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallResolutionTest {
    private class Project(val snapshot: ProjectSnapshot, val context: ProjectAnalysisContext)

    private fun project(vararg files: Pair<String, String>, failed: Boolean = false, concurrent: Boolean = false): Project {
        val sources = files.map { (id, text) -> ProjectSource(FileId(id)) { text } }.toMutableList()
        if (failed) sources += ProjectSource(FileId("failed.sql")) { throw IllegalStateException("reader failed") }
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources, concurrent))
        return Project(ProjectSnapshot.create(context), context)
    }

    private fun n(vararg segments: String) = ProjectQualifiedName.of(*segments)
    private fun id(spelling: String) = ProjectIdentifier.fromSource(spelling)
    private fun member(pkg: String, name: String) = ProjectCallTarget.packageMember(n(pkg), id(name))

    /** `null` is a positional actual, a string a named one. */
    private fun args(vararg labels: String?) =
        ProjectCallArguments.of(labels.map { if (it == null) ProjectCallArgument.positional() else ProjectCallArgument.named(id(it)) })

    /** Resolves publicly and asserts every projected fact equals the binder's, plus public object identity. */
    private fun resolve(
        project: Project,
        target: ProjectCallTarget,
        kind: ProjectCallKind,
        arguments: ProjectCallArguments
    ): ProjectCallResolution {
        val internalKind = when (kind) {
            PROCEDURE -> InternalCallKind.PROCEDURE
            FUNCTION -> InternalCallKind.FUNCTION
        }
        val internal = ProjectCallBinder(project.context).resolve(target.toInternal(), internalKind, arguments.internal)
        val public = project.snapshot.resolveCall(target, kind, arguments)
        val label = "$target $kind $arguments"
        assertThat(public.status.name).describedAs(label).isEqualTo(internal.status.name)
        assertThat(public.target).isSameAs(target)
        assertThat(public.kind).isSameAs(kind)
        assertThat(public.arguments).isSameAs(arguments)
        assertThat(public.uncertainties.map { it.name }).containsExactlyInAnyOrderElementsOf(internal.uncertainties.map { it.name })
        assertThat(public.packageSpecifications.map { it.id.fileId to it.id.ordinal })
            .containsExactlyElementsOf(internal.packageSpecifications.map { it.fileId.value to it.ordinal })
        assertThat(public.failures.map { it.fileId }).containsExactlyElementsOf(internal.failures.map { it.fileId.value })
        assertThat(public.candidates).hasSameSizeAs(internal.candidates)
        public.candidates.zip(internal.candidates).forEach { (p, i) ->
            val declaration = p.declaration
            assertThat(declaration.id.fileId to declaration.id.ordinal).isEqualTo(i.declaration.fileId.value to i.declaration.ordinal)
            assertThat(declaration.role).isNotEqualTo(ProjectDeclarationRole.BODY)
            assertThat(p.applicability.name).describedAs(label).isEqualTo(i.applicability.name)
            assertThat(p.bindings.map { Triple(it.argumentOrdinal, it.parameter.ordinal, it.parameter.name) })
                .containsExactlyElementsOf(i.bindings.map { Triple(it.argumentOrdinal, it.parameter.ordinal, ProjectIdentifier.wrap(it.parameter.name)) })
            p.bindings.forEach {
                assertThat(it.argument).isSameAs(arguments.arguments[it.argumentOrdinal - 1])
                assertThat(it.parameter).isSameAs(declaration.parameters[it.parameter.ordinal - 1])
            }
            assertThat(p.omittedParameters.map { it.ordinal }).containsExactlyElementsOf(i.omittedParameters.map { it.ordinal })
            assertThat(p.missingParameters.map { it.ordinal }).containsExactlyElementsOf(i.missingParameters.map { it.ordinal })
            (p.omittedParameters + p.missingParameters).forEach {
                assertThat(it).isSameAs(declaration.parameters[it.ordinal - 1])
            }
            assertThat(if (p.rejectedArgumentOrdinal.isPresent) p.rejectedArgumentOrdinal.asInt else null)
                .isEqualTo(i.rejectedArgumentOrdinal)
        }
        val internalSelected = internal.candidates.indexOfFirst { it === internal.selected }
        assertThat(public.candidates.indexOfFirst { it === public.selected.orElse(null) }).isEqualTo(internalSelected)
        assertThat(public.selected.isPresent).isEqualTo(internal.selected != null)
        return public
    }

    private val spec = """
        CREATE PACKAGE p AS
          PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1, c NUMBER DEFAULT 2);
          FUNCTION f(x NUMBER DEFAULT 1) RETURN NUMBER;
          PROCEDURE copy(src IN rec_t, dst OUT NOCOPY rec_t);
          PROCEDURE lab(a NUMBER);
          PROCEDURE lab(b NUMBER);
          PROCEDURE boot;
          PROCEDURE boot(x NUMBER DEFAULT 1);
          PROCEDURE same(a NUMBER);
          FUNCTION same(a NUMBER) RETURN NUMBER;
          PROCEDURE "Quoted"("Lbl" NUMBER, y NUMBER);
          PROCEDURE m(a NUMBER, b NUMBER);
          PROCEDURE m(b NUMBER, a NUMBER);
        END p;
    """.trimIndent()
    private val body = "CREATE PACKAGE BODY p AS PROCEDURE run(a NUMBER, b NUMBER, c NUMBER) IS BEGIN NULL; END; END p;"
    private val pkg = project("spec.sql" to spec, "body.sql" to body)

    @Test
    fun positionalNamedMixedAndReorderedBinding() {
        val positional = resolve(pkg, member("p", "run"), PROCEDURE, args(null))
        assertThat(positional.status).isEqualTo(Status.RESOLVED)
        val selected = positional.selected.get()
        assertThat(positional.candidates.single()).isSameAs(selected)
        assertThat(selected.bindings.single().parameter.name).isEqualTo(id("a"))
        assertThat(selected.omittedParameters.map { it.name }).containsExactly(id("b"), id("c"))
        assertThat(selected.declaration.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(selected.declaration.id).isEqualTo(
            pkg.snapshot.findPackageSubprograms(n("p"), id("run")).declarations.single { it.role == ProjectDeclarationRole.SPECIFICATION }.id
        )

        val mixed = resolve(pkg, member("p", "run"), PROCEDURE, args(null, "c")).selected.get()
        assertThat(mixed.bindings.map { it.parameter.ordinal }).containsExactly(1, 3)
        assertThat(mixed.omittedParameters.map { it.ordinal }).containsExactly(2)

        val reordered = resolve(pkg, member("p", "copy"), PROCEDURE, args("dst", "src")).selected.get()
        assertThat(reordered.bindings.map { it.argumentOrdinal to it.parameter.ordinal }).containsExactly(1 to 2, 2 to 1)
        val dst = reordered.bindings[0].parameter
        assertThat(dst.mode).isEqualTo(ProjectParameterMode.OUT)
        assertThat(dst.isNocopy).isTrue()
        assertThat(dst.typeRef.name).isEqualTo(n("rec_t"))
        assertThat(dst.sourceRange.fileId).isEqualTo("spec.sql")
        assertThat(reordered.omittedParameters).isEmpty()
    }

    @Test
    fun rejectionsAndRequiredFormals() {
        fun single(arguments: ProjectCallArguments) = resolve(pkg, member("p", "run"), PROCEDURE, arguments).also {
            assertThat(it.status).isEqualTo(Status.NO_APPLICABLE_CANDIDATE)
            assertThat(it.selected).isEmpty
        }.candidates.single()

        val missingNamed = single(args("c"))
        assertThat(missingNamed.applicability).isEqualTo(MISSING_REQUIRED)
        assertThat(missingNamed.missingParameters.map { it.name }).containsExactly(id("a"))
        assertThat(single(ProjectCallArguments.none()).missingParameters.map { it.ordinal }).containsExactly(1)
        single(args(null, null, null, null)).let {
            assertThat(it.applicability).isEqualTo(TOO_MANY_ARGUMENTS)
            assertThat(it.rejectedArgumentOrdinal.asInt).isEqualTo(4)
        }
        single(args(null, "unknown")).let {
            assertThat(it.applicability).isEqualTo(UNKNOWN_LABEL)
            assertThat(it.rejectedArgumentOrdinal.asInt).isEqualTo(2)
            assertThat(it.bindings).isEmpty()
        }
        single(args(null, "a")).let {
            assertThat(it.applicability).isEqualTo(DUPLICATE_BINDING)
            assertThat(it.rejectedArgumentOrdinal.asInt).isEqualTo(2)
        }
        // A duplicate binding for one overload is a valid call of another.
        val m = resolve(pkg, member("p", "m"), PROCEDURE, args(null, "a"))
        assertThat(m.status).isEqualTo(Status.RESOLVED)
        assertThat(m.candidates.map { it.applicability }).containsExactly(DUPLICATE_BINDING, APPLICABLE)
        assertThat(m.selected.get()).isSameAs(m.candidates[1])
    }

    @Test
    fun ambiguityZeroActualsAndKinds() {
        val lab = resolve(pkg, member("p", "lab"), PROCEDURE, args(null))
        assertThat(lab.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(lab.candidates.map { it.applicability }).containsExactly(APPLICABLE, APPLICABLE)
        assertThat(resolve(pkg, member("p", "lab"), PROCEDURE, args("b")).selected.get().declaration.parameters.single().name)
            .isEqualTo(id("b"))

        val boot = resolve(pkg, member("p", "boot"), PROCEDURE, ProjectCallArguments.none())
        assertThat(boot.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(boot.candidates[1].omittedParameters.map { it.ordinal }).containsExactly(1)

        val function = resolve(pkg, member("p", "f"), FUNCTION, ProjectCallArguments.none())
        assertThat(function.status).isEqualTo(Status.RESOLVED)
        assertThat(function.selected.get().declaration).isInstanceOf(ProjectFunctionView::class.java)
        assertThat(resolve(pkg, member("p", "f"), PROCEDURE, ProjectCallArguments.none()).let { it.status to it.candidates.single().applicability })
            .isEqualTo(Status.WRONG_KIND to WRONG_KIND)

        val procedure = resolve(pkg, member("p", "same"), PROCEDURE, args(null))
        assertThat(procedure.candidates.map { it.applicability }).containsExactly(APPLICABLE, WRONG_KIND)
        assertThat(procedure.selected.get().declaration).isNotInstanceOf(ProjectFunctionView::class.java)
        val func = resolve(pkg, member("p", "same"), FUNCTION, args(null))
        assertThat(func.candidates.map { it.applicability }).containsExactly(WRONG_KIND, APPLICABLE)
        assertThat(func.selected.get().declaration).isInstanceOf(ProjectFunctionView::class.java)
    }

    @Test
    fun notationIsDataAndPrecedesEverything() {
        val afterNamed = args("a", null)
        assertThat(afterNamed.notation).isEqualTo(ProjectCallArguments.Notation.POSITIONAL_AFTER_NAMED)
        assertThat(afterNamed.invalidArgumentOrdinal.asInt).isEqualTo(2)
        val duplicate = args("a", "\"A\"")
        assertThat(duplicate.notation).isEqualTo(ProjectCallArguments.Notation.DUPLICATE_LABEL)
        assertThat(duplicate.invalidArgumentOrdinal.asInt).isEqualTo(2)
        assertThat(args("a", "\"a\"").notation).isEqualTo(ProjectCallArguments.Notation.VALID)
        assertThat(args("a", "\"a\"").invalidArgumentOrdinal).isEmpty
        assertThat(ProjectCallArguments.none().notation).isEqualTo(ProjectCallArguments.Notation.VALID)

        val notPrepared = Project(ProjectSnapshot.create(ProjectAnalysisContext.NOT_PREPARED), ProjectAnalysisContext.NOT_PREPARED)
        val failing = project("spec.sql" to spec, failed = true)
        for (p in listOf(pkg, notPrepared, failing)) {
            val result = resolve(p, member("p", "run"), PROCEDURE, afterNamed)
            assertThat(result.status).isEqualTo(Status.INVALID_NOTATION)
            assertThat(result.candidates).isEmpty()
            assertThat(result.failures).isEmpty()
            assertThat(result.packageSpecifications).isEmpty()
        }
        assertThat(resolve(notPrepared, member("p", "run"), PROCEDURE, args(null)).status).isEqualTo(Status.NOT_PREPARED)
        val empty = project()
        assertThat(empty.snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_EMPTY)
        assertThat(resolve(empty, member("p", "run"), PROCEDURE, args(null)).status).isEqualTo(Status.UNKNOWN_PACKAGE)
    }

    @Test
    fun oracleIdentifierIdentity() {
        val quoted = resolve(pkg, member("p", "\"Quoted\""), PROCEDURE, args("\"Lbl\"", "y"))
        assertThat(quoted.status).isEqualTo(Status.RESOLVED)
        assertThat(quoted.selected.get().bindings[0].parameter.name.originalSpelling).isEqualTo("\"Lbl\"")
        assertThat(resolve(pkg, member("p", "\"Quoted\""), PROCEDURE, args("lbl", "y")).candidates.single().applicability)
            .isEqualTo(UNKNOWN_LABEL)
        assertThat(resolve(pkg, member("p", "quoted"), PROCEDURE, args(null, null)).status).isEqualTo(Status.NOT_FOUND_IN_PROJECT)
        // Canonical equivalence: "P"."RUN" and "A" reach run(a ...).
        val canonical = resolve(pkg, ProjectCallTarget.packageMember(n("\"P\""), id("\"RUN\"")), PROCEDURE, args("\"A\""))
        assertThat(canonical.status).isEqualTo(Status.RESOLVED)
        assertThat(canonical.selected.get().bindings.single().argument.label.get()).isEqualTo(id("a"))
    }

    @Test
    fun targetUncertaintyNeverResolves() {
        val callSpec = "FUNCTION run(t NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME 'A.f(int) return int';"
        val retained = resolve(project("p.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); $callSpec END p;"), member("p", "run"), PROCEDURE, args(null))
        assertThat(retained.status).isEqualTo(Status.PARTIAL_DECLARATIONS)
        assertThat(retained.candidates.single().applicability).isEqualTo(APPLICABLE)
        assertThat(retained.selected).isEmpty

        val dropped = resolve(project("p.sql" to "CREATE PACKAGE p AS $callSpec END p;"), member("p", "run"), FUNCTION, args(null))
        assertThat(dropped.status).isEqualTo(Status.PARTIAL_DECLARATIONS)
        assertThat(dropped.candidates).isEmpty()

        val duplicates = project("a.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;", "b.sql" to "CREATE PACKAGE p AS PROCEDURE other; END p;")
        val none = resolve(duplicates, member("p", "run"), PROCEDURE, ProjectCallArguments.none())
        assertThat(none.status).isEqualTo(Status.AMBIGUOUS_TARGET)
        assertThat(none.candidates).isEmpty()
        assertThat(none.uncertainties).containsExactly(ProjectCallUncertainty.DUPLICATE_SPECIFICATION)

        val competition = project("x.sql" to "CREATE PACKAGE x AS PROCEDURE other; END x;", "y.sql" to "CREATE PROCEDURE x.y IS BEGIN NULL; END;")
        val one = resolve(competition, ProjectCallTarget.written(n("x", "y")), PROCEDURE, ProjectCallArguments.none())
        assertThat(one.status).isEqualTo(Status.AMBIGUOUS_TARGET)
        assertThat(one.candidates.single().applicability).isEqualTo(APPLICABLE)
        assertThat(one.selected).isEmpty
        assertThat(resolve(competition, ProjectCallTarget.standalone(n("x", "y")), PROCEDURE, ProjectCallArguments.none()).status)
            .isEqualTo(Status.RESOLVED)

        val failed = resolve(
            project("p.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); $callSpec END p;", failed = true),
            member("p", "run"), PROCEDURE, args(null)
        )
        assertThat(failed.status).isEqualTo(Status.INCOMPLETE_INDEX)
        assertThat(failed.uncertainties).containsExactly(ProjectCallUncertainty.PARTIAL_DECLARATIONS)
        assertThat(failed.candidates.single().applicability).isEqualTo(APPLICABLE)
        assertThat(failed.selected).isEmpty
        assertThat(failed.failures.single().fileId).isEqualTo("failed.sql")
    }

    @Test
    fun requestAndResultCollectionsAreDefensiveAndUnmodifiable() {
        val source = mutableListOf(ProjectCallArgument.positional())
        val arguments = ProjectCallArguments.of(source)
        source.clear()
        assertThat(arguments.arguments).hasSize(1)
        val result = resolve(pkg, member("p", "run"), PROCEDURE, arguments)
        val candidate = result.candidates.single()
        @Suppress("UNCHECKED_CAST")
        listOf(arguments.arguments, result.candidates, result.packageSpecifications, result.failures,
            candidate.bindings, candidate.omittedParameters, candidate.missingParameters).forEach {
            assertThatThrownBy { (it as MutableList<Any?>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        }
        assertThatThrownBy { (result.uncertainties as MutableSet<ProjectCallUncertainty>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun serialConcurrentAndReversedPreparationAgree() {
        val files = listOf("spec.sql" to spec, "body.sql" to body, "dup.sql" to "CREATE PACKAGE q AS PROCEDURE r; END q;",
            "dup2.sql" to "CREATE PACKAGE q AS PROCEDURE r(a NUMBER DEFAULT 1); END q;")
        val requests = listOf(
            Triple(member("p", "run"), PROCEDURE, args(null, "c")), Triple(member("p", "lab"), PROCEDURE, args(null)),
            Triple(member("p", "m"), PROCEDURE, args(null, "a")), Triple(member("q", "r"), PROCEDURE, ProjectCallArguments.none())
        )
        fun summary(p: Project) = requests.map { (target, kind, arguments) ->
            resolve(p, target, kind, arguments).let { r ->
                listOf(r.status, r.uncertainties, r.candidates.map { listOf(it.declaration.id.fileId, it.declaration.id.ordinal, it.applicability) },
                    r.selected.map { it.declaration.id.ordinal }.orElse(-1))
            }
        }
        val serial = summary(project(*files.toTypedArray()))
        assertThat(summary(project(*files.toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(summary(project(*files.reversed().toTypedArray()))).isEqualTo(serial)
    }
}
