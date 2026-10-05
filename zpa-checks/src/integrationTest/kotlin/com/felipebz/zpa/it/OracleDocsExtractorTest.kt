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

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class OracleDocsExtractorTest {

    private val rejectNewSyntax = SyntaxGate { sql ->
        if (sql.contains("NEWSYNTAX")) SqlclSyntaxError("unsupported by SQLcl", 1, 8) else null
    }

    private val zpaRejectsNewSyntax = { sql: String ->
        if (sql.contains("NEWSYNTAX")) CandidateParseResult(false, "unexpected token", 1) else CandidateParseResult(true, null)
    }

    private fun extractor(
        gate: SyntaxGate = rejectNewSyntax,
        source: StatementSource = StatementSource(::realStatements),
        zpa: (String) -> CandidateParseResult = zpaRejectsNewSyntax,
    ) = OracleDocsBlockExtractor(source, gate, zpaParser = zpa)

    private fun realStatements(text: String): Iterator<String> {
        val parser = oracle.dbtools.raptor.newscriptrunner.ScriptParser(text)
        return generateSequence {
            parser.next()?.let { cmd ->
                cmd.sqlOrig + when (cmd.statementTerminator) {
                    "/" -> "\n/"
                    else -> cmd.statementTerminator
                }
            }
        }.iterator()
    }

    @Test
    fun emitsAcceptedStatementsLikeBefore() {
        val result = OracleDocsBlockExtractor().extract("SELECT 1 FROM dual;\nSELECT 2 FROM dual;")
        assertEquals("SELECT 1 FROM dual;\nSELECT 2 FROM dual;", result.acceptedSql)
        assertEquals(2, result.acceptedStatements)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun retainsSqlLookingStatementsTheGateRejects() {
        val result = extractor().extract("CREATE NEWSYNTAX x;")
        assertEquals("", result.acceptedSql)
        val candidate = result.candidates.single()
        assertEquals(ExtractionStatus.SQLCL_SYNTAX_REJECTED, candidate.status)
        assertEquals("CREATE NEWSYNTAX x;", candidate.text)
        assertEquals(SqlclSyntaxError("unsupported by SQLcl", 1, 8), candidate.syntaxError)
    }

    @Test
    fun splitsMixedBlocksStatementByStatement() {
        val result = extractor().extract("SELECT 1 FROM dual;\nCREATE NEWSYNTAX x;\nSELECT 2 FROM dual;")
        assertEquals("SELECT 1 FROM dual;\nSELECT 2 FROM dual;", result.acceptedSql)
        assertEquals(listOf("CREATE NEWSYNTAX x;"), result.candidates.map { it.text.trim() })
        assertTrue(result.isMixed)
    }

    @Test
    fun retainsEveryRejectedStatementOfABlock() {
        val result = extractor().extract("ALTER NEWSYNTAX a;\nDROP NEWSYNTAX b;\nGRANT NEWSYNTAX c;")
        assertEquals(3, result.candidates.size)
        assertFalse(result.isMixed)
        assertEquals("", result.acceptedSql)
    }

    @Test
    fun ignoresCommentsAndProseButCountsRejectedNonSql() {
        val comment = extractor().extract("-- only a comment")
        assertEquals("", comment.acceptedSql)
        assertTrue(comment.candidates.isEmpty())

        val output = extractor(gate = { SqlclSyntaxError("rejected", 1, 0) }).extract("ERROR at line 1:\nORA-00942: table or view does not exist")
        assertTrue(output.candidates.isEmpty())
        assertTrue(output.notSqlStatements > 0)
        assertFalse(output.sqlLooking)
    }

    @Test
    fun keepsTheBlockWhenTheScriptParserFails() {
        val failing = StatementSource {
            sequence {
                yield("SELECT 1 FROM dual;")
                yield("CREATE NEWSYNTAX x;")
                throw NullPointerException("dbtools")
            }.iterator()
        }
        val text = "SELECT 1 FROM dual;\nCREATE NEWSYNTAX x;\nbroken"
        val result = extractor(source = failing).extract(text)
        assertEquals("SELECT 1 FROM dual;", result.acceptedSql)
        assertEquals(
            listOf(ExtractionStatus.SQLCL_SYNTAX_REJECTED, ExtractionStatus.SCRIPT_PARSER_FAILURE),
            result.candidates.map { it.status })
        assertEquals(text, result.candidates.last().text)
        assertEquals("NullPointerException", result.candidates.last().syntaxError?.message)
    }

    @Test
    fun keepsSqlLookingTextWhenNoStatementIsProduced() {
        val result = extractor(source = { emptyList<String>().iterator() }).extract("CREATE WHATEVER x")
        assertEquals(ExtractionStatus.UNPARSED_REMAINDER, result.candidates.single().status)
        assertTrue(extractor(source = { emptyList<String>().iterator() }).extract("not sql at all").candidates.isEmpty())
    }

    @Test
    fun prefixesPackagesLikeBefore() {
        val seen = mutableListOf<String>()
        OracleDocsBlockExtractor(StatementSource { seen += it; emptyList<String>().iterator() }, rejectNewSyntax)
            .extract("PACKAGE p IS END;")
        assertEquals(listOf("CREATE PACKAGE p IS END;"), seen)
    }

    @Test
    fun keepsProvenanceForEveryBlockOfAPage() {
        val html = """
            <html><body>
            <pre class="oac_no_warn">SELECT 1 FROM dual;</pre>
            <p>text</p>
            <pre class="codeblock"><code>CREATE NEWSYNTAX x;</code></pre>
            </body></html>
        """.trimIndent()
        val results = OracleDocsExtractor().extractPage("sqlrf", "some/dir/sqlrf/PAGE-NAME.html", Jsoup.parse(html))
        assertEquals(listOf(0, 1), results.map { it.blockIndex })
        assertEquals("sqlrf/PAGE-NAME.html", results[0].page)
        assertEquals("https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/PAGE-NAME.html", results[1].url)
        assertEquals("PAGE-NAME-0.sql", results[0].fixtureName)
        assertEquals("SELECT 1 FROM dual;", results[0].extraction.acceptedSql)
    }

    private fun block(book: String, page: String, index: Int, extraction: BlockExtraction) =
        BlockResult(book, page, index, "https://docs/$page", extraction)

    private val zpaOk = CandidateParseResult(true, null)
    private val zpaFail = CandidateParseResult(false, "unexpected token", 3)
    private val accepted = BlockExtraction("SELECT 1 FROM dual;", 1, emptyList(), 0, true)
    private fun rejected(text: String, zpa: CandidateParseResult = zpaFail) = BlockExtraction(
        "", 0, listOf(CandidateStatement(ExtractionStatus.SQLCL_SYNTAX_REJECTED, text, zpa, SqlclSyntaxError("m", 1, 2))), 0, true)
    private val mixed = BlockExtraction(
        "SELECT 1 FROM dual;", 1,
        listOf(CandidateStatement(ExtractionStatus.SQLCL_SYNTAX_REJECTED, "ALTER NEWSYNTAX;", zpaFail)), 0, true)
    private val failure = BlockExtraction(
        "", 0,
        listOf(CandidateStatement(ExtractionStatus.SCRIPT_PARSER_FAILURE, "raw", zpaOk, SqlclSyntaxError("NullPointerException", 0, 0))),
        0, true)
    private val remainder = BlockExtraction(
        "", 0, listOf(CandidateStatement(ExtractionStatus.UNPARSED_REMAINDER, "CREATE X", zpaFail)), 0, true)

    @Test
    fun summaryCountsStatusesAndZpaBehavior() {
        val results = listOf(
            block("sqlrf", "sqlrf/B.html", 1, rejected("b1")),
            block("lnpls", "lnpls/Z.html", 0, rejected("z0", zpaOk)),
            block("sqlrf", "sqlrf/A.html", 0, accepted),
            block("sqlrf", "sqlrf/B.html", 0, accepted),
            block("sqlrf", "sqlrf/A.html", 2, mixed),
            block("sqlrf", "sqlrf/C.html", 0, failure),
            block("sqlrf", "sqlrf/D.html", 0, remainder),
        )
        val summary = oracleDocsAuditSummary(results)
        assertEquals(7, summary["blocksExamined"])
        assertEquals(3, summary["blocksWithFixture"])
        assertEquals(3, summary["acceptedStatements"])
        assertEquals(5, summary["candidateBlocks"])
        assertEquals(5, summary["candidateStatements"])
        assertEquals(
            mapOf("SQLCL_SYNTAX_REJECTED" to 3, "SCRIPT_PARSER_FAILURE" to 1, "UNPARSED_REMAINDER" to 1),
            summary["candidateStatusCounts"])
        assertEquals(2, summary["zpaAcceptedCandidates"])
        assertEquals(3, summary["zpaRejectedCandidates"])
        assertEquals(1, summary["mixedBlocks"])
        assertEquals(1, summary["scriptParserFailures"])
        assertEquals(1, summary["unparsedRemainders"])
        assertEquals(summary, oracleDocsAuditSummary(results.reversed()))
        val console = renderConsoleSummary(summary)
        assertTrue(console.contains("SQLcl-rejected candidates: 5"))
        assertTrue(console.contains("ZPA accepted:            2"))
        assertTrue(console.contains("ZPA rejected:            3"))
    }

    @Test
    fun candidateOutputIsFlatDeterministicAndRecordsBothParsers() {
        val results = listOf(
            block("sqlrf", "sqlrf/B.html", 1, rejected("b1")),
            block("lnpls", "lnpls/Z.html", 0, rejected("z0", zpaOk)),
            block("sqlrf", "sqlrf/A.html", 2, mixed),
        )
        val candidates = oracleDocsAuditCandidates(results)
        assertEquals(
            listOf("sqlrf/A.html#2", "sqlrf/B.html#1", "lnpls/Z.html#0"),
            candidates.map { "${it["page"]}#${it["blockIndex"]}" })
        assertEquals(candidates, oracleDocsAuditCandidates(results.reversed()))

        val rejectedEntry = candidates[1]
        assertEquals("REJECTED", rejectedEntry["zpaParse"])
        assertEquals(mapOf("line" to 3, "message" to "unexpected token"), rejectedEntry["zpaError"])
        assertEquals(mapOf("line" to 1, "offset" to 2, "message" to "m"), rejectedEntry["sqlclError"])
        assertEquals("https://docs/sqlrf/B.html", rejectedEntry["url"])
        assertEquals(true, candidates[0]["mixedBlock"])

        val acceptedEntry = candidates[2]
        assertEquals("ACCEPTED", acceptedEntry["zpaParse"])
        assertEquals(null, acceptedEntry["zpaError"])
    }

    @Test
    fun writesFixturesOnlyForAcceptedStatementsAndTheAuditSeparately() {
        val root = Files.createTempDirectory("zpa-docs-extractor").toFile()
        try {
            val results = listOf(block("sqlrf", "sqlrf/A.html", 3, mixed), block("sqlrf", "sqlrf/B.html", 0, rejected("x")))
            val extractor = OracleDocsExtractor()
            extractor.writeFixtures(results, root.resolve("sources"))
            extractor.writeAudit(results, root.resolve("audit"))

            assertEquals(
                "-- https://docs/sqlrf/A.html\nSELECT 1 FROM dual;",
                root.resolve("sources/sqlrf/A-3.sql").readText())
            assertEquals(listOf("A-3.sql"), root.resolve("sources/sqlrf").list()!!.toList())
            val manifest = root.resolve("audit/candidates.json").readText()
            assertTrue(manifest.contains("ALTER NEWSYNTAX;"))
            assertTrue(manifest.contains("\"blockIndex\" : 3"))
            assertTrue(root.resolve("audit/summary.json").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun retainsSqlLookingTextWhenTheZpaLexerFails() {
        val failingLexer = { _: String -> throw IllegalStateException("lexer") }
        listOf(
            "CREATE FUTURE THING x;",
            "alter future thing;",
            "BEGIN NULL; END;",
            "<<label>> BEGIN NULL; END;",
            "-- c\n/* d */ ADMINISTER KEY MANAGEMENT x;",
        ).forEach { assertTrue(looksLikeSql(it, failingLexer), it) }
        listOf("ERROR at line 1:\nsome explanatory sentence", "", "   ", "-- only a comment", "1 row selected.").forEach {
            assertFalse(looksLikeSql(it, failingLexer), it)
        }
    }

    @Test
    fun labelsOnlyStatementSourceFailuresAsScriptParserFailures() {
        val failingStart = StatementSource { throw NullPointerException("start") }
        assertEquals(
            ExtractionStatus.SCRIPT_PARSER_FAILURE,
            extractor(source = failingStart).extract("CREATE x;").candidates.single().status)

        val explodingGate = SyntaxGate { throw IllegalStateException("gate bug") }
        assertThrows(IllegalStateException::class.java) { extractor(gate = explodingGate).extract("SELECT 1 FROM dual;") }
        assertThrows(IllegalStateException::class.java) {
            OracleDocsBlockExtractor(StatementSource(::realStatements), rejectNewSyntax, { throw IllegalStateException("tokens") })
                .extract("SELECT 1 FROM dual;")
        }
        assertThrows(IllegalStateException::class.java) {
            OracleDocsBlockExtractor(StatementSource(::realStatements), { SqlclSyntaxError("m", 1, 1) }, { true }, { throw IllegalStateException("classifier") })
                .extract("CREATE x;")
        }
        assertThrows(IllegalStateException::class.java) {
            extractor(zpa = { throw IllegalStateException("zpa bug") }).extract("CREATE NEWSYNTAX x;")
        }
    }

    @Test
    fun rejectedCandidatesAreRunThroughZpaAndRecordTheResult() {
        val seen = mutableListOf<String>()
        val result = extractor(zpa = { seen += it; zpaRejectsNewSyntax(it) })
            .extract("SELECT 1 FROM dual;\nCREATE NEWSYNTAX x;")
        assertEquals(listOf("CREATE NEWSYNTAX x;"), seen)
        assertEquals(CandidateParseResult(false, "unexpected token", 1), result.candidates.single().zpaParse)

        val permissive = extractor(zpa = { CandidateParseResult(true, null) }).extract("CREATE NEWSYNTAX x;")
        assertTrue(permissive.candidates.single().zpaParse.accepted)
    }

    @Test
    fun realZpaParserBoundaryDistinguishesAcceptedFromRejected() {
        assertTrue(parseCandidate("SELECT 1 FROM dual;").accepted)
        val rejected = parseCandidate("SELECT FROM WHERE ;")
        assertFalse(rejected.accepted)
        assertEquals(1, rejected.line)
        assertTrue(rejected.message!!.isNotBlank())
    }
}
