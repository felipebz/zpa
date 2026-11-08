//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Call specification headings of a package specification are extracted like any other heading. */
@OptIn(ZpaExperimentalApi::class)
class ProjectPackageCallSpecificationTest {
    private fun prepare(vararg files: Pair<String, String>): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)

    private fun id(s: String) = ProjectIdentifier.fromSource(s)

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("forms")
    fun everyFormIsExtractedWithItsParameters(kind: String, keyword: String, language: String) {
        val function = kind == "function"
        val heading = "$kind run_it(p_count number, p_label in varchar2)" + (if (function) " return number" else "")
        val s = prepare("j.sql" to "create or replace package pk_forms as\n  $heading $keyword $language;\n  procedure plain;\nend;\n/\n")
        val found = s.findPackageSubprograms(ProjectQualifiedName.of("pk_forms"), id("run_it"))
        assertThat(found.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        val member = found.declarations.single()
        assertThat(member.kind).isEqualTo(if (function) ProjectDeclarationKind.PACKAGE_FUNCTION else ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertThat(member.parameters.map { "${it.name.originalSpelling} ${it.typeRef.name.last.originalSpelling}" })
            .containsExactly("p_count number", "p_label varchar2")
        if (function) assertThat((member as ProjectFunctionView).returnType.name.last.originalSpelling).isEqualTo("number")
    }

    @Test
    fun aQualifiedCallResolvesToTheCallSpecification() {
        val spec = "create or replace package pk_j as\n" +
            "  function run(t number) return number as language java name 'A.run(int) return int';\n" +
            "  procedure c_run(t number) as language c name \"crun\" library lib_c;\n  procedure plain;\nend;\n/\n"
        val s = prepare("j.sql" to spec)
        val resolution = s.resolveCall(
            ProjectCallTarget.written(ProjectQualifiedName.of("pk_j", "run")), ProjectCallKind.FUNCTION,
            ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        assertThat(resolution.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(resolution.selected.get().declaration.qualifiedName.toString()).isEqualTo("pk_j.run")
    }

    companion object {
        private val languages = listOf(
            "language java name 'A.run(int, java.lang.String) return int'",
            "language c name \"c_run\" library lib_c",
            "external library lib_c name \"c_run\" parameters (p_count, p_label)",
            "mle module mod_j signature 'run(number, string)'"
        )

        @JvmStatic
        fun forms() = listOf("procedure", "function").flatMap { kind ->
            listOf("as", "is").flatMap { keyword -> languages.map { arrayOf(kind, keyword, it) } }
        }
    }
}
