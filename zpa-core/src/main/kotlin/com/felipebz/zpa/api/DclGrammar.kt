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
package com.felipebz.zpa.api

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder
import com.felipebz.zpa.api.PlSqlGrammar.IDENTIFIER_NAME
import com.felipebz.zpa.api.PlSqlGrammar.UNIT_NAME
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*

enum class DclGrammar : GrammarRuleKey {

    IDENTIFIER_OR_KEYWORD,
    GRANT_STATEMENT,
    PRIVILEGE_PART,
    PRIVILEGE_COLUMNS,
    GRANT_SYSTEM_PRIVILEGES,
    GRANT_OBJECT_PRIVILEGES,
    GRANT_ROLES_TO_PROGRAMS,
    REVOKE_STATEMENT,
    REVOKE_SYSTEM_PRIVILEGES,
    REVOKE_SCHEMA_PRIVILEGES,
    REVOKE_OBJECT_PRIVILEGES,
    REVOKE_ON_OBJECT_CLAUSE,
    REVOKE_ROLES_FROM_PROGRAMS,
    DCL_COMMAND;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createDclCommands(b)
        }

        private fun createDclCommands(b: PlSqlGrammarBuilder) {
            val keywords = PlSqlKeyword.entries
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

            createRevoke(b)

            b.rule(DCL_COMMAND).define(b.firstOf(GRANT_STATEMENT, REVOKE_STATEMENT))
        }

        private fun createRevoke(b: PlSqlGrammarBuilder) {
            val privilegeItem = b.oneOrMore(b.nextNot(b.firstOf(FROM, ON)), IDENTIFIER_OR_KEYWORD)
            fun privilegeList(item: Any) = b.sequence(item, b.zeroOrMore(COMMA, item))
            val revokees = b.sequence(
                FROM, IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD))
            val schemaObjectName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))

            b.rule(REVOKE_SYSTEM_PRIVILEGES).define(privilegeList(privilegeItem), revokees)

            b.rule(REVOKE_SCHEMA_PRIVILEGES).define(
                privilegeList(b.firstOf(b.sequence(ALL, PRIVILEGES), b.sequence(b.nextNot(ALL), privilegeItem))),
                ON, SCHEMA, IDENTIFIER_NAME,
                revokees)

            b.rule(REVOKE_OBJECT_PRIVILEGES).define(privilegeList(privilegeItem), REVOKE_ON_OBJECT_CLAUSE, revokees)

            // ON SCHEMA always starts the schema branch in Oracle 26 (ORA-00987 for `ON SCHEMA FROM`), and an
            // unquoted USER is never an object or schema name here (ORA-00903/ORA-00990).
            b.rule(REVOKE_ON_OBJECT_CLAUSE).define(
                ON,
                b.firstOf(
                    b.sequence(USER, IDENTIFIER_NAME),
                    b.sequence(DIRECTORY, schemaObjectName),
                    b.sequence(EDITION, IDENTIFIER_NAME),
                    b.sequence(MINING, MODEL, schemaObjectName),
                    b.sequence(JAVA, b.firstOf(SOURCE, RESOURCE), schemaObjectName),
                    b.sequence(SQL, TRANSLATION, PROFILE, schemaObjectName),
                    b.sequence(
                        b.nextNot(b.firstOf(SCHEMA, USER)), IDENTIFIER_NAME,
                        b.optional(DOT, b.nextNot(USER), IDENTIFIER_NAME))))

            val programUnit = b.sequence(b.firstOf(FUNCTION, PROCEDURE, PACKAGE), schemaObjectName)
            b.rule(REVOKE_ROLES_FROM_PROGRAMS).define(
                IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, IDENTIFIER_OR_KEYWORD),
                FROM, programUnit, b.zeroOrMore(COMMA, programUnit))

            // Oracle 26 accepts CASCADE CONSTRAINTS/FORCE after system and schema privileges too.
            b.rule(REVOKE_STATEMENT).define(
                REVOKE,
                b.firstOf(
                    REVOKE_ROLES_FROM_PROGRAMS,
                    b.sequence(
                        b.firstOf(REVOKE_SCHEMA_PRIVILEGES, REVOKE_OBJECT_PRIVILEGES, REVOKE_SYSTEM_PRIVILEGES),
                        b.optional(b.firstOf(b.sequence(CASCADE, CONSTRAINTS), FORCE)),
                        b.optional(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)))),
                b.optional(SEMICOLON))
        }
    }

}
