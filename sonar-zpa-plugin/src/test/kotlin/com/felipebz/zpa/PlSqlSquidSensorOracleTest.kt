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
package com.felipebz.zpa

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.oracle.*
import com.felipebz.zpa.symbols.ObjectLocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
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
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.sql.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarOutputStream
import java.util.logging.Logger

/** A driver whose connections fail with a message carrying the URL and the password, as some drivers do. */
class FailingConnectTestDriver : TestDriverBase() {
    override fun connect(url: String, info: Properties): Connection? {
        if (!acceptsURL(url)) return null
        throw SQLException("Listener refused $url for ${info["user"]} with password ${info["password"]}", "08006", 12514)
    }

    override fun acceptsURL(url: String) = url.startsWith("jdbc:zpafail:")
}

/** A driver that fails while checking its own URLs, with the URL in the message. */
class ThrowingLookupTestDriver : TestDriverBase() {
    override fun connect(url: String, info: Properties): Connection? = null
    override fun acceptsURL(url: String): Boolean {
        if (url.startsWith("jdbc:zpathrow:")) throw SQLException("Malformed URL $url", "08001", 17067)
        return false
    }
}

abstract class TestDriverBase : Driver {
    override fun getPropertyInfo(url: String?, info: Properties?): Array<DriverPropertyInfo> = emptyArray()
    override fun getMajorVersion() = 1
    override fun getMinorVersion() = 0
    override fun jdbcCompliant() = false
    override fun getParentLogger(): Logger = throw SQLFeatureNotSupportedException()
}

@OptIn(ZpaExperimentalApi::class)
class PlSqlSquidSensorOracleTest {
    @TempDir
    lateinit var baseDir: Path

    private val secret = "s3cr3t-Pa55"
    private val environment = mapOf("ZPA_ORACLE_PASSWORD" to secret)

    private val metadata = ProjectExternalMetadata.builder("test fixture").completeSchema("HR").relation(
        ProjectExternalRelation.of(
            "HR", "EMPLOYEES", true, listOf(
                ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 6, 0)),
                ProjectExternalColumn.of("NAME", 2, ProjectExternalDatatype.of("VARCHAR2", null, 40, ProjectExternalDatatype.LengthSemantics.CHAR, null, null)),
                ProjectExternalColumn.of("SALARY", 3, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))
            )
        )
    ).build()

    /** Counts acquisitions; the settings it received are kept for inspection. */
    private inner class StubAcquirer(private val result: ProjectExternalMetadata = metadata) : OracleMetadataAcquirer {
        val calls = AtomicInteger()
        var settings: OracleMetadataSettings? = null
        override fun acquire(settings: OracleMetadataSettings): AcquiredOracleMetadata {
            calls.incrementAndGet()
            this.settings = settings
            return AcquiredOracleMetadata(result, "stub")
        }
    }

    private val forbidden = OracleMetadataAcquirer { throw AssertionError("no database access expected") }

    @BeforeEach
    fun reset() = ExternalTypeProbeCheck.reset()

    @AfterEach
    fun cleanup() = ExternalTypeProbeCheck.reset()

    private fun sensor(properties: Map<String, String>, acquirer: OracleMetadataAcquirer, concurrent: Boolean = true): PlSqlSquidSensor {
        val activeRules = ActiveRulesBuilder()
            .addRule(NewActiveRule.Builder().setRuleKey(RuleKey.of(PlSqlRuleRepository.KEY, "EmptyBlock")).build())
            .addRule(NewActiveRule.Builder().setRuleKey(RuleKey.of(ExternalTypeProbeCheck.REPOSITORY, ExternalTypeProbeCheck.KEY)).build())
            .build()
        val fileLinesContextFactory = mock(FileLinesContextFactory::class.java)
        `when`(fileLinesContextFactory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        val settings = MapSettings().apply {
            setProperty(PlSqlPlugin.CONCURRENT_EXECUTION_KEY, concurrent.toString())
            properties.forEach { (k, v) -> setProperty(k, v) }
        }
        return PlSqlSquidSensor(
            activeRules, settings.asConfig(), mock(NoSonarFilter::class.java), fileLinesContextFactory,
            arrayOf(ExternalTypeProbeCheck.Definition()), ObjectLocator()
        ).also {
            it.environment = environment::get
            it.oracleMetadataAcquirer = acquirer
        }
    }

    private fun context(vararg files: Pair<String, String>): SensorContextTester {
        val context = SensorContextTester.create(baseDir)
        for ((name, text) in files) {
            context.fileSystem().add(
                TestInputFileBuilder("key", name).setLanguage(PlSql.KEY).setCharset(StandardCharsets.UTF_8)
                    .setModuleBaseDir(baseDir).setContents(text).initMetadata(text).build()
            )
        }
        return context
    }

    private val hrApi = """
        CREATE OR REPLACE PACKAGE hr_api AS
          v_salary employees.salary%TYPE;
          v_name employees.name%TYPE;
          r_employee employees%ROWTYPE;
          v_bonus employees.bonus%TYPE;
          v_department departments.name%TYPE;
        END hr_api;
        """.trimIndent()
    private val billing = "CREATE OR REPLACE PACKAGE otherapp.billing AS v_salary employees.salary%TYPE; END billing;"

    private val oracle = mapOf(
        OracleMetadataSettings.URL_KEY to "jdbc:oracle:thin:@//db.internal:1521/FREEPDB1",
        OracleMetadataSettings.USER_KEY to "APP",
        OracleMetadataSettings.SCHEMAS_KEY to "HR",
        OracleMetadataSettings.COMPILATION_SCHEMA_KEY to "HR"
    )

    private fun probeMessages(context: SensorContextTester): Map<String, String> =
        context.allIssues().filter { it.ruleKey().repository() == ExternalTypeProbeCheck.REPOSITORY }
            .map { it.primaryLocation().message()!! }
            .associate { it.substringBefore(' ') to it.substringAfter(' ') }

    @Test
    fun withoutOracleSettingsNothingConnectsAndNothingResolvesExternally() {
        val context = context("hr_api.pks" to hrApi)
        sensor(emptyMap(), forbidden).execute(context)
        assertThat(ExternalTypeProbeCheck.contexts).containsExactly(ProjectExternalContext.none())
        assertThat(probeMessages(context).values).containsOnly("UNAVAILABLE").hasSize(5)
    }

    @Test
    fun acquiredMetadataReachesChecksThroughTheScanner() {
        val acquirer = StubAcquirer()
        val extra = (1..8).map { "p$it.pks" to "CREATE OR REPLACE PACKAGE p$it AS v employees.id%TYPE; END p$it;" }
        val context = context("hr_api.pks" to hrApi, "billing.pks" to billing, *extra.toTypedArray())
        sensor(oracle, acquirer, concurrent = true).execute(context)

        // One acquisition for the whole concurrent analysis; every check saw that metadata and one snapshot.
        assertThat(acquirer.calls).hasValue(1)
        assertThat(acquirer.settings.toString()).doesNotContain(secret).doesNotContain("db.internal")
        val external = ExternalTypeProbeCheck.contexts.single()
        assertThat(external.metadata).isSameAs(metadata)
        assertThat(external.defaultCompilationSchema.get().originalSpelling).isEqualTo("\"HR\"")
        val snapshot = ExternalTypeProbeCheck.snapshots.single()
        assertThat(snapshot.attemptedFileCount).isEqualTo(10)

        val messages = probeMessages(context)
        assertThat(messages["hr_api.v_salary"]).isEqualTo("RESOLVED NUMBER(10,2)")
        assertThat(messages["hr_api.v_name"]).isEqualTo("RESOLVED VARCHAR2(40 CHAR)")
        assertThat(messages["hr_api.r_employee"]).isEqualTo("RESOLVED (ID NUMBER(6,0), NAME VARCHAR2(40 CHAR), SALARY NUMBER(10,2))")
        assertThat(messages["hr_api.v_bonus"]).isEqualTo("COLUMN_NOT_FOUND")
        assertThat(messages["hr_api.v_department"]).isEqualTo("RELATION_NOT_FOUND")
        // An explicitly owned unit compiles in its own schema, which the metadata does not cover.
        assertThat(messages["otherapp.billing.v_salary"]).isEqualTo("INCOMPLETE")
        assertThat((1..8).map { messages["p$it.v"] }).containsOnly("RESOLVED NUMBER(6,0)")
    }

    @Test
    fun withoutACompilationSchemaUnqualifiedUnitsKeepTheUncertainty() {
        val context = context("hr_api.pks" to hrApi, "billing.pks" to billing)
        sensor(oracle - OracleMetadataSettings.COMPILATION_SCHEMA_KEY, StubAcquirer()).execute(context)
        val messages = probeMessages(context)
        assertThat(messages["hr_api.v_salary"]).isEqualTo("SCHEMA_CONTEXT_MISSING")
        assertThat(messages["otherapp.billing.v_salary"]).isEqualTo("INCOMPLETE")
    }

    @Test
    fun existingIssuesMetricsAndHighlightingAreUnchanged() {
        val code = File("src/test/resources/com/felipebz/zpa/code.sql").readText()
        val plain = context("code.sql" to code, "hr_api.pks" to hrApi)
        sensor(emptyMap(), forbidden).execute(plain)
        val withMetadata = context("code.sql" to code, "hr_api.pks" to hrApi)
        sensor(oracle, StubAcquirer()).execute(withMetadata)

        fun rules(context: SensorContextTester) = context.allIssues()
            .filter { it.ruleKey().repository() == PlSqlRuleRepository.KEY }
            .map { listOf(it.ruleKey().toString(), it.primaryLocation().inputComponent().key(), it.primaryLocation().textRange()?.toString(), it.primaryLocation().message()) }
            .sortedBy { it.toString() }
        assertThat(rules(plain)).isNotEmpty.isEqualTo(rules(withMetadata))
        for (file in listOf("key:code.sql", "key:hr_api.pks")) {
            val metrics = { c: SensorContextTester -> c.measures(file).associate { it.metric().key() to it.value() } }
            assertThat(metrics(plain)).isNotEmpty.isEqualTo(metrics(withMetadata))
            val lines = (if (file.endsWith("code.sql")) code else hrApi).lines()
            for ((index, line) in lines.withIndex()) {
                for (offset in line.indices) {
                    assertThat(withMetadata.highlightingTypeAt(file, index + 1, offset)).isEqualTo(plain.highlightingTypeAt(file, index + 1, offset))
                }
            }
        }
    }

    private fun failure(properties: Map<String, String>, acquirer: OracleMetadataAcquirer = forbidden, env: Map<String, String> = environment): String {
        val sensor = sensor(properties, acquirer)
        sensor.environment = env::get
        return assertThrows<MessageException> { sensor.execute(context("hr_api.pks" to hrApi)) }.message!!
    }

    @Test
    fun invalidConfigurationFailsTheAnalysisBeforeAnyConnection() {
        assertThat(failure(mapOf(OracleMetadataSettings.SCHEMAS_KEY to "HR"))).contains("require sonar.zpa.oracle.url")
        assertThat(failure(oracle - OracleMetadataSettings.USER_KEY)).contains("requires sonar.zpa.oracle.user")
        assertThat(failure(oracle - OracleMetadataSettings.SCHEMAS_KEY)).contains("sonar.zpa.oracle.schemas or sonar.zpa.oracle.relations")
        assertThat(failure(oracle + (OracleMetadataSettings.RELATIONS_KEY to "COUNTRIES"))).contains("expected SCHEMA.RELATION")
        assertThat(failure(oracle + (OracleMetadataSettings.SCHEMAS_KEY to "\"hr\""))).contains("exact dictionary names")
        assertThat(failure(oracle + (OracleMetadataSettings.COMPILATION_SCHEMA_KEY to "\"hr\""))).contains("exact dictionary names")
        assertThat(failure(oracle + (OracleMetadataSettings.DRIVER_PATH_KEY to "missing/ojdbc17.jar"))).contains("does not name a JAR file")
        assertThat(failure(oracle, env = emptyMap())).contains("'ZPA_ORACLE_PASSWORD' is not set")
        assertThat(failure(oracle + (OracleMetadataSettings.PASSWORD_ENV_KEY to "MY_DB_PASSWORD"))).contains("'MY_DB_PASSWORD' is not set")
        for (url in listOf("jdbc:oracle:thin:scott/$secret@//db:1521/FREEPDB1", "jdbc:oracle:thin:@db?oracle.jdbc.password=$secret")) {
            val message = failure(oracle + (OracleMetadataSettings.URL_KEY to url))
            assertThat(message).contains("must not contain credentials").doesNotContain(secret)
        }
    }

    @Test
    fun customPasswordVariableAndTargetedRelationsAreHonoured() {
        val acquirer = StubAcquirer()
        val sensor = sensor(oracle + mapOf(OracleMetadataSettings.PASSWORD_ENV_KEY to "MY_DB_PASSWORD", OracleMetadataSettings.RELATIONS_KEY to "REF.COUNTRIES,REF.CITIES"), acquirer)
        sensor.environment = mapOf("MY_DB_PASSWORD" to "other")::get
        sensor.execute(context("hr_api.pks" to hrApi))
        val settings = acquirer.settings!!
        assertThat(settings.password).isEqualTo("other")
        assertThat(settings.schemas).containsExactly("HR")
        assertThat(settings.relations).containsExactly("REF" to "COUNTRIES", "REF" to "CITIES")
    }

    @Test
    fun missingDriverFailsWithGuidanceAndWithoutTheUrl() {
        val url = "jdbc:zpanone://db.internal:1521/SVC"
        val message = failure(oracle + (OracleMetadataSettings.URL_KEY to url), JdbcOracleMetadataAcquirer())
        assertThat(message).contains("no JDBC driver accepting sonar.zpa.oracle.url was found visible to the plugin")
            .contains("sonar.zpa.oracle.driverPath")
            // The registered provider whose class does not exist is skipped and reported, not fatal.
            .contains("1 JDBC driver provider(s) could not be loaded")
            .doesNotContain(url)

        val emptyJar = baseDir.resolve("empty.jar")
        JarOutputStream(Files.newOutputStream(emptyJar)).close()
        val withJar = failure(oracle + mapOf(OracleMetadataSettings.URL_KEY to url, OracleMetadataSettings.DRIVER_PATH_KEY to "empty.jar"), JdbcOracleMetadataAcquirer())
        assertThat(withJar).contains("was found in $emptyJar").doesNotContain(url)
    }

    /** A JAR registering a driver whose class file targets a Java version that does not exist yet. */
    private fun incompatibleDriverJar(): Path {
        val name = "com/felipebz/zpa/FutureTestDriver"
        val bytes = java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use { out ->
            out.writeInt(0xCAFEBABE.toInt()); out.writeShort(0); out.writeShort(99)
            out.writeShort(5)
            out.writeByte(1); out.writeUTF(name); out.writeByte(7); out.writeShort(1)
            out.writeByte(1); out.writeUTF("java/lang/Object"); out.writeByte(7); out.writeShort(3)
            out.writeShort(0x21); out.writeShort(2); out.writeShort(4)
            repeat(4) { out.writeShort(0) }
        }
        val jar = baseDir.resolve("future-driver.jar")
        JarOutputStream(Files.newOutputStream(jar)).use { out ->
            out.putNextEntry(java.util.jar.JarEntry("META-INF/services/java.sql.Driver"))
            out.write("com.felipebz.zpa.FutureTestDriver\n".toByteArray())
            out.putNextEntry(java.util.jar.JarEntry("$name.class"))
            out.write(bytes.toByteArray())
        }
        return jar
    }

    @Test
    fun incompatibleDriverIsSkippedAndReported() {
        val url = "jdbc:zpanone://db.internal:1521/SVC"
        val message = failure(
            oracle + mapOf(OracleMetadataSettings.URL_KEY to url, OracleMetadataSettings.DRIVER_PATH_KEY to incompatibleDriverJar().toString()),
            JdbcOracleMetadataAcquirer()
        )
        // The future class file and the missing provider class of the test class path.
        assertThat(message).contains("2 JDBC driver provider(s) could not be loaded (broken or incompatible with this Java version)")
            .doesNotContain(url)
    }

    @Test
    fun driverAndConnectionFailuresAreSanitized() {
        val lookupUrl = "jdbc:zpathrow://db.internal:1521/SVC?token=$secret"
        val lookup = failure(oracle + (OracleMetadataSettings.URL_KEY to lookupUrl), JdbcOracleMetadataAcquirer())
        assertThat(lookup).contains("JDBC driver lookup failed (error 17067)").doesNotContain(lookupUrl).doesNotContain(secret)

        val connectUrl = "jdbc:zpafail://db.internal:1521/SVC"
        val connect = failure(oracle + (OracleMetadataSettings.URL_KEY to connectUrl), JdbcOracleMetadataAcquirer())
        assertThat(connect).contains("acquisition failed (error 12514)").doesNotContain(connectUrl).doesNotContain(secret)
        assertThat(ExternalTypeProbeCheck.contexts).isEmpty()
    }
}
