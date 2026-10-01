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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class KeyTransferTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ADMINISTER_KEY_MANAGEMENT)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).matches("administer key management $tail")
        }
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) {
            assertThat(p).describedAs(tail).notMatches("administer key management $tail")
        }
    }

    @Test
    fun matchesExportAndImportAuthenticationBranches() {
        for ((operation, direction) in listOf("export" to "to", "import" to "from")) {
            matches(
                "$operation keys with secret secret $direction '/missing/keys' identified by pw",
                "$operation encryption keys with secret \"secret:phrase\" $direction '/missing/keys' " +
                    "force keystore identified by \"user:password\";",
                "$operation keys with secret secret $direction '/missing/keys' identified by external store",
                "$operation encryption keys with secret secret $direction '/missing/keys' " +
                    "force keystore identified by external store"
            )
        }
    }

    @Test
    fun matchesOptionalImportBackupAndNormalLiteralTokens() {
        matches(
            "import keys with secret s from '/missing/keys' identified by pw with backup",
            "import keys with secret s from '/missing/keys' force keystore identified by external store " +
                "with backup using 'import_backup'",
            "import encryption keys with secret s from '/missing/keys' identified by pw with backup using backup_name",
            "import keys with secret s from '/missing/keys' identified by pw with backup using \"backup name\""
        )
        for ((operation, direction) in listOf("export" to "to", "import" to "from")) {
            for (filename in listOf("q'[/missing/key''s]'", "N'/missing/keys'", "''")) {
                matches("$operation keys with secret s $direction $filename identified by pw")
            }
        }
    }

    @Test
    fun matchesUnparenthesizedIdentifierLists() {
        for (identifiers in listOf("'id1'", "'id1', 'id2'", "id1", "\"id1\"", "'id1', id2, \"id3\"",
            "N'id1', q'[id2]'", "''")) {
            matches("export encryption keys with secret s to '/missing/keys' force keystore " +
                "identified by external store with identifier in $identifiers")
        }
    }

    @Test
    fun matchesCompleteParenthesizedQueries() {
        for (query in listOf(
            "select key_id from v\$encryption_keys where rownum < 2",
            "with keys_to_export as (select 'id1' key_id from dual) select key_id from keys_to_export",
            "(select 'id1' from dual)",
            "select key_id from (select 'id1' key_id from dual)",
            "select 'id1' from dual union all select 'id2' from dual order by 1",
            "select key_id from v\$encryption_keys order by key_id fetch first 1 row only"
        )) {
            matches("export keys with secret s to '/missing/keys' identified by pw with identifier in ($query)")
        }
    }

    @Test
    fun rejectsMalformedSecretsFilenamesAndAuthentication() {
        for ((operation, direction) in listOf("export" to "to", "import" to "from")) {
            for (secret in listOf("'secret'", "N'secret'", "q'[secret]'", "123", ":secret", "select")) {
                notMatches("$operation keys with secret $secret $direction '/missing/keys' identified by pw")
            }
            for (filename in listOf("filename", "\"filename\"", "123", ":filename")) {
                notMatches("$operation keys with secret s $direction $filename identified by pw")
            }
            notMatches(
                "$operation keys secret s $direction '/missing/keys' identified by pw",
                "$operation with secret s $direction '/missing/keys' identified by pw",
                "$operation keys with secret $direction '/missing/keys' identified by pw",
                "$operation keys with secret s '/missing/keys' identified by pw",
                "$operation keys with secret s $direction identified by pw",
                "$operation keys with secret s $direction '/missing/keys'",
                "$operation keys with secret s $direction '/missing/keys' identified by 'pw'",
                "$operation keys with secret s $direction '/missing/keys' identified by 123",
                "$operation keys with secret s $direction '/missing/keys' identified by :pw",
                "$operation keys with secret s $direction '/missing/keys' identified by external",
                "$operation keys with secret s $direction '/missing/keys' identified by external pw",
                "$operation keys with secret s force keystore $direction '/missing/keys' identified by pw",
                "$operation keys with secret s $direction '/missing/keys' force identified by pw",
                "$operation keys with secret s $direction '/missing/keys' identified by pw force keystore",
                "$operation keys with secret s $direction '/missing/keys' identified by pw identified by pw",
                "$operation keys with secret s $direction '/missing/keys' identified by pw container = current"
            )
        }
    }

    @Test
    fun rejectsMalformedIdentifierFiltersAndOrdering() {
        val export = "export keys with secret s to '/missing/keys' identified by pw"
        for (filter in listOf(
            "", "'id1',", "'id1', 123", "123", ":id1", "()", "('id1')", "('id1', 'id2')",
            "select 'id1' from dual", "(select from dual)", "(garbage)", "(select 'id1' from dual",
            "'id1', (select 'id2' from dual)", "(select 'id1' from dual), 'id2'"
        )) {
            notMatches("$export with identifier in $filter")
        }
        notMatches(
            "$export with identifier 'id1'",
            "$export with in 'id1'",
            "$export with garbage in 'id1'",
            "$export with identifier in 'id1' with identifier in 'id2'",
            "$export with backup",
            "$export with identifier in 'id1' with backup",
            "export keys with secret s to '/missing/keys' with identifier in 'id1' identified by pw",
            "import keys with secret s from '/missing/keys' identified by pw with identifier in 'id1'",
            "import keys with secret s from '/missing/keys' with backup identified by pw",
            "import keys with secret s from '/missing/keys' identified by pw with backup using",
            "import keys with secret s from '/missing/keys' identified by pw with backup using 123",
            "import keys with secret s from '/missing/keys' identified by pw with backup with backup"
        )
    }

    @Test
    fun preservesNewNonReservedKeywordsAsNamesAndQuotedBackupIdentifiers() {
        for (word in listOf("export", "import", "migrate", "identifier")) {
            matches(
                "export keys with secret $word to '/missing/keys' identified by $word with identifier in $word",
                "set key using tag $word identified by $word with backup using \"$word\"",
                "set key identified by $word migrate using $word"
            )
            notMatches("import keys with secret s from '/missing/keys' identified by pw with backup using $word")
        }
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches("create table export (import number, migrate number, identifier number); " +
            "insert into export (import, migrate, identifier) values (1, 2, 3); " +
            "select import, migrate, identifier from export;")
    }

    @Test
    fun preservesVisibleTransfersAndQueryStructure() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("administer key management export keys with secret s to '/missing/keys' " +
            "identified by pw with identifier in (with k as (select 'id1' key_id from dual) " +
            "select key_id from k where key_id in (select 'id1' from dual)); " +
            "administer key management import encryption keys with secret s from '/missing/keys' " +
            "force keystore identified by external store with backup using 'b';")
        assertThatAst(tree.getDescendants(DdlGrammar.KEY_MANAGEMENT_CLAUSES)).hasSize(2)
        val export = tree.getDescendants(DdlGrammar.EXPORT_KEYS).single()
        assertThatAst(tree.getDescendants(DdlGrammar.IMPORT_KEYS)).hasSize(1)
        assertThatAst(export.getFirstChild(DmlGrammar.SELECT_EXPRESSION)).isNotNull()
        assertThatAst(export.getDescendants(DmlGrammar.SELECT_EXPRESSION)).hasSize(3)
        assertThatAst(export.getDescendants(DmlGrammar.SUBQUERY_FACTORING_CLAUSE)).hasSize(1)
        for (helper in listOf(DdlGrammar.FORCE_KEYSTORE, DdlGrammar.KEYSTORE_IDENTIFIED_BY,
            DdlGrammar.KEYSTORE_WITH_BACKUP)) {
            assertThatAst(tree.getDescendants(helper)).isEmpty()
        }
    }
}
