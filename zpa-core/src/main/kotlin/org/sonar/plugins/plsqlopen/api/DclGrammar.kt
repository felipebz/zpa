/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plugins.plsqlopen.api

import com.sonar.sslr.api.GenericTokenType
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.sslr.grammar.GrammarRuleKey
import org.sonar.sslr.grammar.LexerfulGrammarBuilder
import java.util.*

enum class DclGrammar : GrammarRuleKey {

    IDENTIFIER_OR_KEYWORD,
    GRANT_STATEMENT,
    PRIVILEGE_PART,
    PRIVILEGE_COLUMNS,
    GRANT_SYSTEM_PRIVILEGES,
    GRANT_OBJECT_PRIVILEGES,
    GRANT_ROLES_TO_PROGRAMS,
    DCL_COMMAND;

    companion object {
        fun buildOn(b: LexerfulGrammarBuilder) {
            createDclCommands(b)
        }

        private fun createDclCommands(b: LexerfulGrammarBuilder) {
            val keywords = Arrays.asList(*PlSqlKeyword.values())
            val rest = keywords.subList(1, keywords.size).toTypedArray()
            b.rule(IDENTIFIER_OR_KEYWORD).define(b.firstOf(GenericTokenType.IDENTIFIER, keywords[0], *rest))

            b.rule(PRIVILEGE_PART).define(b.nextNot(b.firstOf(COMMA, ON, TO, LPARENTHESIS)), IDENTIFIER_OR_KEYWORD)

            b.rule(PRIVILEGE_COLUMNS).define(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)

            b.rule(GRANT_SYSTEM_PRIVILEGES).define(
                    b.oneOrMore(PRIVILEGE_PART),
                    b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART)),
                    TO, IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD),
                    b.optional(IDENTIFIED, BY, b.anyToken(), b.zeroOrMore(COMMA, b.anyToken())),
                    b.optional(WITH, b.firstOf(ADMIN, DELEGATE), OPTION),
                    b.optional(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)))

            b.rule(GRANT_OBJECT_PRIVILEGES).define(
                    b.oneOrMore(PRIVILEGE_PART), b.optional(PRIVILEGE_COLUMNS),
                    b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART, b.optional(PRIVILEGE_COLUMNS))),
                    b.optional(ON, b.oneOrMore(b.anyTokenButNot(TO))),
                    TO, IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD),
                    b.optional(WITH, HIERARCHY, OPTION),
                    b.optional(WITH, GRANT, OPTION))

            b.rule(GRANT_ROLES_TO_PROGRAMS).define(
                    b.oneOrMore(PRIVILEGE_PART),
                    b.zeroOrMore(COMMA, b.oneOrMore(PRIVILEGE_PART)),
                    TO, b.firstOf(FUNCTION, PROCEDURE, PACKAGE), UNIT_NAME,
                    b.zeroOrMore(COMMA, b.firstOf(FUNCTION, PROCEDURE, PACKAGE), UNIT_NAME)
            )

            b.rule(GRANT_STATEMENT).define(GRANT, b.firstOf(GRANT_ROLES_TO_PROGRAMS, GRANT_SYSTEM_PRIVILEGES, GRANT_OBJECT_PRIVILEGES), b.optional(SEMICOLON))

            b.rule(DCL_COMMAND).define(GRANT_STATEMENT)
        }
    }

}
