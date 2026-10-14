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
package com.felipebz.zpa.oracle

import com.felipebz.zpa.PlSql
import com.felipebz.zpa.PlSqlPlugin
import com.felipebz.zpa.PlSqlRuleRepository
import com.felipebz.zpa.PlSqlSquidSensor
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.symbols.ObjectLocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder
import org.sonar.api.batch.rule.internal.NewActiveRule
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.rule.RuleKey
import org.sonar.api.utils.MessageException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * The sensor against a real Oracle database, with the Oracle JDBC driver loaded from its JAR through
 * `sonar.zpa.oracle.driverPath` (it is not on this suite's class path) and a semantic check run by the scanner.
 */
@OptIn(ZpaExperimentalApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlSqlSquidSensorOracleIT {
    private lateinit var fixture: OracleFixture
    private val baseDir: Path = Files.createTempDirectory("zpa-sensor-oracle-it")

    @BeforeAll
    fun createFixture() {
        fixture = OracleFixture.create()
    }

    @AfterAll
    fun dropFixture() {
        baseDir.toFile().deleteRecursively()
        if (::fixture.isInitialized) fixture.close()
    }

    @BeforeEach
    fun reset() = ExternalTypeProbeCheck.reset()

    private fun oracleSettings(user: String = fixture.owner.name, url: String = fixture.jdbcUrl) = mapOf(
        OracleMetadataSettings.URL_KEY to url,
        OracleMetadataSettings.USER_KEY to user,
        OracleMetadataSettings.SCHEMAS_KEY to fixture.owner.name,
        OracleMetadataSettings.RELATIONS_KEY to "${fixture.reference.name}.COUNTRIES",
        OracleMetadataSettings.COMPILATION_SCHEMA_KEY to fixture.owner.name,
        OracleMetadataSettings.DRIVER_PATH_KEY to fixture.driverJar.toString()
    )

    private fun analyze(properties: Map<String, String>, password: String? = fixture.owner.password): SensorContextTester {
        val activeRules = ActiveRulesBuilder()
            .addRule(NewActiveRule.Builder().setRuleKey(RuleKey.of(PlSqlRuleRepository.KEY, "EmptyBlock")).build())
            .addRule(NewActiveRule.Builder().setRuleKey(RuleKey.of(ExternalTypeProbeCheck.REPOSITORY, ExternalTypeProbeCheck.KEY)).build())
            .build()
        val fileLinesContextFactory = mock(FileLinesContextFactory::class.java)
        `when`(fileLinesContextFactory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        val settings = MapSettings().setProperty(PlSqlPlugin.CONCURRENT_EXECUTION_KEY, "true")
        properties.forEach { (k, v) -> settings.setProperty(k, v) }
        val sensor = PlSqlSquidSensor(
            activeRules, settings.asConfig(), mock(NoSonarFilter::class.java), fileLinesContextFactory,
            arrayOf(ExternalTypeProbeCheck.Definition()), ObjectLocator()
        )
        sensor.environment = mapOf("ZPA_ORACLE_PASSWORD" to password)::get
        val context = SensorContextTester.create(baseDir)
        for ((name, text) in fixture.sources()) {
            context.fileSystem().add(
                TestInputFileBuilder("key", name).setLanguage(PlSql.KEY).setCharset(StandardCharsets.UTF_8)
                    .setModuleBaseDir(baseDir).setContents(text).initMetadata(text).build()
            )
        }
        sensor.execute(context)
        return context
    }

    private fun probeMessages(context: SensorContextTester): Map<String, String> =
        context.allIssues().filter { it.ruleKey().repository() == ExternalTypeProbeCheck.REPOSITORY }
            .map { it.primaryLocation().message()!! }
            .associate { it.substringBefore(' ') to it.substringAfter(' ') }

    @Test
    fun theDriverIsNotOnTheClassPath() {
        assertThrows<ClassNotFoundException> { Class.forName("oracle.jdbc.OracleDriver") }
    }

    @Test
    fun checksResolveOracleTypesThroughTheScanner() {
        val before = fixture.columnQueryExecutions(fixture.owner.name)
        val context = analyze(oracleSettings())
        // One acquisition for the whole concurrent analysis: one whole-schema plus one targeted column query.
        assertThat(fixture.columnQueryExecutions(fixture.owner.name) - before).isEqualTo(OracleFixture.COLUMN_QUERIES_PER_ACQUISITION)
        assertThat(ExternalTypeProbeCheck.contexts).hasSize(1)
        assertThat(ExternalTypeProbeCheck.snapshots).hasSize(1)
        assertThat(ExternalTypeProbeCheck.snapshots.single().attemptedFileCount).isEqualTo(fixture.sources().size)

        val visible = fixture.owner.query(
            "SELECT column_name FROM user_tab_columns WHERE table_name = 'EMPLOYEES' AND column_id IS NOT NULL ORDER BY column_id"
        ) { it.getString(1) }
        assertThat(visible).containsExactly("ID", "NAME", "CODE", "SALARY", "HIRED", "NOTE")
        assertThat(fixture.owner.accepts("DECLARE r employees%ROWTYPE; BEGIN r.secret := 'x'; END;")).isFalse()
        assertThat(fixture.owner.accepts("DECLARE v emp_syn.salary%TYPE; BEGIN NULL; END;")).isTrue()

        val messages = probeMessages(context)
        assertThat(messages).containsAllEntriesOf(
            mapOf(
                "hr_api.v_salary" to "RESOLVED NUMBER(10,2)",
                "hr_api.v_name" to "RESOLVED VARCHAR2(40 CHAR)",
                "hr_api.v_hired" to "RESOLVED DATE",
                "hr_api.r_employee" to "RESOLVED (ID NUMBER(6,0), NAME VARCHAR2(40 CHAR), CODE CHAR(3 BYTE), SALARY NUMBER(10,2), HIRED DATE, NOTE CLOB)",
                "hr_api.r_view" to "RESOLVED (ID NUMBER(6,0), NAME VARCHAR2(40 CHAR), ANNUAL NUMBER)",
                // Inconclusive, never guessed: an absent column, a synonym Oracle resolves, a schema read for one relation only.
                "gaps.v_bonus" to "COLUMN_NOT_FOUND",
                "gaps.v_synonym" to "RELATION_NOT_FOUND",
                "gaps.v_city" to "INCOMPLETE",
                // An explicitly owned unit compiles in its own schema, whatever the default.
                "otherapp.billing.v_salary" to "INCOMPLETE"
            )
        )
        assertThat((1..6).map { messages["p$it.v"] }).containsOnly("RESOLVED NUMBER(6,0)")
    }

    @Test
    fun ruleFindingsAreTheSameWithAndWithoutMetadata() {
        fun findings(context: SensorContextTester) = context.allIssues()
            .filter { it.ruleKey().repository() == PlSqlRuleRepository.KEY }
            .map { "${it.ruleKey()} ${it.primaryLocation().inputComponent().key()} ${it.primaryLocation().textRange()} ${it.primaryLocation().message()}" }
            .sorted()
        val offline = findings(analyze(emptyMap()))
        assertThat(offline).isNotEmpty
        assertThat(findings(analyze(oracleSettings()))).isEqualTo(offline)
    }

    @Test
    fun failuresStopTheAnalysisWithoutSecrets() {
        val wrong = "Wrong-${fixture.owner.password}"
        val denied = assertThrows<MessageException> { analyze(oracleSettings(), password = wrong) }.message!!
        assertThat(denied).contains("acquisition failed (error 1017)")

        val unreachableUrl = fixture.jdbcUrl.replace(Regex(":\\d+/"), ":1/")
        assertThat(unreachableUrl).isNotEqualTo(fixture.jdbcUrl)
        val unreachable = assertThrows<MessageException> { analyze(oracleSettings(url = unreachableUrl)) }.message!!
        assertThat(unreachable).contains("acquisition failed (error")

        for (message in listOf(denied, unreachable)) {
            assertThat(message).doesNotContain(fixture.owner.password).doesNotContain(wrong)
                .doesNotContain(fixture.jdbcUrl).doesNotContain(unreachableUrl)
        }
        assertThat(ExternalTypeProbeCheck.contexts).isEmpty()
    }
}
