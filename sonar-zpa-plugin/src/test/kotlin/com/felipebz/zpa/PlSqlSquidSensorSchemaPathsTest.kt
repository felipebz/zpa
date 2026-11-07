//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.symbols.ObjectLocator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
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

/** `sonar.zpa.schemaPaths` gives each file its schema; a name qualified by it reaches the file's units. */
class PlSqlSquidSensorSchemaPathsTest {
    @TempDir
    lateinit var baseDir: File

    private fun issues(vararg properties: Pair<String, String>): List<String> {
        val settings = MapSettings()
        properties.forEach { (key, value) -> settings.setProperty(key, value) }
        val factory = mock(FileLinesContextFactory::class.java)
        `when`(factory.createFor(any(InputFile::class.java))).thenReturn(mock(FileLinesContext::class.java))
        val rules = ActiveRulesBuilder().addRule(NewActiveRule.Builder().setRuleKey(RuleKey.of(PlSqlRuleRepository.KEY, "SelectAllColumns")).build()).build()
        val sensor = PlSqlSquidSensor(rules, settings.asConfig(), mock(NoSonarFilter::class.java), factory, null, ObjectLocator())
        val context = SensorContextTester.create(baseDir)
        // `v` is a collection of numbers only through schema APP: SELECT * into it is then a finding.
        for ((name, text) in listOf(
            "src/types.sql" to "create type t_nums as table of number;\n/\n",
            "test/use.sql" to "create procedure p is\n  v app.t_nums;\nbegin\n  select * bulk collect into v from dual;\nend;\n/\n"
        )) {
            context.fileSystem().add(TestInputFileBuilder("key", name).setLanguage(PlSql.KEY).setCharset(StandardCharsets.UTF_8)
                .setModuleBaseDir(baseDir.toPath()).setContents(text).initMetadata(text).build())
        }
        sensor.execute(context)
        return context.allIssues().map { "${it.primaryLocation().inputComponent()}:${it.primaryLocation().textRange()?.start()?.line()}" }
    }

    @Test
    fun theSchemaPathsReachTheProjectLookups() {
        assertThat(issues()).isEmpty()
        assertThat(issues(PlSqlPlugin.SCHEMA_PATHS_KEY to "src/**=APP")).containsExactly("test/use.sql:4")
        // The default compilation schema applies to the files no pattern matches.
        assertThat(issues("sonar.zpa.oracle.compilationSchema" to "APP")).containsExactly("test/use.sql:4")
    }

    @Test
    fun anInvalidValueFailsWithThePropertyName() {
        assertThatThrownBy { issues(PlSqlPlugin.SCHEMA_PATHS_KEY to "src/**") }
            .isInstanceOf(MessageException::class.java).hasMessageContaining(PlSqlPlugin.SCHEMA_PATHS_KEY)
    }
}
