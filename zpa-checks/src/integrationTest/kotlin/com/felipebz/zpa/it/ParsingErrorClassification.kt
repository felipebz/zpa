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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.felipebz.zpa.it

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.Locale

private val ALLOWED_METADATA_FIELDS = setOf("classification", "oracleError", "reason")

internal enum class ParsingErrorClassification {
    PARSER_LIMITATION,
    INVALID_SOURCE;

    companion object {
        fun fromJson(value: String, source: String): ParsingErrorClassification =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown parsing error classification '$value' in $source")
    }
}

internal data class ParsingErrorMetadata(
    val classification: ParsingErrorClassification = ParsingErrorClassification.PARSER_LIMITATION,
    val oracleError: String? = null,
    val reason: String? = null,
)

internal data class ClassifiedParsingError(
    val path: String,
    val line: Int,
    val metadata: ParsingErrorMetadata,
)

internal data class ParserCoverage(
    val coveredFiles: Int,
    val validFiles: Int,
) {
    val percentage: Double
        get() = if (validFiles == 0) 100.0 else coveredFiles.toDouble() / validFiles * 100.0
}

internal data class SummaryItem(
    val name: String,
    val files: Int,
    val parserLimitationFiles: Set<String>,
    val nonParserOnlyFiles: Set<String>,
    val nonParserErrors: List<ClassifiedParsingError> = emptyList(),
) {
    val parserLimitations: Int
        get() = parserLimitationFiles.size

    val expectedNonParser: Int
        get() = nonParserOnlyFiles.size

    val parserCoverage: ParserCoverage
        get() {
            val validFiles = (files - nonParserOnlyFiles.size).coerceAtLeast(0)
            val coveredFiles = (validFiles - parserLimitationFiles.size).coerceAtLeast(0)
            return ParserCoverage(coveredFiles, validFiles)
        }

    val status: String
        get() = if (parserLimitationFiles.isEmpty()) "✅" else "⚠️"
}

internal fun loadParsingErrorMetadata(mapper: ObjectMapper, file: File): Map<String, Map<Int, ParsingErrorMetadata>> {
    if (!file.exists()) {
        return emptyMap()
    }
    return parseParsingErrorMetadata(mapper.readTree(file), file.path)
}

internal fun parseParsingErrorMetadata(root: JsonNode?, source: String): Map<String, Map<Int, ParsingErrorMetadata>> {
    require(root != null && root.isObject) { "Parsing error metadata in $source must be a JSON object" }

    val result = sortedMapOf<String, Map<Int, ParsingErrorMetadata>>()
    root.properties().forEach { (path, pathNode) ->
        require(pathNode.isObject) {
            "Parsing error metadata for '$path' in $source must be a JSON object"
        }

        val lineMetadata = sortedMapOf<Int, ParsingErrorMetadata>()
        pathNode.properties().forEach { (lineKey, lineNode) ->
            val line = lineKey.toIntOrNull()
            require(line != null && line > 0) {
                "Parsing error metadata line '$lineKey' in $source must be a positive integer"
            }
            require(lineNode.isObject) {
                "Parsing error metadata for '$path:$lineKey' in $source must be a JSON object"
            }
            val unknownFields = lineNode.properties()
                .map { it.key }
                .filterNot { it in ALLOWED_METADATA_FIELDS }
                .sorted()
            require(unknownFields.isEmpty()) {
                "Unknown parsing error metadata fields for '$path:$lineKey' in $source: ${unknownFields.joinToString()}"
            }

            val classification = lineNode.get("classification")
                ?.takeUnless { it.isNull }
                ?.let {
                    require(it.isTextual) {
                        "Parsing error classification for '$path:$lineKey' in $source must be a string"
                    }
                    ParsingErrorClassification.fromJson(it.textValue(), source)
                }
                ?: ParsingErrorClassification.PARSER_LIMITATION

            lineMetadata[line] = ParsingErrorMetadata(
                classification = classification,
                oracleError = optionalText(lineNode, "oracleError", path, lineKey, source),
                reason = optionalText(lineNode, "reason", path, lineKey, source),
            )
        }
        result[path] = lineMetadata
    }
    return result
}

private fun optionalText(node: JsonNode, field: String, path: String, line: String, source: String): String? {
    val value = node.get(field)?.takeUnless { it.isNull } ?: return null
    require(value.isTextual) {
        "Parsing error metadata field '$field' for '$path:$line' in $source must be a string"
    }
    return value.textValue()
}

internal fun parseExpectedParsingErrors(root: JsonNode?, source: String): Map<String, Set<Int>> {
    if (root == null || root.isNull) {
        return emptyMap()
    }
    require(root.isObject) { "Parsing error expectations in $source must be a JSON object" }

    val result = sortedMapOf<String, Set<Int>>()
    root.properties().forEach { (path, pathNode) ->
        require(pathNode.isArray) {
            "Parsing error expectations for '$path' in $source must be an array"
        }
        val lines = sortedSetOf<Int>()
        pathNode.forEach { lineNode ->
            require(lineNode.isIntegralNumber && lineNode.intValue() > 0) {
                "Parsing error expectation for '$path' in $source must be a positive integer"
            }
            lines += lineNode.intValue()
        }
        result[path] = lines
    }
    return result
}

internal fun validateParsingErrorMetadata(
    expected: Map<String, Set<Int>>,
    metadata: Map<String, Map<Int, ParsingErrorMetadata>>,
): List<String> {
    val staleEntries = mutableListOf<String>()
    metadata.toSortedMap().forEach { (path, lineMetadata) ->
        lineMetadata.toSortedMap().forEach { (line, _) ->
            if (line !in (expected[path] ?: emptySet())) {
                staleEntries += "$path:$line"
            }
        }
    }
    return staleEntries
}

internal fun classifyParsingErrors(
    expected: Map<String, Set<Int>>,
    metadata: Map<String, Map<Int, ParsingErrorMetadata>>,
): List<ClassifiedParsingError> = expected.toSortedMap().flatMap { (path, lines) ->
    lines.sorted().map { line ->
        ClassifiedParsingError(path, line, metadata[path]?.get(line) ?: ParsingErrorMetadata())
    }
}

internal fun summaryItem(
    name: String,
    files: Int,
    parsingErrors: List<ClassifiedParsingError>,
): SummaryItem {
    val parserLimitations = parsingErrors
        .filter { it.metadata.classification == ParsingErrorClassification.PARSER_LIMITATION }
        .map { it.path }
        .toSet()
    val nonParserErrors = parsingErrors
        .filter { it.metadata.classification != ParsingErrorClassification.PARSER_LIMITATION }
    val nonParserOnly = nonParserErrors
        .map { it.path }
        .toSet() - parserLimitations
    return SummaryItem(name, files, parserLimitations, nonParserOnly, nonParserErrors)
}

internal fun renderProgressSummary(items: Collection<SummaryItem>): String {
    val output = StringBuilder()
    output.append("| Project | Files | Parser Limitations | Expected Non-Parser | Parser Coverage | Status |\n")
    output.append("| --- | --- | --- | --- | --- | --- |\n")

    items.sortedWith(compareByDescending<SummaryItem> { it.parserLimitations }.thenBy { it.name })
        .forEach { item ->
            val coverage = item.parserCoverage
            val percentage = String.format(Locale.ROOT, "%.2f", coverage.percentage)
            output.append(
                "| ${markdownCell(item.name)} | ${item.files} | ${item.parserLimitations} | " +
                    "${item.expectedNonParser} | ${coverage.coveredFiles}/${coverage.validFiles} " +
                    "($percentage%) | ${item.status} |\n"
            )
        }

    val auditedErrors = items.flatMap { item ->
        item.nonParserErrors.map { item.name to it }
    }.sortedWith(compareBy<Pair<String, ClassifiedParsingError>>({ it.first }, { it.second.path }, { it.second.line }))
    if (auditedErrors.isNotEmpty()) {
        output.append(
            "\n## Expected non-parser errors\n\n" +
                "| Project | File | Line | Classification | Oracle Error | Reason |\n" +
                "| --- | --- | ---: | --- | --- | --- |\n"
        )
        auditedErrors.forEach { (project, error) ->
            output.append(
                "| ${markdownCell(project)} | ${markdownCell(error.path)} | ${error.line} | " +
                    "${error.metadata.classification.name} | ${markdownCell(error.metadata.oracleError)} | " +
                    "${markdownCell(error.metadata.reason)} |\n"
            )
        }
    }
    return output.toString()
}

private fun markdownCell(value: String?): String = value
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("|", "\\|")
    .replace("\r\n", "\n")
    .replace("\r", "\n")
    .replace("\n", "<br>")
