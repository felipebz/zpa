//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.it

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files

class ParsingErrorClassificationTest {

    private val mapper = ObjectMapper()

    @Test
    fun missingMetadataDefaultsToEmpty() {
        val directory = Files.createTempDirectory("zpa-parsing-error-metadata").toFile()
        try {
            assertTrue(loadParsingErrorMetadata(mapper, directory.resolve("missing.json")).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun readsInvalidSourceClassificationAndAuditDetails() {
        val metadata = parseParsingErrorMetadata(
            mapper.readTree(
                """
                {
                  "demo.sql": {
                    "39": {
                      "classification": "INVALID_SOURCE",
                      "oracleError": "PLS-00572",
                      "reason": "Oracle rejects this declaration."
                    }
                  }
                }
                """.trimIndent()
            ),
            "test metadata"
        )

        val entry = metadata.getValue("demo.sql").getValue(39)
        assertEquals(ParsingErrorClassification.INVALID_SOURCE, entry.classification)
        assertEquals("PLS-00572", entry.oracleError)
        assertEquals("Oracle rejects this declaration.", entry.reason)
    }

    @Test
    fun missingClassificationDefaultsToParserLimitation() {
        val expected = mapOf("demo.sql" to setOf(12))
        val classified = classifyParsingErrors(expected, emptyMap())

        assertEquals(ParsingErrorClassification.PARSER_LIMITATION, classified.single().metadata.classification)
    }

    @Test
    fun mixedClassificationsUseAffectedFilesForCoverage() {
        val expected = mapOf(
            "parser.sql" to setOf(7, 8),
            "invalid.sql" to setOf(39),
        )
        val metadata = mapOf(
            "invalid.sql" to mapOf(
                39 to ParsingErrorMetadata(ParsingErrorClassification.INVALID_SOURCE)
            )
        )
        val item = summaryItem("project", 160, classifyParsingErrors(expected, metadata))

        assertEquals(1, item.parserLimitations)
        assertEquals(1, item.expectedNonParser)
        assertEquals(ParserCoverage(158, 159), item.parserCoverage)
        assertEquals("⚠️", item.status)
    }

    @Test
    fun mixedClassificationsInOneFileRemainParserLimitations() {
        val errors = listOf(
            ClassifiedParsingError(
                "mixed.sql",
                10,
                ParsingErrorMetadata(ParsingErrorClassification.INVALID_SOURCE)
            ),
            ClassifiedParsingError(
                "mixed.sql",
                30,
                ParsingErrorMetadata(ParsingErrorClassification.PARSER_LIMITATION)
            ),
        )
        val item = summaryItem("project", 10, errors)

        assertEquals(setOf("mixed.sql"), item.parserLimitationFiles)
        assertTrue(item.nonParserOnlyFiles.isEmpty())
        assertEquals(ParserCoverage(9, 10), item.parserCoverage)
        assertEquals("⚠️", item.status)
    }

    @Test
    fun staleMetadataIsReportedWhenExpectedLineIsMissing() {
        val expected = mapOf("demo.sql" to setOf(12))
        val metadata = mapOf(
            "demo.sql" to mapOf(
                13 to ParsingErrorMetadata(ParsingErrorClassification.INVALID_SOURCE)
            )
        )

        assertEquals(listOf("demo.sql:13"), validateParsingErrorMetadata(expected, metadata))
    }

    @Test
    fun rejectsMisspelledMetadataField() {
        val exception = assertThrows<IllegalArgumentException> {
            parseParsingErrorMetadata(
                mapper.readTree(
                    """{"demo.sql":{"39":{"clasification":"INVALID_SOURCE"}}}"""
                ),
                "test metadata"
            )
        }

        assertTrue(exception.message!!.contains("clasification"))
    }

    @Test
    fun rejectsUnknownMetadataField() {
        val exception = assertThrows<IllegalArgumentException> {
            parseParsingErrorMetadata(
                mapper.readTree(
                    """{"demo.sql":{"39":{"classification":"INVALID_SOURCE","owner":"oracle"}}}"""
                ),
                "test metadata"
            )
        }

        assertTrue(exception.message!!.contains("owner"))
    }

    @Test
    fun rendersSummaryCountsCoverageAndStatus() {
        val markdown = renderProgressSummary(
            listOf(
                SummaryItem("limited", 160, setOf("parser.sql"), setOf("invalid.sql")),
                SummaryItem("clean", 10, emptySet(), emptySet()),
            )
        )

        assertTrue(markdown.startsWith("| Project | Files | Parser Limitations | Expected Non-Parser | Parser Coverage | Status |"))
        assertTrue(markdown.contains("| limited | 160 | 1 | 1 | 158/159 (99.37%) | ⚠️ |"))
        assertTrue(markdown.contains("| clean | 10 | 0 | 0 | 10/10 (100.00%) | ✅ |"))
        assertFalse(markdown.contains("❌"))
        assertFalse(markdown.contains("## Expected non-parser errors"))
    }

    @Test
    fun rendersAuditedNonParserDetailsWithMarkdownEscaping() {
        val error = ClassifiedParsingError(
            "demo.sql",
            39,
            ParsingErrorMetadata(
                classification = ParsingErrorClassification.INVALID_SOURCE,
                oracleError = "PLS|00572",
                reason = "first line | detail\nsecond line",
            )
        )
        val markdown = renderProgressSummary(
            listOf(SummaryItem("audit", 2, emptySet(), setOf("demo.sql"), listOf(error)))
        )

        assertTrue(markdown.contains("## Expected non-parser errors"))
        assertTrue(markdown.contains("| audit | demo.sql | 39 | INVALID_SOURCE | PLS\\|00572 | first line \\| detail<br>second line |"))
    }
}
