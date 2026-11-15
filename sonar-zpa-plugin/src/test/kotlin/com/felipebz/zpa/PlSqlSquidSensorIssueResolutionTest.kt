//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.symbols.ObjectLocator
import com.felipebz.zpa.utils.log.Loggers
import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.issue.IssueResolution
import org.sonar.api.batch.sensor.issue.IssueResolution.Status.DEFAULT
import org.sonar.api.batch.sensor.issue.IssueResolution.Status.FALSE_POSITIVE
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.rule.RuleKey
import org.sonar.scanner.plugin.api.impl.config.MapSettings
import org.sonar.scanner.plugin.api.impl.rule.ActiveRulesBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import com.felipebz.zpa.utils.log.Logger as ZpaLogger

class PlSqlSquidSensorIssueResolutionTest {

    /** The warnings logged during the analysis. */
    private class RecordingLoggers : Loggers() {
        val warnings: MutableList<String> = Collections.synchronizedList(ArrayList())
        override fun getLogger(name: String): ZpaLogger = object : ZpaLogger {
            override fun trace(msg: String) = Unit
            override fun trace(msg: String, vararg args: Any?) = Unit
            override fun debug(msg: String) = Unit
            override fun debug(msg: String, vararg args: Any?) = Unit
            override fun info(msg: String) = Unit
            override fun info(msg: String, vararg args: Any?) = Unit
            override fun warn(msg: String) { warnings += msg }
            override fun warn(msg: String, vararg args: Any?) { warnings += msg }
            override fun error(msg: String) = Unit
            override fun error(msg: String, vararg args: Any?) = Unit
        }
    }

    private val loggers = RecordingLoggers()
    private val previousLoggers = Loggers.factory

    @BeforeEach
    fun recordLogs() {
        Loggers.factory = loggers
    }

    @AfterEach
    fun restoreLogs() {
        Loggers.factory = previousLoggers
    }

    private fun analyze(source: String, type: InputFile.Type = InputFile.Type.MAIN): Collection<IssueResolution> {
        val factory = mock(FileLinesContextFactory::class.java)
        `when`(factory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        val sensor = PlSqlSquidSensor(ActiveRulesBuilder().build(), MapSettings().asConfig(), mock(NoSonarFilter::class.java), factory, null, ObjectLocator())
        val context = SensorContextTester.create(File("."))
        val inputFile = TestInputFileBuilder("key", "resolved.sql")
            .setLanguage(PlSql.KEY).setType(type).setCharset(StandardCharsets.UTF_8).setContents(source).initMetadata(source).build()
        context.fileSystem().add(inputFile)
        sensor.execute(context)
        return context.allIssueResolutions()
    }

    private fun Collection<IssueResolution>.described() =
        map { listOf(it.textRange().start().line(), it.status(), it.ruleKeys(), it.comment()) }

    @Test
    fun lineAndBlockCommentsAtTheEndOfAStatement() {
        val resolutions = analyze(
            """
            BEGIN
              total := 0; -- sonar-resolve [fp] crm:DeadStore reset before the batch loop
              total := 1; /* sonar-resolve [accept] crm:DeadStore kept for the audit trigger */
            END;
            """.trimIndent()
        )
        assertThat(resolutions.described()).containsExactly(
            listOf(2, FALSE_POSITIVE, setOf(RuleKey.of("crm", "DeadStore")), "reset before the batch loop"),
            listOf(3, DEFAULT, setOf(RuleKey.of("crm", "DeadStore")), "kept for the audit trigger")
        )
        assertThat(resolutions.first().textRange()).isEqualTo(resolutions.first().inputFile().selectLine(2))
        assertThat(loggers.warnings).isEmpty()
    }

    @Test
    fun aCommentOnItsOwnLineCoversThatLineOnly() {
        val resolutions = analyze(
            """
            BEGIN
              -- sonar-resolve crm:LongLine the next statement mirrors a vendor script
              apply_discount(42);
            END;
            """.trimIndent()
        )
        assertThat(resolutions).extracting("textRange.start.line").containsExactly(2)
    }

    @ParameterizedTest
    @ValueSource(strings = ["[fp]", "[FP]", "[Fp]"])
    fun falsePositiveInAnyLetterCase(status: String) {
        val resolutions = analyze("BEGIN NULL; -- sonar-resolve $status crm:EmptyStatement placeholder for the hook\nEND;")
        assertThat(resolutions).extracting("status").containsExactly(FALSE_POSITIVE)
    }

    @ParameterizedTest
    @ValueSource(strings = ["[accept] ", "[ACCEPT] ", ""])
    fun acceptIsTheDefault(status: String) {
        val resolutions = analyze("BEGIN NULL; -- sonar-resolve ${status}crm:EmptyStatement placeholder for the hook\nEND;")
        assertThat(resolutions).extracting("status", "comment").containsExactly(tuple(DEFAULT, "placeholder for the hook"))
    }

    @Test
    fun twoRuleKeys() {
        val resolutions = analyze("BEGIN NULL; -- sonar-resolve crm:EmptyStatement,billing:NoOp intentionally idle\nEND;")
        assertThat(resolutions.single().ruleKeys()).containsExactlyInAnyOrder(RuleKey.of("crm", "EmptyStatement"), RuleKey.of("billing", "NoOp"))
    }

    @Test
    fun aTestFile() {
        val resolutions = analyze("BEGIN NULL; -- sonar-resolve [fp] crm:EmptyStatement fixture stub\nEND;", InputFile.Type.TEST)
        assertThat(resolutions.described()).containsExactly(listOf(1, FALSE_POSITIVE, setOf(RuleKey.of("crm", "EmptyStatement")), "fixture stub"))
    }

    @Test
    fun aStringLiteralIsNotAComment() {
        assertThat(analyze("BEGIN note := '-- sonar-resolve crm:DeadStore not a comment'; END;")).isEmpty()
        assertThat(loggers.warnings).isEmpty()
    }

    @Test
    fun aCommentThatMentionsTheWordLater() {
        assertThat(analyze("BEGIN NULL; -- see sonar-resolve crm:DeadStore in the guide\nEND;")).isEmpty()
        assertThat(loggers.warnings).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "-- sonar-resolve [maybe] crm:DeadStore unknown status",
            "-- sonar-resolve [fp] only a justification",
            "-- sonar-resolve",
            "-- sonar-resolve DeadStore a key without repository",
            "-- sonar-resolve crm:DeadStore,Other one key without repository",
            "-- sonar-resolve crm:DeadStore",
            "/* sonar-resolve [fp] crm:DeadStore */"
        ]
    )
    fun aBrokenCommentResolvesNothingAndWarns(comment: String) {
        assertThat(analyze("BEGIN\n  NULL; $comment\nEND;")).isEmpty()
        assertThat(loggers.warnings).singleElement().asString().contains("resolved.sql").contains(":2")
    }
}
