//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Calls, package values and types reached through private and public synonyms, as Oracle resolves them. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCallSynonymTest {
    private val appB = ProjectIdentifier.fromSource("APP_B")

    private val snapshot = ProjectSnapshots.prepare(
        listOf(
            ProjectSourceInput.ofText(
                "core.sql",
                "create package core_a.pk_tax as\n  function rate(p number) return number;\n  g_value number;\n  type t_rec is record (a number);\nend;\n/\n" +
                    "create function core_a.fn_calc(p number) return number is begin return 1; end;\n/\n"
            ),
            ProjectSourceInput.ofText("other.sql", "create package other_c.pk_tax as\n  function rate(p number) return number;\nend;\n/\n")
        ),
        false
    )

    private fun synonym(owner: String, name: String, targetOwner: String?, target: String, link: String? = null) =
        ProjectExternalSynonym.of(owner, name, targetOwner, target, link)

    private fun metadata(complete: Boolean = true, publicComplete: Boolean = true, vararg synonyms: ProjectExternalSynonym, more: ProjectExternalMetadata.Builder.() -> Unit = {}) =
        ProjectExternalMetadata.builder("fixture").apply {
            if (complete) completeSchema("APP_B")
            if (publicComplete) completePublicSynonyms()
            synonyms.forEach { synonym(it) }
            more()
        }.build()

    private fun call(md: ProjectExternalMetadata, vararg name: String) = snapshot.preferring(appB, md).resolveCall(
        ProjectCallTarget.written(ProjectQualifiedName.of(*name)), ProjectCallKind.FUNCTION, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
    )

    private fun owner(resolution: ProjectCallResolution) =
        (resolution.selected.get().declaration as ProjectPackageSubprogramView).packageOwner.toString().lowercase()

    private fun chain(synonyms: List<ProjectExternalSynonym>) = synonyms.map { "${it.owner.identifier.lookupName}.${it.name.identifier.lookupName}" }

    private val privateTax = synonym("APP_B", "TAX", "CORE_A", "PK_TAX")

    @Test
    fun aPrivateOrAPublicSynonymLeadsToThePackageOfTheProject() {
        val viaPrivate = call(metadata(synonyms = arrayOf(privateTax)), "tax", "rate")
        assertThat(viaPrivate.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(owner(viaPrivate)).isEqualTo("core_a.pk_tax")
        assertThat(chain(viaPrivate.synonyms)).containsExactly("APP_B.TAX")
        assertThat(viaPrivate.synonymTarget).contains(ProjectQualifiedName.of("CORE_A", "PK_TAX"))

        val viaPublic = call(metadata(synonyms = arrayOf(synonym("PUBLIC", "RATES", "CORE_A", "PK_TAX"))), "rates", "rate")
        assertThat(owner(viaPublic)).isEqualTo("core_a.pk_tax")
        assertThat(viaPublic.synonyms.single().isPublic).isTrue()

        // Discovery and narrowing go the same way.
        val candidates = snapshot.preferring(appB, metadata(synonyms = arrayOf(privateTax)))
            .findCallCandidates(ProjectCallTarget.written(ProjectQualifiedName.of("tax", "rate")))
        assertThat(candidates.status).isEqualTo(ProjectCallCandidates.Status.FOUND)
        assertThat(chain(candidates.synonyms)).containsExactly("APP_B.TAX")
        val md = metadata(synonyms = arrayOf(privateTax))
        val typed = snapshot.narrowCall(call(md, "tax", "rate"), listOf(ProjectArgumentType.builtIn("NUMBER")), md, appB)
        assertThat(typed.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
    }

    @Test
    fun theCompilationSchemaComesFirstThenItsPrivateSynonymThenAPublicOne() {
        val publicTax = synonym("PUBLIC", "TAX", "OTHER_C", "PK_TAX")
        // A private synonym comes before a public synonym of the same name.
        assertThat(owner(call(metadata(synonyms = arrayOf(privateTax, publicTax)), "tax", "rate"))).isEqualTo("core_a.pk_tax")
        assertThat(owner(call(metadata(synonyms = arrayOf(publicTax)), "tax", "rate"))).isEqualTo("other_c.pk_tax")
        // A package of the compilation schema comes before a public synonym, in the project or only in the metadata.
        val local = ProjectSnapshots.prepare(
            snapshotSources() + ProjectSourceInput.ofText("local.sql", "create package app_b.tax as\n  function rate(p number) return number;\nend;\n/\n"), false
        )
        val inProject = local.preferring(appB, metadata(synonyms = arrayOf(publicTax))).resolveCall(
            ProjectCallTarget.written(ProjectQualifiedName.of("tax", "rate")), ProjectCallKind.FUNCTION, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        assertThat(owner(inProject)).isEqualTo("app_b.tax")
        assertThat(inProject.synonyms).isEmpty()
        val inMetadata = call(metadata(synonyms = arrayOf(publicTax)) { schemaObject(ProjectExternalObject.of("APP_B", "TAX", ProjectExternalObject.Kind.PACKAGE)) }, "tax", "rate")
        assertThat(inMetadata.synonyms).isEmpty()
        assertThat(inMetadata.status).isNotEqualTo(ProjectCallResolution.Status.RESOLVED)
        // Without the compilation schema's object names a public synonym is not followed.
        val open = call(metadata(complete = false, synonyms = arrayOf(publicTax)), "tax", "rate")
        assertThat(open.synonyms).isEmpty()
        assertThat(open.status).isEqualTo(ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun standaloneSubprogramsAndSynonymsOfSynonyms() {
        val calc = call(metadata(synonyms = arrayOf(synonym("APP_B", "CALC", "CORE_A", "FN_CALC"))), "calc")
        assertThat(calc.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(calc.selected.get().declaration.qualifiedName).isEqualTo(ProjectQualifiedName.of("core_a", "fn_calc"))
        val twice = call(metadata(synonyms = arrayOf(synonym("APP_B", "TAX2", "APP_B", "TAX0"), synonym("APP_B", "TAX0", "CORE_A", "PK_TAX"))), "tax2", "rate")
        assertThat(owner(twice)).isEqualTo("core_a.pk_tax")
        assertThat(chain(twice.synonyms)).containsExactly("APP_B.TAX2", "APP_B.TAX0")
    }

    @Test
    fun aSynonymForAnObjectTheProjectDoesNotHoldIsNotAMissingName() {
        // Oracle runs SYS.PK_OUT; its source is not supplied.
        val external = call(metadata(synonyms = arrayOf(synonym("PUBLIC", "PK_OUT", "SYS", "PK_OUT"))), "pk_out", "put_line")
        assertThat(external.status).isEqualTo(ProjectCallResolution.Status.SYNONYM_TARGET_NOT_IN_PROJECT)
        assertThat(external.synonymTarget).contains(ProjectQualifiedName.of("SYS", "PK_OUT"))
        assertThat(external.candidates).isEmpty()
        // A synonym for a table: Oracle rejects tab.rate(1) (PLS-00224); the target is reported.
        val table = call(metadata(synonyms = arrayOf(synonym("APP_B", "TAB", "CORE_A", "T_TAB"))), "tab", "rate")
        assertThat(table.status).isEqualTo(ProjectCallResolution.Status.SYNONYM_TARGET_NOT_IN_PROJECT)
        assertThat(table.synonymTarget).contains(ProjectQualifiedName.of("CORE_A", "T_TAB"))
        // Over a database link, or resolving to itself, the synonyms lead to no object.
        val remote = call(metadata(synonyms = arrayOf(synonym("APP_B", "REMOTE_TAX", null, "PK_TAX", "NOWHERE_LINK"))), "remote_tax", "rate")
        assertThat(remote.status).isEqualTo(ProjectCallResolution.Status.SYNONYM_TARGET_NOT_IN_PROJECT)
        assertThat(remote.synonymTarget).isEmpty
        val loop = call(metadata(synonyms = arrayOf(synonym("APP_B", "LOOP_A", "APP_B", "LOOP_B"), synonym("APP_B", "LOOP_B", "APP_B", "LOOP_A"))), "loop_a", "rate")
        assertThat(loop.status).isEqualTo(ProjectCallResolution.Status.SYNONYM_TARGET_NOT_IN_PROJECT)
        assertThat(loop.synonyms).hasSize(2)
    }

    @Test
    fun packageValuesAndTypesFollowTheSynonymToo() {
        val view = snapshot.preferring(appB, metadata(synonyms = arrayOf(privateTax)))
        val value = view.resolvePackageValue(ProjectQualifiedName.of("tax", "g_value"), null)
        assertThat(value.status).isEqualTo(ProjectValueResolution.Status.RESOLVED)
        assertThat(chain(value.synonyms)).containsExactly("APP_B.TAX")
        val type = view.resolveNamedType(ProjectQualifiedName.of("tax", "t_rec"), null)
        assertThat(type.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(chain(type.synonyms)).containsExactly("APP_B.TAX")
    }

    @Test
    fun withoutMetadataNothingChanges() {
        val plain = snapshot.preferring(appB).resolveCall(
            ProjectCallTarget.written(ProjectQualifiedName.of("tax", "rate")), ProjectCallKind.FUNCTION, ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        assertThat(plain.synonyms).isEmpty()
        assertThat(plain.status).isEqualTo(ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.preferring(appB, ProjectExternalMetadata.none())).isNotNull
        assertThat(call(ProjectExternalMetadata.none(), "tax", "rate").status).isEqualTo(ProjectCallResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    private fun snapshotSources() = listOf(
        ProjectSourceInput.ofText(
            "core.sql",
            "create package core_a.pk_tax as\n  function rate(p number) return number;\n  g_value number;\n  type t_rec is record (a number);\nend;\n/\n"
        ),
        ProjectSourceInput.ofText("other.sql", "create package other_c.pk_tax as\n  function rate(p number) return number;\nend;\n/\n")
    )
}
