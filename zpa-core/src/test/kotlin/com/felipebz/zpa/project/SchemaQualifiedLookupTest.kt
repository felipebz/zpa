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
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** A name qualified by a schema reaches the declarations whose effective schema it is. */
class SchemaQualifiedLookupTest {
    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val types = "create type t_obj as object (a number);\n/\n" +
        "create package pk as\n  type r is record (f number);\nend;\n/\n"

    /** `line:written=typeName` for each variable type of [file], and `line:name=fieldBinding` for each member. */
    private fun answers(paths: String, defaultSchema: String?, file: String, vararg sources: Pair<String, String>): List<String> {
        val context = ProjectExternalContext.of(
            ProjectExternalMetadata.none(), defaultSchema?.let(ProjectIdentifier::fromSource), ProjectSchemaPaths.parse(null, paths)
        )
        val preparation = ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), context)
            .prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter { it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION }.forEach {
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategories!!.typeName(it)}"
                }
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.parent.type == PlSqlGrammar.ASSIGNMENT_STATEMENT || it.parent.type == PlSqlGrammar.EXPRESSION }
                    .forEach { member -> member.children.drop(1).filter { it.type == PlSqlGrammar.IDENTIFIER_NAME }.forEach {
                        rows += "${it.token.line}:${it.tokenOriginalValue}=${this.context.fieldBinding(it)}"
                    } }
            }
        }
        val prepared = ProjectAnalysisContext.prepared(preparation).withExternalContext(context)
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, prepared)
            .scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun use(vararg types: String) = "create procedure use_it is\n" +
        types.joinToString("") { "  $it;\n" } + "  n number;\nbegin\n  null;\nend;\n/\n"

    @Test
    fun aMappedSchemaReachesTheUnitsOfItsFiles() {
        val use = "create procedure use_it is\n  v app.t_obj;\n  w app.pk.r;\n  n number;\nbegin\n  n := v.a;\nend;\n/\n"
        assertThat(answers("src/**=APP", null, "test/use.sql", "src/types.sql" to types, "test/use.sql" to use)).containsExactly(
            "2:app.t_obj=Project(STANDALONE_TYPE t_obj)",
            "3:app.pk.r=Project(PACKAGE_TYPE pk.r)",
            "4:number=BuiltIn(number)",
            "6:a=Field(ATTRIBUTE#0@src/types.sql:1:29)"
        )
    }

    @Test
    fun theDefaultSchemaIsTheSchemaOfAnUnmappedFile() {
        // types.sql matches no pattern: the default schema is its.
        assertThat(answers("src/**=APP", "LIB", "test/use.sql", "lib/types.sql" to types, "test/use.sql" to use("v lib.t_obj", "w app.t_obj")))
            .containsExactly("2:lib.t_obj=Project(STANDALONE_TYPE t_obj)", "3:app.t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "4:number=BuiltIn(number)")
    }

    @Test
    fun anUnknownSchemaOrAKnownOneWithoutTheNameResolvesNothing() {
        // `other` is no known schema; `app` is, but declares no t_none: as without a mapping, the project has no such name.
        val rows = answers("src/**=APP", null, "test/use.sql", "src/types.sql" to types, "test/use.sql" to use("v other.t_obj", "w app.t_none"))
        assertThat(rows).containsExactly(
            "2:other.t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "3:app.t_none=Unresolved(NOT_FOUND_IN_PROJECT)", "4:number=BuiltIn(number)"
        )
        // Without any configuration, the same answers as before for the qualified names.
        assertThat(answers("", null, "test/use.sql", "src/types.sql" to types, "test/use.sql" to use("v app.t_obj")))
            .containsExactly("2:app.t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "3:number=BuiltIn(number)")
    }

    @Test
    fun theUnitsOwnSchemaResolvesAsTheUnqualifiedName() {
        val own = "create procedure own is\n  v app.t_obj;\n  w t_obj;\n  n number;\nbegin\n  null;\nend;\n/\n"
        assertThat(answers("src/**=APP", null, "src/own.sql", "src/types.sql" to types, "src/own.sql" to own)).containsExactly(
            "2:app.t_obj=Project(STANDALONE_TYPE t_obj)", "3:t_obj=Project(STANDALONE_TYPE t_obj)", "4:number=BuiltIn(number)"
        )
    }

    @Test
    fun aProjectUnitNamedLikeTheSchemaKeepsTheSchemaOut() {
        // A package `app`, in the unit's schema or in another: Oracle may read `app.t_obj` as a member of it.
        val pkg = "create package app as\n  x number;\nend;\n/\n"
        for (where in listOf("test/app.sql", "other/app.sql")) {
            assertThat(answers("src/**=APP,test/**=TESTS,other/**=OTHER", null, "test/use.sql",
                "src/types.sql" to types, where to pkg, "test/use.sql" to use("v app.t_obj")))
                .describedAs(where).containsExactly("2:app.t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "3:number=BuiltIn(number)")
        }
    }

    @Test
    fun twoDeclarationsOfTheNameInTheSchemaAreAmbiguous() {
        assertThat(answers("src/**=APP", null, "test/use.sql",
            "src/types.sql" to types, "src/again.sql" to "create type t_obj as object (b number);\n/\n", "test/use.sql" to use("v app.t_obj")))
            .containsExactly("2:app.t_obj=Unresolved(AMBIGUOUS)", "3:number=BuiltIn(number)")
    }

    @Test
    fun schemaNamesFollowOracleIdentifierRules() {
        val use = use("v app.t_obj", "w \"App\".t_obj", "z \"APP\".t_obj")
        // Unquoted APP is the identifier APP; "App" is another.
        assertThat(answers("src/**=APP", null, "test/use.sql", "src/types.sql" to types, "test/use.sql" to use)).containsExactly(
            "2:app.t_obj=Project(STANDALONE_TYPE t_obj)", "3:\"App\".t_obj=Unresolved(NOT_FOUND_IN_PROJECT)",
            "4:\"APP\".t_obj=Project(STANDALONE_TYPE t_obj)", "5:number=BuiltIn(number)"
        )
        assertThat(answers("src/**=\"App\"", null, "test/use.sql", "src/types.sql" to types, "test/use.sql" to use)).containsExactly(
            "2:app.t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "3:\"App\".t_obj=Project(STANDALONE_TYPE t_obj)",
            "4:\"APP\".t_obj=Unresolved(NOT_FOUND_IN_PROJECT)", "5:number=BuiltIn(number)"
        )
    }

    @Test
    fun theFirstMatchingPatternGivesTheSchema() {
        val paths = ProjectSchemaPaths.parse(Path.of("/base"), "test/helper/**=HELPER,test/**=TESTS,*.sql=ROOT")
        assertThat(paths.schemaOf("/base/test/helper/a/b.sql").map { it.toString() }).hasValue("HELPER")
        assertThat(paths.schemaOf("/base/test/c.sql").map { it.toString() }).hasValue("TESTS")
        assertThat(paths.schemaOf("file:///base/x.sql").map { it.toString() }).hasValue("ROOT")
        assertThat(paths.schemaOf("/base/src/x.sql")).isEmpty
        assertThat(paths.schemaOf("/elsewhere/test/c.sql")).isEmpty
    }
}
