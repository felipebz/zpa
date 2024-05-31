/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.it

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.sonar.plsqlopen.checks.CheckList
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.squid.AstScanner
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.exists

class PlSqlRulingTest {

    private val mapper = ObjectMapper()
    private val prettyPrinter = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    }

    @Test
    fun alexandria_plsql_utils() {
        analyze("alexandria-plsql-utils")
    }

    @Test
    fun pljson() {
        analyze("pljson")
    }

    @Test
    fun utPLSQL2() {
        analyze("utPLSQL2")
    }

    @Test
    fun utPLSQL3() {
        analyze("utPLSQL3")
    }

    @Test
    fun demo0001() {
        analyze("demo0001", "Doag-Forms-extracted/demo0001/WEBUTIL_DEMO")
    }

    @Test
    fun demo0002() {
        analyze("demo0002", "Doag-Forms-extracted/demo0002/DEMO0002")
    }

    @Test
    fun demo0002_2() {
        analyze("demo0002_2", "Doag-Forms-extracted/demo0002/FRW_REF")
    }

    @Test
    fun demo0003() {
        analyze("demo0003", "Doag-Forms-extracted/demo0003/DEMO0003")
    }

    @Test
    fun demo0004() {
        analyze("demo0004", "Doag-Forms-extracted/demo0004/RELEASE_LOCKS")
    }

    @Test
    fun demo0005() {
        analyze("demo0005", "Doag-Forms-extracted/demo0005/TIMEOUTPJC_TEST")
    }

    @Test
    fun demo0006() {
        analyze("demo0006", "Doag-Forms-extracted/demo0006/TIMEOUT_SYS_CLIENT_IDL")
    }

    @Test
    fun demo0007() {
        analyze("demo0007", "Doag-Forms-extracted/demo0007/CD_DEMO_EXCEL")
    }

    @Test
    fun demo0008() {
        analyze("demo0008", "Doag-Forms-extracted/demo0008/LATENCY_TEST")
    }

    @Test
    fun demo0009() {
        analyze("demo0009", "Doag-Forms-extracted/demo0009/FORMSAPI_WIZARD_2905")
    }

    @Test
    fun demo0010() {
        analyze("demo0010", "Doag-Forms-extracted/demo0010/CHK_MYFFI_SAMPLE5")
    }

    @Test
    fun demo0011() {
        analyze("demo0011", "Doag-Forms-extracted/demo0011/WEBUTIL_DEMO")
    }

    @Test
    fun demo0012() {
        analyze("demo0012", "Doag-Forms-extracted/demo0012/PDFVIEWER")
    }

    @Test
    fun demo0013() {
        analyze("demo0013", "Doag-Forms-extracted/demo0013/COLOR_SLIDER")
    }

    @Test
    fun demo0014() {
        analyze("demo0014", "Doag-Forms-extracted/demo0014/ACCORDION")
    }

    @Test
    fun demo0014_2() {
        analyze("demo0014_2", "Doag-Forms-extracted/demo0014/ACCORDION2")
    }

    @Test
    fun demo0015() {
        analyze("demo0015", "Doag-Forms-extracted/demo0015/MODERNIZE")
    }

    @Test
    fun demo0016() {
        analyze("demo0016", "Doag-Forms-extracted/demo0016/CHK_CBOX3")
    }

    @Test
    fun demo0017() {
        analyze("demo0017", "Doag-Forms-extracted/demo0017/POC_ACCOUNT")
    }

    @Test
    fun demo0018() {
        analyze("demo0018", "Doag-Forms-extracted/demo0018/TEST")
    }

    @Test
    fun oracleDatabase19() {
        val project = "oracle-database_19"
        if (!File("src/integrationTest/resources/sources/$project").exists()) {
            OracleDocsExtractor().extract()
        }
        analyze(project)
    }

    private fun analyze(project: String, sources: String = project) {
        val extensions = "sql,typ,pkg,pkb,pks,tab,tps,tpb,pcd,fun,tgg"

        val baseDir = File("src/integrationTest/resources/sources/$sources").absoluteFile
        val baseDirPath = baseDir.toPath()

        val metadataFilePath = Paths.get(baseDir.absolutePath, "metadata.json")
        val metadata = if (metadataFilePath.exists()) FormsMetadata.loadFromFile(metadataFilePath.toString()) else null

        val checks = CheckList.checks.map { it.getDeclaredConstructor().newInstance() as PlSqlVisitor }
        val scanner = AstScanner(checks, metadata, false, StandardCharsets.UTF_8)

        val issues = baseDir
            .walkTopDown()
            .filter { it.isFile && it.extension.isNotEmpty() && extensions.contains(it.extension.lowercase(Locale.getDefault())) }
            .toList()
            .map { scanner.scanFile(InputFile(PlSqlFile.Type.MAIN, baseDirPath, it, StandardCharsets.UTF_8)) }
            .flatMap { it.issues }

        val writer = mapper.writer(prettyPrinter)
        var differences = ""
        for (check in checks) {
            val export = issues.filter { it.check == check }
                .groupBy({ (it.file as InputFile).pathRelativeToBase }, { it.primaryLocation.startLine() })
                .toSortedMap()
            val actualContent = if (export.size > 0) mapper.valueToTree<JsonNode>(export) else null

            val expectedFile = File("src/integrationTest/resources/expected/$project/${check::class.simpleName}.json")
            val expectedContent = if (expectedFile.exists()) mapper.readTree(expectedFile) else null

            if (actualContent == null && expectedFile.exists()) {
                differences += "\nExpected issues on $expectedFile were not found"
            } else if (actualContent != null && actualContent != expectedContent) {
                val actualFile = File("build/integrationTest/$project/${check::class.simpleName}.json")
                actualFile.parentFile.mkdirs()
                actualFile.writeText(writer.writeValueAsString(actualContent))

                differences += "\nIssues differences on the expected file $expectedFile (actual: $actualFile)"
            }
        }

        if (differences.isNotEmpty()) {
            fail(differences)
        }
    }

}
