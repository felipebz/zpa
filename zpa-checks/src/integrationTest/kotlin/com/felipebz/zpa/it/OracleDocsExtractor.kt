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
package com.felipebz.zpa.it

import oracle.dbtools.parser.Lexer
import oracle.dbtools.parser.plsql.SyntaxError
import oracle.dbtools.raptor.newscriptrunner.ScriptParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

fun main() {
    OracleDocsExtractor().extract()
}

internal fun oracleDocsCodeBlockText(element: Element): String =
    element.wholeText().replace('’', '\'')

class OracleDocsExtractor {

    fun extract() {
        // you need to get the file from https://docs.oracle.com/en/database/oracle/oracle-database/26/zip/oracle-database_26.zip
        val outputDir = oracleDocsOutputDirectory()

        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val booksToExtract = setOf(
            "adjsn", // JSON Developer's Guide
            "lnpls", // PL/SQL Language Reference
            "sqlrf", // SQL Language Reference
        )

        ZipFile(System.getProperty("oracleDocs")).use { archive ->
            archive.entries().asSequence()
                .filter { !it.isDirectory }
                .map { entry ->
                    entry to entry.name.substringBeforeLast('/').substringAfterLast('/')
                }
                .filter { (_, parent) -> parent in booksToExtract }
                .forEach { (entry, parent) ->
                    archive.getInputStream(entry).use { stream ->
                        Jsoup.parse(stream, Charsets.UTF_8.name(), "").run {
                            select("pre.oac_no_warn, pre.codeblock code").forEachIndexed { index, element ->
                                var text = oracleDocsCodeBlockText(element)
                                val name = "${File(entry.name).nameWithoutExtension}-$index.sql"

                                val fileContent = extractValidStatementsFrom(text)

                                if (fileContent.isNotEmpty()) {
                                    val path = "$parent/${entry.name.substringAfter("$parent/")}"
                                    text = "-- https://docs.oracle.com/en/database/oracle/oracle-database/26/$path\n$fileContent"

                                    val pathOutput = outputDir.toPath().resolve(parent).resolve(name).toFile()
                                    pathOutput.parentFile.mkdirs()
                                    pathOutput.writeText(text, Charsets.UTF_8)
                                }
                            }
                        }
                    }
                }
        }
    }

    internal fun extractValidStatementsFrom(text: String): String {
        val alteredText =
            if (text.startsWith("PACKAGE")) {
                "CREATE $text"
            } else {
                text
            }

        val parser = ScriptParser(alteredText)
        var validText = ""
        while (true) {
            val cmd = try {
                parser.next() ?: break
            } catch (_: NullPointerException) {
                // the dbtools-common from SQLcl 25.x throws a NullPointerException when parsing invalid code
                break
            }
            val sql = cmd.sqlOrig + when (cmd.statementTerminator) {
                "/" -> "\n/"
                else -> cmd.statementTerminator
            }
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

internal fun oracleDocsOutputDirectory(
    startDirectory: Path = Paths.get(System.getProperty("user.dir")),
): File {
    var directory = startDirectory.toAbsolutePath().normalize()

    while (true) {
        if (Files.isRegularFile(directory.resolve("settings.gradle.kts")) &&
            Files.isDirectory(directory.resolve("zpa-checks"))
        ) {
            return directory
                .resolve("zpa-checks/src/integrationTest/resources/sources/oracle-database_26")
                .toFile()
        }

        directory = directory.parent
            ?: error("Could not find the ZPA repository root from $startDirectory")
    }
}
