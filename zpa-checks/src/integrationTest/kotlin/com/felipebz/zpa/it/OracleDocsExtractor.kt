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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.checks.ParsingErrorCheck
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import oracle.dbtools.parser.Lexer
import oracle.dbtools.parser.plsql.SyntaxError
import oracle.dbtools.raptor.newscriptrunner.ScriptParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

fun main() {
    OracleDocsExtractor().extract()
}

internal fun oracleDocsCodeBlockText(element: Element): String =
    element.wholeText().replace('’', '\'')

internal enum class ExtractionStatus {
    SQLCL_SYNTAX_REJECTED,
    SCRIPT_PARSER_FAILURE,
    UNPARSED_REMAINDER,
}

internal data class SqlclSyntaxError(val message: String, val line: Int, val offset: Int)

internal data class CandidateParseResult(val accepted: Boolean, val message: String?, val line: Int? = null)

internal data class CandidateStatement(
    val status: ExtractionStatus,
    val text: String,
    val zpaParse: CandidateParseResult,
    val syntaxError: SqlclSyntaxError? = null,
)

internal data class BlockExtraction(
    val acceptedSql: String,
    val acceptedStatements: Int,
    val candidates: List<CandidateStatement>,
    val notSqlStatements: Int,
    val sqlLooking: Boolean,
) {
    val isMixed: Boolean get() = acceptedStatements > 0 && candidates.isNotEmpty()
}

internal data class BlockResult(
    val book: String,
    val page: String,
    val blockIndex: Int,
    val url: String,
    val extraction: BlockExtraction,
) {
    val fixtureName: String get() = "${page.substringAfterLast('/').substringBeforeLast('.')}-$blockIndex.sql"
}

internal fun interface StatementSource {
    /** Statements in order, terminator included. The iterator may throw while advancing. */
    fun statements(text: String): Iterator<String>
}

internal fun interface SyntaxGate {
    fun check(sql: String): SqlclSyntaxError?
}

private val sqlclStatementSource = StatementSource { text ->
    val parser = ScriptParser(text)
    generateSequence {
        parser.next()?.let { cmd ->
            cmd.sqlOrig + when (cmd.statementTerminator) {
                "/" -> "\n/"
                else -> cmd.statementTerminator
            }
        }
    }.iterator()
}

private val sqlclSyntaxGate = SyntaxGate { sql ->
    SyntaxError.checkSyntax(sql, arrayOf("select", "sql_statement", "sql_statements"))?.let {
        SqlclSyntaxError(it.message ?: it.javaClass.simpleName, it.line, it.offset)
    }
}

private val zpaLexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8))

// Independent of SQLcl, so a statement SQLcl cannot parse is still recognized by its first word.
private val STATEMENT_START_WORDS = setOf(
    "ADMINISTER", "ALTER", "ANALYZE", "ASSOCIATE", "AUDIT", "BEGIN", "CALL", "COMMENT", "COMMIT", "CREATE",
    "DECLARE", "DELETE", "DISASSOCIATE", "DROP", "EXPLAIN", "FLASHBACK", "FUNCTION", "GRANT", "INSERT", "LOCK",
    "MERGE", "NOAUDIT", "PACKAGE", "PROCEDURE", "PURGE", "RENAME", "REVOKE", "ROLLBACK", "SAVEPOINT", "SELECT",
    "SET", "TRUNCATE", "TYPE", "UPDATE", "VALUES", "WITH", "<<",
)

internal fun looksLikeSql(text: String, firstToken: (String) -> String? = ::zpaFirstToken): Boolean {
    val first = try {
        firstToken(text)
    } catch (_: RuntimeException) {
        null
    } ?: rawFirstWord(text)
    return first?.uppercase() in STATEMENT_START_WORDS
}

private fun zpaFirstToken(text: String): String? = zpaLexer.lex(text).firstOrNull()?.originalValue

// Used when the ZPA lexer fails: false positives only add review work, false negatives lose material.
internal fun rawFirstWord(text: String): String? {
    var rest = text.trimStart()
    while (true) {
        rest = when {
            rest.startsWith("--") -> rest.substringAfter('\n', "")
            rest.startsWith("/*") -> rest.substringAfter("*/", "")
            else -> break
        }.trimStart()
    }
    if (rest.startsWith("<<")) return "<<"
    return Regex("^[A-Za-z_][A-Za-z0-9_$#]*").find(rest)?.value
}

internal class OracleDocsBlockExtractor(
    private val statementSource: StatementSource = sqlclStatementSource,
    private val syntaxGate: SyntaxGate = sqlclSyntaxGate,
    private val hasTokens: (String) -> Boolean = { Lexer.parse(it).isNotEmpty() },
    private val sqlLooking: (String) -> Boolean = { looksLikeSql(it) },
    private val zpaParser: (String) -> CandidateParseResult = ::parseCandidate,
) {

    fun extract(text: String): BlockExtraction {
        val alteredText =
            if (text.startsWith("PACKAGE")) {
                "CREATE $text"
            } else {
                text
            }

        var acceptedSql = ""
        var accepted = 0
        var notSql = 0
        val candidates = mutableListOf<CandidateStatement>()
        var statementsSeen = 0

        var failure: RuntimeException? = null
        val statements = try {
            statementSource.statements(alteredText)
        } catch (e: RuntimeException) {
            failure = e
            null
        }
        while (statements != null) {
            val sql = try {
                if (statements.hasNext()) statements.next() else break
            } catch (e: RuntimeException) {
                failure = e
                break
            }
            statementsSeen++
            val syntaxError = syntaxGate.check(sql)
            if (syntaxError == null) {
                // ignore the command if it doesn't have any token (e.g. comment line)
                if (hasTokens(sql)) {
                    if (acceptedSql.isNotEmpty()) {
                        acceptedSql += "\n"
                    }
                    acceptedSql += sql
                    accepted++
                }
            } else if (sqlLooking(sql)) {
                candidates += CandidateStatement(ExtractionStatus.SQLCL_SYNTAX_REJECTED, sql, zpaParser(sql), syntaxError)
            } else {
                notSql++
            }
        }
        if (failure != null) {
            // The script parser may throw without reporting what it consumed, so the whole block is kept.
            candidates += CandidateStatement(
                ExtractionStatus.SCRIPT_PARSER_FAILURE,
                alteredText,
                zpaParser(alteredText),
                SqlclSyntaxError(failure.javaClass.simpleName, 0, 0),
            )
        } else if (statementsSeen == 0 && alteredText.isNotBlank() && sqlLooking(alteredText)) {
            candidates += CandidateStatement(ExtractionStatus.UNPARSED_REMAINDER, alteredText, zpaParser(alteredText))
        }
        return BlockExtraction(acceptedSql, accepted, candidates, notSql, sqlLooking(alteredText))
    }
}

class OracleDocsExtractor internal constructor(private val blockExtractor: OracleDocsBlockExtractor) {

    constructor() : this(OracleDocsBlockExtractor())

    fun extract() {
        // you need to get the file from https://docs.oracle.com/en/database/oracle/oracle-database/26/zip/oracle-database_26.zip
        val results = ZipFile(System.getProperty("oracleDocs")).use { archive ->
            archive.entries().asSequence()
                .filter { !it.isDirectory }
                .map { entry -> entry to entry.name.substringBeforeLast('/').substringAfterLast('/') }
                .filter { (_, parent) -> parent in BOOKS_TO_EXTRACT }
                .sortedBy { (entry, _) -> entry.name }
                .flatMap { (entry, parent) ->
                    archive.getInputStream(entry).use { stream ->
                        extractPage(parent, entry.name, Jsoup.parse(stream, Charsets.UTF_8.name(), "")).asSequence()
                    }
                }
                .toList()
        }
        writeFixtures(results, oracleDocsOutputDirectory())
        val auditDir = oracleDocsAuditDirectory()
        writeAudit(results, auditDir)
        println(renderConsoleSummary(oracleDocsAuditSummary(results)))
    }

    internal fun extractPage(book: String, entryName: String, page: org.jsoup.nodes.Document): List<BlockResult> {
        val pagePath = "$book/${entryName.substringAfter("$book/")}"
        return page.select("pre.oac_no_warn, pre.codeblock code").mapIndexed { index, element ->
            BlockResult(
                book,
                pagePath,
                index,
                "https://docs.oracle.com/en/database/oracle/oracle-database/26/$pagePath",
                blockExtractor.extract(oracleDocsCodeBlockText(element)),
            )
        }
    }

    internal fun writeFixtures(results: List<BlockResult>, outputDir: File) {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        results.filter { it.extraction.acceptedSql.isNotEmpty() }.forEach { result ->
            val text = "-- ${result.url}\n${result.extraction.acceptedSql}"
            val pathOutput = outputDir.toPath().resolve(result.book).resolve(result.fixtureName).toFile()
            pathOutput.parentFile.mkdirs()
            pathOutput.writeText(text, Charsets.UTF_8)
        }
    }

    internal fun writeAudit(results: List<BlockResult>, auditDir: File) {
        auditDir.mkdirs()
        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        mapper.writeValue(auditDir.resolve("summary.json"), oracleDocsAuditSummary(results))
        mapper.writeValue(auditDir.resolve("candidates.json"), oracleDocsAuditCandidates(results))
    }

    private companion object {
        val BOOKS_TO_EXTRACT = setOf(
            "adjsn", // JSON Developer's Guide
            "lnpls", // PL/SQL Language Reference
            "sqlrf", // SQL Language Reference
        )
    }
}

private fun orderedBlocks(results: List<BlockResult>) =
    results.sortedWith(compareBy({ it.book }, { it.page }, { it.blockIndex }))

internal fun oracleDocsAuditSummary(results: List<BlockResult>): Map<String, Any> {
    val blocks = orderedBlocks(results)
    val candidateBlocks = blocks.filter { it.extraction.candidates.isNotEmpty() }
    val candidates = candidateBlocks.flatMap { it.extraction.candidates }
    return linkedMapOf(
        "blocksExamined" to blocks.size,
        "blocksWithFixture" to blocks.count { it.extraction.acceptedSql.isNotEmpty() },
        "acceptedStatements" to blocks.sumOf { it.extraction.acceptedStatements },
        "candidateBlocks" to candidateBlocks.size,
        "candidateStatements" to candidates.size,
        "candidateStatusCounts" to ExtractionStatus.entries.associate { status ->
            status.name to candidates.count { it.status == status }
        },
        "zpaAcceptedCandidates" to candidates.count { it.zpaParse.accepted },
        "zpaRejectedCandidates" to candidates.count { !it.zpaParse.accepted },
        "mixedBlocks" to blocks.count { it.extraction.isMixed },
        "scriptParserFailures" to candidates.count { it.status == ExtractionStatus.SCRIPT_PARSER_FAILURE },
        "unparsedRemainders" to candidates.count { it.status == ExtractionStatus.UNPARSED_REMAINDER },
    )
}

internal fun oracleDocsAuditCandidates(results: List<BlockResult>): List<Map<String, Any?>> =
    orderedBlocks(results).flatMap { block ->
        block.extraction.candidates.map { candidate ->
            linkedMapOf(
                "book" to block.book,
                "page" to block.page,
                "blockIndex" to block.blockIndex,
                "url" to block.url,
                "mixedBlock" to block.extraction.isMixed,
                "status" to candidate.status.name,
                "sqlclError" to candidate.syntaxError?.let {
                    linkedMapOf("line" to it.line, "offset" to it.offset, "message" to it.message)
                },
                "zpaParse" to if (candidate.zpaParse.accepted) "ACCEPTED" else "REJECTED",
                "zpaError" to candidate.zpaParse.takeUnless { it.accepted }?.let {
                    linkedMapOf("line" to it.line, "message" to it.message)
                },
                "text" to candidate.text,
            )
        }
    }.sortedBy { if (it["zpaParse"] == "REJECTED") 0 else 1 }

internal fun renderConsoleSummary(summary: Map<String, Any>): String = buildString {
    appendLine("Oracle documentation extraction")
    appendLine()
    appendLine("Active fixtures:        ${summary["blocksWithFixture"]}")
    appendLine("Accepted statements:    ${summary["acceptedStatements"]}")
    appendLine()
    appendLine("SQLcl-rejected candidates: ${summary["candidateStatements"]}")
    appendLine("  ZPA accepted:            ${summary["zpaAcceptedCandidates"]}")
    appendLine("  ZPA rejected:            ${summary["zpaRejectedCandidates"]}")
    appendLine()
    appendLine("Audit:")
    appendLine("  build/oracle-docs-extraction/summary.json")
    append("  build/oracle-docs-extraction/candidates.json")
}

internal fun oracleDocsOutputDirectory(
    startDirectory: Path = Paths.get(System.getProperty("user.dir")),
): File = oracleDocsRepositoryRoot(startDirectory)
    .resolve("zpa-checks/src/integrationTest/resources/sources/oracle-database_26")
    .toFile()

internal fun oracleDocsAuditDirectory(
    startDirectory: Path = Paths.get(System.getProperty("user.dir")),
): File = oracleDocsRepositoryRoot(startDirectory)
    .resolve("zpa-checks/build/oracle-docs-extraction")
    .toFile()

private fun oracleDocsRepositoryRoot(startDirectory: Path): Path {
    var directory = startDirectory.toAbsolutePath().normalize()

    while (true) {
        if (Files.isRegularFile(directory.resolve("settings.gradle.kts")) &&
            Files.isDirectory(directory.resolve("zpa-checks"))
        ) {
            return directory
        }

        directory = directory.parent
            ?: error("Could not find the ZPA repository root from $startDirectory")
    }
}

internal fun parseCandidate(sql: String): CandidateParseResult {
    val directory = Files.createTempDirectory("zpa-docs-candidate").toFile()
    val scannerLogger = java.util.logging.Logger.getLogger(AstScanner::class.java.name)
    val previousLevel = scannerLogger.level
    scannerLogger.level = java.util.logging.Level.OFF
    try {
        val file = directory.resolve("candidate.sql")
        file.writeText(sql, Charsets.UTF_8)
        val result = AstScanner(listOf(ParsingErrorCheck()), null, false, StandardCharsets.UTF_8)
            .scanFile(InputFile(PlSqlFile.Type.MAIN, directory.toPath(), file, StandardCharsets.UTF_8))
        val issue = result.issues.firstOrNull { it.check is ParsingErrorCheck }
        return if (issue == null) {
            CandidateParseResult(true, null)
        } else {
            CandidateParseResult(false, issue.primaryLocation.message().lineSequence().firstOrNull(), issue.primaryLocation.startLine())
        }
    } finally {
        scannerLogger.level = previousLevel
        directory.deleteRecursively()
    }
}
