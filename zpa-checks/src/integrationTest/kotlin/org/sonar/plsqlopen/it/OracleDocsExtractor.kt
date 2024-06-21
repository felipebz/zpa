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

import oracle.dbtools.parser.Lexer
import oracle.dbtools.parser.plsql.SyntaxError
import oracle.dbtools.raptor.newscriptrunner.ISQLCommand
import oracle.dbtools.raptor.newscriptrunner.ScriptParser
import org.jsoup.Jsoup
import java.io.File
import java.nio.file.Paths
import java.util.zip.ZipFile

fun main() {
    OracleDocsExtractor().extract()
}

class OracleDocsExtractor {

    fun extract() {
        // you need to get the file from https://docs.oracle.com/en/database/oracle/oracle-database/23/zip/oracle-database_23.zip
        val zipFile = ZipFile(System.getProperty("oracleDocs"))
        val outputDir = File("src/integrationTest/resources/sources/oracle-database_23")

        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val entries = zipFile.entries()

        val booksToExtract = listOf(
            "lnpls", // PL/SQL Language Reference
            "sqlrf", // SQL Language Reference
            )

        entries.asSequence()
            .filter { !it.isDirectory }
            .map { Pair(it, File(it.name).parentFile.name) }
            .forEach { (entry, parent) ->
                if (parent in booksToExtract) {
                    zipFile.getInputStream(entry).use { stream ->
                        Jsoup.parse(stream, Charsets.UTF_8.name(), "").run {
                            select("pre.oac_no_warn, pre.codeblock code").forEachIndexed { index, element ->
                                var text = element.text()
                                    .replace('’', '\'')

                                val name = "${File(entry.name).nameWithoutExtension}-$index.sql"

                                val fileContent = extractValidStatementsFrom(text)

                                if (fileContent.isNotEmpty()) {
                                    val path = entry.name.substring(entry.name.indexOf(parent))
                                    text = "-- https://docs.oracle.com/en/database/oracle/oracle-database/23/$path\n$fileContent"

                                    val pathOutput = Paths.get(outputDir.absolutePath, parent, name).toFile()
                                    pathOutput.parentFile.mkdirs()
                                    pathOutput.writeText(text, Charsets.UTF_8)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun extractValidStatementsFrom(text: String): String {
        val alteredText =
            if (text.startsWith("PACKAGE")) {
                "CREATE $text"
            } else {
                text
            }

        val parser = ScriptParser(alteredText)
        var cmd: ISQLCommand
        var validText = ""
        while ((parser.next().also { cmd = it }) != null) {
            val sql = cmd.sqlOrigWithTerminator
            val syntaxError = SyntaxError.checkSyntax(
                sql,
                arrayOf("select", "sql_statement", "sql_statements")
            )
            if (syntaxError == null) {
                val tokens = Lexer.parse(sql)

                // ignore the command if it doesn't have any token (e.g. comment line)
                if (tokens.isNotEmpty()) {
                    if (validText.isNotEmpty()) {
                        validText += "\n"
                    }
                    validText += sql
                }
            }
        }
        return validText
    }

}
