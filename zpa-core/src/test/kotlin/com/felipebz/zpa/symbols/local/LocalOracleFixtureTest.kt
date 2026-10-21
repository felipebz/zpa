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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * The Oracle-classified comparison of the legacy symbol table, as executable cases: each case names a source shape run
 * on Oracle, the references it shows and the outcome the resolver gives for each. Cases outside the resolver's reach
 * (typing, overload narrowing, the Project System, relation metadata) are kept and checked like the others.
 *
 * Format of `oracle-cases.txt`, one case per paragraph:
 * - `case <name>`, `source <file>` (in `sources/`), `oracle <classification> | <Oracle result>`;
 * - `target met`, `target out-of-scope: <reason>` or `target not-comparable`;
 * - `ref <name> <context> <occurrence> = <outcome>`: the occurrence-th reference to the name in that context, resolved
 *   segment by segment;
 * - `none <name> <first line>-<last line>`: no reference to the name in those lines;
 * - `contexts <name> <line> = <context,…>`: the contexts of the references to the name on that line.
 */
class LocalOracleFixtureTest {

    private class Case(val name: String, val lines: List<String>) {
        fun value(key: String) = lines.single { it.startsWith("$key ") }.substringAfter(' ')
        val checks get() = lines.filter { it.startsWith("ref ") || it.startsWith("none ") || it.startsWith("contexts ") }
    }

    private class Analysis(val model: LocalSemanticModel, val references: List<LocalReference>)

    private val analyses = HashMap<String, Analysis>()

    private fun analysis(source: String) = analyses.getOrPut(source) {
        val text = File(DIRECTORY, "sources/$source.sql").readText()
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val model = LocalSemanticModelBuilder.build(root, FileId("$source.sql"), text)
        Analysis(model, LocalReferenceCollector.collect(root, model))
    }

    private fun cases(): List<Case> = File(DIRECTORY, "oracle-cases.txt").readText().split(Regex("\n\\s*\n"))
        .map { block -> block.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") } }
        .filter { it.isNotEmpty() }
        .map { Case(it.first().removePrefix("case "), it.drop(1)) }

    @TestFactory
    fun oracleCases(): List<DynamicTest> {
        val cases = cases()
        assertThat(cases.map { it.name }).doesNotHaveDuplicates()
        return cases.map { case -> DynamicTest.dynamicTest(case.name) { check(case) } }
    }

    private fun check(case: Case) {
        val analysis = analysis(case.value("source"))
        assertThat(case.checks).describedAs("checks of ${case.name}").isNotEmpty
        val outcomes = ArrayList<String>()
        for (check in case.checks) {
            val (head, expected) = check.split(" = ", limit = 2).let { it[0] to it.getOrNull(1) }
            // A quoted name may hold spaces.
            val words = WORDS.findAll(head).map { it.value }.toList()
            val name = OracleIdentifier.fromSource(words[1])
            when (words[0]) {
                "ref" -> {
                    val context = LocalReferenceContext.valueOf(words[2])
                    val matches = analysis.references.filter { it.name == name && it.context == context }
                    val reference = matches.getOrNull(words[3].toInt())
                    assertThat(reference).describedAs("${case.name}: $head in ${case.value("source")}").isNotNull
                    val actual = LocalResolver.resolveQualified(analysis.model, reference!!).joinToString(" / ") { render(analysis.model, it) }
                    assertThat(actual).describedAs("${case.name}: $head").isEqualTo(expected)
                    outcomes += actual
                }
                "none" -> {
                    val (first, last) = words[2].split('-').map(String::toInt)
                    assertThat(analysis.references.filter { it.name == name && it.range.startLine in first..last })
                        .describedAs("${case.name}: $head").isEmpty()
                }
                "contexts" -> {
                    val line = words[2].toInt()
                    val actual = analysis.references.filter { it.name == name && it.range.startLine == line }.map { it.context.name }
                    assertThat(actual.joinToString(",").ifEmpty { "none" }).describedAs("${case.name}: $head").isEqualTo(expected)
                }
                else -> error("unknown check $check")
            }
        }
        // A row where legacy accepts what Oracle rejects meets its target only with an error-class outcome.
        val classification = case.value("oracle").substringBefore(' ')
        if (classification == "LEGACY_ACCEPTS_ERROR" && case.value("target") == "met") {
            assertThat(outcomes).describedAs("error-class outcome of ${case.name}").anyMatch { outcome -> ERRORS.any { Regex("\\b$it\\b").containsMatchIn(outcome) } }
        }
    }

    companion object {
        private val DIRECTORY = File("src/test/resources/local-semantic")
        private val WORDS = Regex("\"[^\"]*\"|\\S+")

        private val ERRORS = listOf(
            "DeclaredLater", "WrongKind", "Duplicate", "Undeclared", "OutOfScope", "InstanceMemberInStatic", "LocalFunctionInSql"
        )

        /** An outcome with each declaration as `KIND name@line:column` of its name. */
        internal fun render(model: LocalSemanticModel, outcome: LocalResolution): String {
            fun d(id: LocalDeclarationId) = model.declaration(id).let {
                "${it.kind} ${it.name.originalSpelling}@${it.nameRange.startLine}:${it.nameRange.startColumn}"
            }
            fun ds(ids: List<LocalDeclarationId>) = ids.joinToString(", ", transform = ::d)
            return when (outcome) {
                is LocalResolution.Bound -> "Bound(${d(outcome.declaration)})"
                is LocalResolution.OverloadSet -> "OverloadSet(${ds(outcome.declarations)})"
                is LocalResolution.WrongKind -> "WrongKind(${d(outcome.declaration)})"
                is LocalResolution.DeclaredLater -> "DeclaredLater(${d(outcome.declaration)})"
                is LocalResolution.Duplicate -> "Duplicate(${ds(outcome.declarations)})"
                LocalResolution.ProvenAbsent -> "ProvenAbsent"
                is LocalResolution.Incomplete ->
                    "Incomplete${(outcome.regionReasons + outcome.lookupReasons).map { it.name }}(${render(model, outcome.provisional)})"
                is LocalResolution.Unsupported -> "Unsupported(${listOfNotNull(outcome.construct, outcome.kind, outcome.context).joinToString(" ")})"
                is LocalResolution.SqlColumnOrLocal -> "SqlColumnOrLocal(${d(outcome.declaration)})"
                LocalResolution.Undeclared -> "Undeclared"
                is LocalResolution.OutOfScope -> "OutOfScope(${d(outcome.declaration)})"
                is LocalResolution.Field ->
                    "Field(${d(outcome.recordType)}.${(model.declaration(outcome.recordType).details as LocalRecordType).fields[outcome.ordinal].name.originalSpelling})"
                is LocalResolution.CollectionMethod -> "CollectionMethod(${d(outcome.collectionType)})"
                is LocalResolution.ImplicitSelf -> "ImplicitSelf(${d(outcome.method)})"
                is LocalResolution.InstanceMemberInStatic -> "InstanceMemberInStatic(${d(outcome.declaration)})"
                LocalResolution.SqlRelation -> "SqlRelation"
                is LocalResolution.LocalFunctionInSql -> "LocalFunctionInSql(${ds(outcome.declarations)})"
                is LocalResolution.SqlBuiltinOrLocal -> "SqlBuiltinOrLocal(${ds(outcome.declarations)})"
            }
        }
    }
}
