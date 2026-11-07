//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.project

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectExternalContext
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.api.project.ProjectSchemaPaths
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Bodies pair with specifications by schema, and an unqualified name prefers the unit's own schema. */
class SchemaPairingTest {
    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun context(paths: String) =
        ProjectExternalContext.of(ProjectExternalMetadata.none(), null, ProjectSchemaPaths.parse(null, paths))

    private fun prepare(paths: String, vararg sources: Pair<String, String>) =
        ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), context(paths))
            .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)

    private fun name(vararg parts: String) = QualifiedName(parts.map { OracleIdentifier.fromSource(it) })

    /** Whether the body [bodyName] in [bodyFile] pairs with a specification (the lookup every correlation reads). */
    private fun pairs(paths: String, specFile: String, spec: String, bodyFile: String, body: String, bodyName: String): Boolean {
        val index = prepare(paths, specFile to spec, bodyFile to body).index
        return index.counterparts(name(*bodyName.split('.').toTypedArray()), FileId(bodyFile)).any {
            val d = it.declaration
            d is StandaloneTypeDeclaration || d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION
        }
    }

    private val rows = listOf(
        // paths, spec name, body name, pairs
        listOf("spec/**=APP", "p", "app.p", true),
        listOf("body/**=APP", "app.p", "p", true),
        listOf("spec/**=OTHER", "p", "app.p", false),
        listOf("", "p", "app.p", false),
        listOf("", "p", "p", true)
    )

    @Test
    fun packageBodiesPairBySchema() {
        for ((paths, spec, body, expected) in rows) {
            assertThat(pairs(paths as String, "spec/s.sql", "create package $spec as\n  x number;\nend;\n/\n",
                "body/b.sql", "create package body $body as\nend;\n/\n", body as String)).describedAs("$spec / $body / $paths").isEqualTo(expected)
        }
    }

    @Test
    fun typeBodiesPairBySchema() {
        for ((paths, spec, body, expected) in rows) {
            assertThat(pairs(paths as String, "spec/s.sql", "create type $spec as object (a number, member procedure m);\n/\n",
                "body/b.sql", "create type body $body as\n  member procedure m is begin null; end;\nend;\n/\n", body as String))
                .describedAs("$spec / $body / $paths").isEqualTo(expected)
        }
    }

    @Test
    fun aBodyWhoseOwnerDiffersFromTheSpecificationsSchemaGoesToItsOwner() {
        // Both files compile in OTHER; the body's written owner sends it to APP, where no specification is.
        assertThat(pairs("**=OTHER", "spec/s.sql", "create package p as\n  x number;\nend;\n/\n",
            "body/b.sql", "create package body app.p as\nend;\n/\n", "app.p")).isFalse
    }

    @Test
    fun aPairedBodyFindsItsSubprogramSpecification() {
        val spec = "create package p as\n  procedure run;\nend;\n/\n"
        val body = "create package body app.p as\n  procedure run is begin null; end;\nend;\n/\n"
        fun found(paths: String): Int {
            val index = prepare(paths, "spec/s.sql" to spec, "body/b.sql" to body).index
            val run = index.occurrences.map { it.declaration }.filterIsInstance<PackageSubprogramDeclaration>().single { it.role == DeclarationRole.BODY }
            return index.findSpecificationOccurrences(run).size
        }
        assertThat(found("spec/**=APP")).isEqualTo(1)
        assertThat(found("spec/**=OTHER")).isZero
        assertThat(found("")).isZero
    }

    @Test
    fun schemaViewsShareTheRootsMapsAndKeepTheirAnswers() {
        val root = prepare(maps, tApp, tOther).index
        val app = root.preferring(OracleIdentifier.fromSource("app"))
        val other = root.preferring(OracleIdentifier.fromSource("other"))
        // Built once per schema, over the root's maps, not copies of them.
        assertThat(root.preferring(OracleIdentifier.fromSource("app"))).isSameAs(app)
        assertThat(app.sharesTablesWith(root)).isTrue
        assertThat(other.sharesTablesWith(root)).isTrue
        assertThat(app.preferring(null)).isSameAs(root)
        assertThat(app.occurrences).isSameAs(root.occurrences)
        val t = OracleIdentifier.fromSource("t")
        fun attributes(index: ProjectSymbolIndex) = index.findTypeOccurrences(null, t)
            .map { (it.declaration as StandaloneTypeDeclaration).attributes.single().name.toString() }
        assertThat(attributes(app)).containsExactly("a")
        assertThat(attributes(other)).containsExactly("b")
        assertThat(attributes(root)).containsExactly("a", "b")
        // A view of a view is the root's view.
        assertThat(app.preferring(OracleIdentifier.fromSource("other"))).isSameAs(other)
    }

    /** `line:written=typeName` for each variable type of [file]. */
    private fun typeNames(paths: String, file: String, vararg sources: Pair<String, String>): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter { it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategories!!.typeName(it)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, ProjectAnalysisContext.prepared(prepare(paths, *sources)))
            .scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private val tApp = "app/t.sql" to "create type t as object (a number);\n/\n"
    private val tOther = "other/t.sql" to "create type t as object (b number);\n/\n"
    private val maps = "app/**=APP,other/**=OTHER"
    private fun user(owner: String = "") = "create procedure ${owner}use_it is\n  v t;\nbegin\n  null;\nend;\n/\n"

    @Test
    fun anUnqualifiedNamePrefersTheUnitsSchema() {
        assertThat(typeNames(maps, "app/use.sql", tApp, tOther, "app/use.sql" to user()))
            .containsExactly("2:t=Project(STANDALONE_TYPE t)")
        assertThat(typeNames(maps, "other/use.sql", tApp, tOther, "other/use.sql" to user()))
            .containsExactly("2:t=Project(STANDALONE_TYPE t)")
        // The resolved declaration is the schema's own: its attribute tells.
        val index = prepare(maps, tApp, tOther).index
        for ((schema, attribute) in listOf("app" to "a", "other" to "b")) {
            val type = index.preferring(OracleIdentifier.fromSource(schema)).findTypeOccurrences(null, OracleIdentifier.fromSource("t"))
                .single().declaration as StandaloneTypeDeclaration
            assertThat(type.attributes.map { it.name.toString() }).describedAs(schema).containsExactly(attribute)
        }
    }

    @Test
    fun withoutAUnitSchemaOrWithSeveralInItTheNameIsAmbiguous() {
        assertThat(typeNames(maps, "none/use.sql", tApp, tOther, "none/use.sql" to user()))
            .containsExactly("2:t=Unresolved(AMBIGUOUS)")
        assertThat(typeNames(maps, "app/use.sql", tApp, "app/again.sql" to "create type t as object (c number);\n/\n", tOther, "app/use.sql" to user()))
            .containsExactly("2:t=Unresolved(AMBIGUOUS)")
    }

    @Test
    fun noneInTheUnitsSchemaKeepsTheProjectWideLookup() {
        assertThat(typeNames("$maps,third/**=THIRD", "third/use.sql", tApp, "third/use.sql" to user()))
            .containsExactly("2:t=Project(STANDALONE_TYPE t)")
        assertThat(typeNames("$maps,third/**=THIRD", "third/use.sql", tApp, tOther, "third/use.sql" to user()))
            .containsExactly("2:t=Unresolved(AMBIGUOUS)")
    }

    @Test
    fun aWrittenOwnerMakesTheSchemaKnownWithoutConfiguration() {
        val a = "a.sql" to "create type app.t as object (a number);\n/\n"
        val b = "b.sql" to "create type other.t as object (b number);\n/\n"
        assertThat(typeNames("", "use.sql", a, b, "use.sql" to user("app."))).containsExactly("2:t=Project(STANDALONE_TYPE app.t)")
        // From a unit with no schema, `t` is no written name of either: as before, not found.
        assertThat(typeNames("", "use.sql", a, b, "use.sql" to user())).containsExactly("2:t=Unresolved(NOT_FOUND_IN_PROJECT)")
    }

    @Test
    fun aCallReachesADeclarationOnlyThroughItsOwnSchema() {
        val index = prepare("src/**=APP", "src/pk.sql" to "create package pk as\n  procedure run;\nend;\n/\n").index
        val run = index.occurrences.filter { it.declaration is PackageSubprogramDeclaration }
        val pk = index.occurrences.filter { it.declaration is PackageDeclaration }
        fun candidates(vararg written: String) = ProjectCallCandidates(
            ProjectCallCandidates.Status.FOUND, ProjectCallTarget.written(name(*written)), run, emptySet(), pk, emptyList(), true,
            index::effectiveSchema
        )
        assertThat(candidates("app", "pk", "run").candidates).hasSize(1)
        assertThatThrownBy { candidates("other", "pk", "run") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun thePreparedResultCarriesItsSchemasToTheScan() {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("LIB"), ProjectSchemaPaths.parse(null, "src/**=APP"))
        val result = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas).prepare(emptyList(), concurrent = false)
        val prepared = ProjectAnalysisContext.prepared(result)
        assertThat(prepared.externalContext).isSameAs(schemas)
        // Metadata for the scan keeps the schemas.
        val withMetadata = prepared.withMetadata(ProjectExternalMetadata.none())
        assertThat(withMetadata.externalContext.fileSchema("src/a.sql").map { it.toString() }).hasValue("APP")
        assertThat(withMetadata.externalContext.fileSchema("x.sql").map { it.toString() }).hasValue("LIB")
    }
}
