//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.utils.log.Loggers
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.issue.IssueResolution
import org.sonar.api.rule.RuleKey

/**
 * Collects the `sonar-resolve [status] ruleKeys justification` comments of the walked file, which ask SonarQube to
 * accept the issues of those rules on the comment's first line, or to mark them as false positives (`[fp]`). Only the
 * first line of a comment is read, as for `NOSONAR`.
 */
class IssueResolutionVisitor(private val sensorContext: SensorContext, private val inputFile: InputFile) : PlSqlCheck() {

    private class Resolution(val line: Int, val status: IssueResolution.Status, val rules: List<RuleKey>, val justification: String)

    private val resolutions = ArrayList<Resolution>()

    private val logger = Loggers.getLogger(IssueResolutionVisitor::class.java)

    override fun visitComment(trivia: Trivia, content: String) {
        val text = firstLineText(trivia.token.originalValue)
        if (!text.startsWith(MARKER) || (text.length > MARKER.length && !text[MARKER.length].isWhitespace())) return
        val line = trivia.token.line
        val resolution = parse(line, text.substring(MARKER.length).trim())
        if (resolution == null) {
            logger.warn("Ignoring the malformed sonar-resolve comment at $inputFile:$line")
        } else {
            resolutions += resolution
        }
    }

    fun save() {
        for (resolution in resolutions) {
            sensorContext.newIssueResolution()
                .on(inputFile)
                .at(inputFile.selectLine(resolution.line))
                .status(resolution.status)
                .forRules(resolution.rules)
                .comment(resolution.justification)
                .save()
        }
    }

    private fun parse(line: Int, arguments: String): Resolution? {
        var rest = arguments
        var status = IssueResolution.Status.DEFAULT
        if (rest.startsWith("[")) {
            val end = rest.indexOf(']').takeIf { it > 0 } ?: return null
            status = when (rest.substring(1, end).lowercase()) {
                "accept" -> IssueResolution.Status.DEFAULT
                "fp" -> IssueResolution.Status.FALSE_POSITIVE
                else -> return null
            }
            rest = rest.substring(end + 1).trimStart()
        }
        val keysEnd = rest.indexOfFirst { it.isWhitespace() }.takeIf { it >= 0 } ?: return null
        val justification = rest.substring(keysEnd).trim().takeIf { it.isNotEmpty() } ?: return null
        val rules = rest.substring(0, keysEnd).split(',').map { key ->
            val separator = key.indexOf(':')
            if (separator <= 0 || separator == key.length - 1) return null
            RuleKey.parse(key)
        }
        return Resolution(line, status, rules, justification)
    }

    private companion object {
        const val MARKER = "sonar-resolve"

        fun firstLineText(comment: String): String {
            val firstLine = comment.lineSequence().first()
            val body = when {
                firstLine.startsWith("--") -> firstLine.substring(2)
                firstLine.startsWith("/*") -> firstLine.substring(2).let { if (comment.lines().size == 1) it.removeSuffix("*/") else it }
                else -> firstLine
            }
            return body.trimStart()
        }
    }
}
