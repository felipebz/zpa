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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertAst

class KeystoreManagementTest : RuleTest() {

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
    fun matchesEveryCreateBranchWithOptionalLocation() {
        for (branch in listOf("keystore", "auto_login keystore from keystore",
            "local auto_login keystore from keystore")) {
            for (location in listOf("", "'/w'", "q'[/w]'", "N'/w'")) {
                for (password in listOf("pw", "\"user:pw\"", "external")) {
                    matches("create $branch $location identified by $password")
                }
            }
        }
    }

    @Test
    fun rejectsMalformedCreateKeystore() {
        notMatches(
            "create keystore '/w'",
            "create keystore '/w' identified by",
            "create keystore '/w' identified pw",
            "create keystore '/w' identified by external store",
            "create local keystore '/w' identified by pw",
            "create auto_login local keystore from keystore '/w' identified by pw",
            "create auto_login keystore '/w' identified by pw",
            "create auto_login keystore from '/w' identified by pw",
            "create auto_login keystore from keystore '/w'",
            "create auto_login keystore from keystore '/w' identified by external store",
            "create keystore '/w' force keystore identified by pw",
            "create keystore '/w' identified by pw with backup",
            "create keystore '/w' identified by pw container = all",
            "create keystore '/w' identified by pw identified by pw",
            "create keystore identified by pw '/w'",
            "create keystore '/w' '/x' identified by pw",
            "create keystore '/w' identified by pw extra"
        )
    }

    @Test
    fun matchesBackupKeystoreOptions() {
        matches(
            "backup keystore identified by pw",
            "backup keystore identified by external store",
            "backup keystore force keystore identified by \"pw\"",
            "backup keystore identified by pw to '/dest'",
            "backup keystore using 'b' identified by pw",
            "backup keystore using 'b' force keystore identified by external store to '/dest';",
            "backup keystore using bare_name identified by pw",
            "backup keystore using \"BACKUP_NAME\" identified by pw",
            "backup keystore using q'[b]' identified by pw to q'[/dest]'",
            "backup keystore using N'b' identified by pw to N'/dest'"
        )
    }

    @Test
    fun rejectsMalformedBackupKeystore() {
        notMatches(
            "backup keystore",
            "backup keystore using 'b'",
            "backup keystore using identified by pw",
            "backup keystore using 123 identified by pw",
            "backup keystore using backup identified by pw",
            "backup keystore using :name identified by pw",
            "backup keystore using 'b' using 'c' identified by pw",
            "backup keystore force keystore using 'b' identified by pw",
            "backup keystore force keystore force keystore identified by pw",
            "backup keystore identified by pw force keystore",
            "backup keystore identified by pw using 'b'",
            "backup keystore to '/dest' identified by pw",
            "backup keystore identified by pw to",
            "backup keystore identified by pw to '/dest' to '/other'",
            "backup keystore identified by pw identified by pw",
            "backup keystore identified by external",
            "backup keystore identified by pw with backup",
            "backup keystore identified by pw container = current",
            "backup keystore identified by pw extra"
        )
    }

    @Test
    fun matchesPasswordChangesWithOptionalBackup() {
        matches(
            "alter keystore password identified by old_pw set new_pw",
            "alter keystore password identified by \"old\" set \"new\" with backup",
            "alter keystore password force keystore identified by old_pw set new_pw",
            "alter keystore password force keystore identified by old_pw set new_pw with backup using 'b'",
            "alter keystore password identified by old_pw set new_pw with backup using backup_name",
            "alter keystore password identified by old_pw set new_pw with backup using \"BACKUP_NAME\"",
            "alter keystore password identified by external set external"
        )
    }

    @Test
    fun rejectsMalformedPasswordChanges() {
        notMatches(
            "alter keystore password set new_pw",
            "alter keystore password identified by old_pw",
            "alter keystore password identified by old_pw set",
            "alter keystore password identified by old_pw new_pw",
            "alter keystore password identified by external store set new_pw",
            "alter keystore password identified by old_pw set external store",
            "alter keystore password identified by old_pw force keystore set new_pw",
            "alter keystore password force keystore force keystore identified by old_pw set new_pw",
            "alter keystore password identified by old_pw with backup set new_pw",
            "alter keystore password identified by old_pw set new_pw set other_pw",
            "alter keystore password identified by old_pw set new_pw with backup with backup",
            "alter keystore password identified by old_pw set new_pw with backup using",
            "alter keystore password identified by old_pw set new_pw with backup container = current",
            "alter keystore password identified by old_pw set new_pw container = all",
            "alter keystore password identified by old_pw set new_pw extra"
        )
    }

    @Test
    fun matchesMergeIntoNewWithIndependentSourceAuthentication() {
        for (firstAuth in listOf("", "identified by first_pw")) {
            for (secondAuth in listOf("", "identified by \"second_pw\"")) {
                for (destAuth in listOf("identified by dest_pw", "identified by external store")) {
                    matches("merge keystore '/a' $firstAuth and keystore '/b' $secondAuth " +
                        "into new keystore '/c' $destAuth")
                }
            }
        }
        matches("merge keystore N'/a' and keystore q'[/b]' into new keystore nq'[/c]' identified by pw")
    }

    @Test
    fun matchesMergeIntoExistingWithOptionalSourceAuthenticationAndBackup() {
        for (sourceAuth in listOf("", "identified by source_pw", "identified by external")) {
            for (destAuth in listOf("identified by \"dest_pw\"", "identified by external store")) {
                for (backup in listOf("", "with backup", "with backup using 'b'",
                    "with backup using backup_name", "with backup using \"BACKUP_NAME\"")) {
                    matches("merge keystore '/a' $sourceAuth into existing keystore '/b' $destAuth $backup")
                }
            }
        }
    }

    @Test
    fun rejectsMalformedAndMisassociatedMergeClauses() {
        notMatches(
            "merge keystore '/a' into new keystore '/b' identified by pw",
            "merge keystore '/a' and keystore '/b' into existing keystore '/c' identified by pw",
            "merge keystore '/a' and keystore '/b' and keystore '/c' into new keystore '/d' identified by pw",
            "merge keystore '/a' and '/b' into new keystore '/c' identified by pw",
            "merge keystore '/a' and keystore '/b' into new keystore '/c'",
            "merge keystore '/a' into existing keystore '/b' with backup",
            "merge keystore '/a' into keystore '/b' identified by pw",
            "merge keystore '/a' '/b' into new keystore '/c' identified by pw",
            "merge keystore '/a' identified by first_pw identified by second_pw into existing keystore '/b' identified by pw",
            "merge keystore '/a' identified by external store into existing keystore '/b' identified by pw",
            "merge keystore '/a' and keystore '/b' identified by external store into new keystore '/c' identified by pw",
            "merge auto_login keystore '/a' into existing keystore '/b' identified by pw",
            "merge local auto_login keystore '/a' into existing keystore '/b' identified by pw",
            "merge keystore '/a' force keystore identified by pw into existing keystore '/b' identified by pw",
            "merge keystore '/a' into existing keystore '/b' force keystore identified by pw",
            "merge keystore '/a' into existing keystore '/b' identified by pw identified by other_pw",
            "merge keystore '/a' into existing keystore '/b' with backup identified by pw",
            "merge keystore '/a' into existing keystore '/b' identified by pw with backup with backup",
            "merge keystore '/a' into existing keystore '/b' identified by pw with backup using",
            "merge keystore '/a' and keystore '/b' into new keystore '/c' identified by pw with backup",
            "merge keystore '/a' into existing keystore '/b' identified by external",
            "merge keystore '/a' into existing keystore '/b' identified by pw extra"
        )
    }

    @Test
    fun rejectsWrongPasswordAndPathTokenClasses() {
        for (password in listOf("'pw'", "123", ":pw", "select")) {
            notMatches(
                "create keystore '/w' identified by $password",
                "backup keystore identified by $password",
                "alter keystore password identified by $password set new_pw",
                "alter keystore password identified by old_pw set $password",
                "merge keystore '/a' identified by $password into existing keystore '/b' identified by pw",
                "merge keystore '/a' into existing keystore '/b' identified by $password"
            )
        }
        for (path in listOf("bare", "\"/w\"", "123", ":path")) {
            notMatches(
                "create keystore $path identified by pw",
                "backup keystore identified by pw to $path",
                "merge keystore $path into existing keystore '/b' identified by pw",
                "merge keystore '/a' into existing keystore $path identified by pw"
            )
        }
    }

    @Test
    fun preservesLifecycleNodesAndSourcePasswordAssociation() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("administer key management create keystore '/w' identified by pw; " +
            "administer key management backup keystore identified by pw; " +
            "administer key management alter keystore password identified by old_pw set new_pw; " +
            "administer key management merge keystore '/a' identified by first_pw " +
            "and keystore '/b' identified by second_pw into new keystore '/c' identified by dest_pw; " +
            "administer key management merge keystore '/x' into existing keystore '/y' identified by target_pw;")
        assertAst(tree.getDescendants(DdlGrammar.ADMINISTER_KEY_MANAGEMENT)).hasSize(5)
        assertAst(tree.getDescendants(DdlGrammar.CREATE_KEYSTORE)).hasSize(1)
        assertAst(tree.getDescendants(DdlGrammar.BACKUP_KEYSTORE)).hasSize(1)
        assertAst(tree.getDescendants(DdlGrammar.ALTER_KEYSTORE_PASSWORD)).hasSize(1)
        val newMerge = tree.getDescendants(DdlGrammar.MERGE_INTO_NEW_KEYSTORE).single()
        val sources = newMerge.getDescendants(DdlGrammar.MERGE_KEYSTORE_SOURCE)
        assertAst(sources.map { it.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL).tokenValue })
            .containsExactly("'/a'", "'/b'")
        assertAst(sources.map { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue })
            .containsExactly("FIRST_PW", "SECOND_PW")
        assertAst(newMerge.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue).isEqualTo("DEST_PW")
        val existingMerge = tree.getDescendants(DdlGrammar.MERGE_INTO_EXISTING_KEYSTORE).single()
        assertAst(existingMerge.getDescendants(DdlGrammar.MERGE_KEYSTORE_SOURCE).single()
            .getDescendants(PlSqlGrammar.IDENTIFIER_NAME)).isEmpty()
        assertAst(existingMerge.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue).isEqualTo("TARGET_PW")
        for (helper in listOf(DdlGrammar.KEYSTORE_PASSWORD_IDENTIFIED_BY,
            DdlGrammar.KEYSTORE_BACKUP_IDENTIFIER, DdlGrammar.KEYSTORE_IDENTIFIED_BY,
            DdlGrammar.KEYSTORE_WITH_BACKUP, DdlGrammar.FORCE_KEYSTORE)) {
            assertAst(tree.getDescendants(helper)).isEmpty()
        }
    }
}
