//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)
@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.matchers

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The typed parameters of a [MethodMatcher] compared with the category of an argument the project types. */
class MethodMatcherEffectiveTypeTest {
    private val spec = "CREATE PACKAGE p AS\n" +
        "  TYPE c IS RECORD (id NUMBER, name VARCHAR2(10));\n" +
        "  TYPE t IS RECORD (id NUMBER, name VARCHAR2(10), child c);\n" +
        "END p;\n"

    @Test
    fun aVariableOfAProjectRecordTypeIsARecord() {
        assertThat(matches("value", PlSqlType.RECORD)).isTrue
        assertThat(matches("value", PlSqlType.NUMERIC)).isFalse
    }

    @Test
    fun aFieldOfAProjectRecordHasTheCategoryOfItsType() {
        assertThat(matches("value.id", PlSqlType.NUMERIC)).isTrue
        assertThat(matches("value.id", PlSqlType.CHARACTER)).isFalse
        assertThat(matches("value.name", PlSqlType.CHARACTER)).isTrue
        assertThat(matches("value.child", PlSqlType.RECORD)).isTrue
    }

    @Test
    fun aMemberChainHasTheCategoryOfItsLastField() {
        assertThat(matches("value.child.name", PlSqlType.CHARACTER)).isTrue
        assertThat(matches("value.child.name", PlSqlType.RECORD)).isFalse
    }

    @Test
    fun aNameTheRecordDoesNotHaveIsUnknown() {
        assertThat(matches("value.missing", PlSqlType.NUMERIC)).isFalse
        assertThat(matches("value.missing", PlSqlType.UNKNOWN)).isTrue
    }

    @Test
    fun conditionalTextLeavesTheArgumentUnknown() {
        // The conditional text writes `value`: it may declare another.
        val use = "DECLARE\n  value p.t;\n  \$IF \$\$flag \$THEN other NUMBER; \$ELSE value NUMBER; \$END\nBEGIN\n  func(value.id);\nEND;\n"
        assertThat(matches("value.id", PlSqlType.NUMERIC, use)).isFalse
        assertThat(matches("value.id", PlSqlType.UNKNOWN, use)).isTrue
        // Text that never writes `value` leaves the argument typed.
        assertThat(matches("value.id", PlSqlType.NUMERIC, use.replace("\$ELSE value NUMBER; ", ""))).isTrue
    }

    private fun matches(argument: String, type: PlSqlType, use: String = "DECLARE\n  value p.t;\nBEGIN\n  func($argument);\nEND;\n"): Boolean {
        val sources = listOf(FileId("package.sql") to spec, FileId("use.sql") to use)
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(
            sources.map { (fileId, source) -> ProjectSource(fileId) { source } },
            concurrent = false
        ))
        val check = MatcherCheck(MethodMatcher.create().name("func").addParameter(type))
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        scanner.scanFile(FixtureFile(sources[0].first, sources[0].second))
        scanner.scanFile(FixtureFile(sources[1].first, sources[1].second), listOf(check), sources[1].first)
        return check.matched!!
    }

    private class MatcherCheck(private val matcher: MethodMatcher) : PlSqlCheck() {
        var matched: Boolean? = null

        init {
            subscribeTo(PlSqlGrammar.METHOD_CALL)
        }

        override fun visitNode(node: AstNode) {
            matched = matcher.matches(node)
        }
    }

    private class FixtureFile(private val fileId: FileId, private val source: String) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = fileId.value
        override fun path(): Path = Path.of(fileId.value)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
