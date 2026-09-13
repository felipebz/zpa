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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ActivatedByDefault
import com.felipebz.zpa.api.annotations.ConstantRemediation
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleInfo

@Rule(priority = Priority.MINOR, tags = [Tags.CLUMSY])
@ConstantRemediation("2min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class UnnecessaryStringLiteralConcatenationCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CONCATENATION_EXPRESSION)
    }

    override fun visitNode(node: AstNode) {
        val children = node.children
        var index = 0

        while (index + 2 < children.size) {
            val run = findLiteralRun(children, index)
            if (run != null) {
                if (run.isSizeSafe) {
                    addIssue(run.firstOperator, getLocalizedMessage())
                }
                index = run.lastChildIndex + 1
            } else {
                index++
            }
        }
    }

    private fun findLiteralRun(children: List<AstNode>, startIndex: Int): LiteralRun? {
        if (startIndex + 2 >= children.size ||
            !isEligiblePair(children[startIndex], children[startIndex + 1], children[startIndex + 2])) {
            return null
        }

        val firstOperator = children[startIndex + 1].tokens.firstOrNull() ?: return null
        val firstLiteral = directCharacterLiteral(children[startIndex]) ?: return null
        val secondLiteral = directCharacterLiteral(children[startIndex + 2]) ?: return null
        val literalTokens = mutableListOf(firstLiteral.tokens.single(), secondLiteral.tokens.single())
        val family = characterSetFamily(literalTokens.first())
        var lastChildIndex = startIndex + 2

        while (lastChildIndex + 2 < children.size &&
            isEligiblePair(children[lastChildIndex], children[lastChildIndex + 1], children[lastChildIndex + 2])) {
            val nextLiteral = directCharacterLiteral(children[lastChildIndex + 2]) ?: break
            literalTokens += nextLiteral.tokens.single()
            lastChildIndex += 2
        }

        return LiteralRun(
            firstOperator,
            literalTokens,
            lastChildIndex,
            isLiteralSizeSafe(literalTokens, family)
        )
    }

    private fun isEligiblePair(left: AstNode, operator: AstNode, right: AstNode): Boolean {
        if (operator.type !== PlSqlGrammar.CONCATENATION_OPERATOR) return false

        val leftLiteral = directCharacterLiteral(left) ?: return false
        val rightLiteral = directCharacterLiteral(right) ?: return false
        val leftToken = leftLiteral.tokens.singleOrNull() ?: return false
        val rightToken = rightLiteral.tokens.singleOrNull() ?: return false
        val operatorTokens = operator.tokens

        if (operatorTokens.isEmpty() || !tokensAreOnSameLine(leftToken, rightToken, operatorTokens)) return false
        if (hasCommentBetween(operatorTokens, rightToken)) return false
        if (CheckUtils.isEmptyString(left) || CheckUtils.isEmptyString(right)) return false
        val characterSetFamily = characterSetFamily(leftToken)
        if (characterSetFamily != characterSetFamily(rightToken)) return false

        return true
    }

    private fun directCharacterLiteral(node: AstNode): AstNode? {
        return node.takeIf { it.type === PlSqlGrammar.LITERAL }
            ?.getFirstChildOrNull(PlSqlGrammar.CHARACTER_LITERAL)
    }

    private fun tokensAreOnSameLine(left: Token, right: Token, operatorTokens: List<Token>): Boolean {
        val line = left.line
        return left.endLine == line && right.line == line && right.endLine == line &&
            operatorTokens.all { it.line == line && it.endLine == line }
    }

    private fun hasCommentBetween(operatorTokens: List<Token>, rightToken: Token): Boolean {
        return (operatorTokens + rightToken).any { token -> token.trivia.any(Trivia::isComment) }
    }

    private fun characterSetFamily(token: Token): CharacterSetFamily {
        return if (token.originalValue.startsWith("n", ignoreCase = true)) {
            CharacterSetFamily.NATIONAL
        } else {
            CharacterSetFamily.DATABASE
        }
    }

    /**
     * The byte limits below are deliberately conservative safety bounds based on Oracle's
     * documented standard limits: 4000 bytes for database-character literals and 2000 bytes for
     * national-character literals. They are not an exact model of the effective limit for every
     * Oracle configuration, because the check does not know the source/database character set or
     * settings such as MAX_STRING_SIZE. It therefore charges every source UTF-16 unit as six
     * bytes, the largest representation used by Oracle's UTF-8 family. Token spans also include
     * delimiters and escapes, so this calculated size is an upper bound used for safe suppression,
     * not the literal's exact effective byte size.
     */
    private fun isLiteralSizeSafe(literals: List<Token>, characterSetFamily: CharacterSetFamily): Boolean {
        val sourceUnits = literals.sumOf { token -> token.endColumn - token.column }
        val maximumBytes = when (characterSetFamily) {
            CharacterSetFamily.DATABASE -> MAX_STANDARD_DATABASE_LITERAL_BYTES
            CharacterSetFamily.NATIONAL -> MAX_STANDARD_NATIONAL_LITERAL_BYTES
        }
        return sourceUnits <= maximumBytes / MAX_ORACLE_UTF8_BYTES_PER_CODE_POINT
    }

    private data class LiteralRun(
        val firstOperator: Token,
        val literals: List<Token>,
        val lastChildIndex: Int,
        val isSizeSafe: Boolean
    )

    private enum class CharacterSetFamily {
        DATABASE,
        NATIONAL
    }

    private companion object {
        private const val MAX_STANDARD_DATABASE_LITERAL_BYTES = 4000
        private const val MAX_STANDARD_NATIONAL_LITERAL_BYTES = 2000
        private const val MAX_ORACLE_UTF8_BYTES_PER_CODE_POINT = 6
    }
}
