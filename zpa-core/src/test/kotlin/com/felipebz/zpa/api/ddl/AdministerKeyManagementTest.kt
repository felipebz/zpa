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
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class AdministerKeyManagementTest : RuleTest() {

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
    fun matchesOpenKeystore() {
        matches(
            "set keystore open identified by password;",
            "set keystore open identified by \"user_id:password\"",
            "set keystore open identified by external store",
            "set keystore open identified by store",
            "set keystore open identified by keystore",
            "set keystore open force keystore identified by password",
            "set keystore open identified by password container = current",
            "set keystore open force keystore identified by external store container = all;"
        )
    }

    @Test
    fun matchesCloseKeystore() {
        matches(
            "set keystore close;",
            "set keystore close identified by password",
            "set keystore close identified by \"user_id:password\"",
            "set keystore close identified by external store",
            "set keystore close container = all",
            "set keystore close identified by password container = current;"
        )
    }

    @Test
    fun rejectsMalformedOpenKeystore() {
        notMatches(
            "set keystore open",
            "set keystore open container = current",
            "set keystore open container = all identified by password",
            "set keystore open identified by password force keystore",
            "set keystore open force identified by password",
            "set keystore open force keystore force keystore identified by password",
            "set keystore open identified by 'password'",
            "set keystore open identified by 123",
            "set keystore open identified by :pw",
            "set keystore open identified by select",
            "set keystore open identified by",
            "set keystore open identified password",
            "set keystore open identified by external",
            "set keystore open identified by external password",
            "set keystore open identified by password container",
            "set keystore open identified by password container all",
            "set keystore open identified by password container =",
            "set keystore open identified by password container = pdb1",
            "set keystore open identified by password container = all container = all",
            "set keystore open identified by password extra"
        )
    }

    @Test
    fun rejectsMalformedCloseKeystore() {
        notMatches(
            "set keystore close force keystore",
            "set keystore close force keystore identified by password",
            "set keystore close container = all identified by password",
            "set keystore close identified by",
            "set keystore close identified by external",
            "set keystore close identified by 'password'",
            "set keystore close container = current container = all",
            "set keystore close extra"
        )
    }

    @Test
    fun matchesAddAndUpdateSecretsInCurrentKeystore() {
        for (operation in listOf("add", "update")) {
            matches(
                "$operation secret 's' for client 'c' identified by pw",
                "$operation secret 's' for client 'c' identified by \"user:pw\"",
                "$operation secret 's' for client 'c' identified by external store",
                "$operation secret 's' for client 'c' identified by pw with backup",
                "$operation secret 's' for client 'c' using tag 't' identified by pw",
                "$operation secret 's' for client 'c' force keystore identified by pw",
                "$operation secret 's' for client 'c' using tag 't' force keystore " +
                    "identified by external store with backup using 'b';"
            )
        }
    }

    @Test
    fun matchesExplicitSecretKeystoreTargets() {
        for (operation in listOf("add", "update")) {
            for (target in listOf("keystore '/w' identified by pw",
                "keystore '/w' identified by \"pw\"",
                "auto_login keystore '/w'", "local auto_login keystore '/w'")) {
                matches(
                    "$operation secret 's' for client 'c' to $target",
                    "$operation secret 's' for client 'c' using tag 't' to $target with backup",
                    "$operation secret 's' for client 'c' using tag 't' to $target with backup using 'b'"
                )
            }
        }
        // On explicit targets EXTERNAL is an ordinary password, not EXTERNAL STORE.
        matches("add secret 's' for client 'c' to keystore '/w' identified by external")
    }

    @Test
    fun matchesDeleteSecrets() {
        matches(
            "delete secret for client 'c' identified by pw",
            "delete secret for client 'c' identified by \"user:pw\" with backup",
            "delete secret for client 'c' identified by external store",
            "delete secret for client 'c' force keystore identified by external store with backup using 'b'"
        )
        for (target in listOf("keystore '/w' identified by pw",
            "keystore '/w' identified by \"pw\"",
            "auto_login keystore '/w'", "local auto_login keystore '/w'")) {
            matches(
                "delete secret for client 'c' from $target",
                "delete secret for client 'c' from $target with backup",
                "delete secret for client 'c' from $target with backup using 'b'"
            )
        }
    }

    @Test
    fun matchesOracleLiteralAndIdentifierVariants() {
        matches(
            "add secret q'[s''s]' for client N'c' using tag \"TAG\" identified by pw",
            "update secret N's' for client q'[c]' using tag N't' identified by pw",
            "add secret 's' for client 'c' using tag \"tag\" identified by pw",
            "add secret 's' for client 'c' using tag bare_tag identified by pw",
            "delete secret for client 'c' identified by pw with backup using backup_name",
            "delete secret for client 'c' identified by pw with backup using \"BACKUP_NAME\"",
            "delete secret for client 'c' identified by pw with backup using q'[b]'",
            "delete secret for client 'c' identified by pw with backup using N'b'",
            "add secret 's' for client 'c' to auto_login keystore q'[/w]'",
            "add secret 's' for client 'c' to auto_login keystore N'/w'",
            "update secret 's' for client 'c' to keystore nq'[/w]' identified by pw",
            "delete secret for client 'c' from local auto_login keystore N'/w'"
        )
    }

    @Test
    fun rejectsMalformedSecretValues() {
        notMatches(
            "add secret for client 'c' identified by pw",
            "update secret 's' for client identified by pw",
            "delete secret for client",
            "delete secret 's' for client 'c' identified by pw",
            "delete secret 's' for client 'c' from auto_login keystore '/w'",
            "add secret 's' client 'c' identified by pw"
        )
        for (value in listOf("bare", "\"quoted\"", "123", ":bind")) {
            notMatches(
                "add secret $value for client 'c' identified by pw",
                "update secret 's' for client $value identified by pw",
                "delete secret for client $value identified by pw",
                "add secret 's' for client 'c' to auto_login keystore $value"
            )
        }
        for (value in listOf("123", ":bind")) {
            notMatches("add secret 's' for client 'c' using tag $value identified by pw")
        }
        for (value in listOf("'pw'", "123", ":bind")) {
            notMatches(
                "add secret 's' for client 'c' identified by $value",
                "delete secret for client 'c' from keystore '/w' identified by $value"
            )
        }
    }

    @Test
    fun rejectsSecretClausePermutationsAndDuplicates() {
        for (suffix in listOf(
            "using tag 't' using tag 'u' identified by pw",
            "force keystore using tag 't' identified by pw",
            "identified by pw using tag 't'",
            "identified by pw force keystore",
            "with backup identified by pw",
            "force keystore force keystore identified by pw",
            "identified by pw identified by pw",
            "identified by pw with backup with backup",
            "using 't' identified by pw",
            "using tag identified by pw",
            "identified by pw with backup using",
            "identified by pw with backup using 123",
            "identified by pw with backup using backup",
            "identified by pw with backup using 'b' using 'c'",
            "identified by pw to keystore '/w'",
            "to keystore '/w' using tag 't' identified by pw",
            "force keystore to keystore '/w' identified by pw",
            "force identified by pw",
            "identified by",
            "identified by external",
            "with",
            "identified by pw with",
            "identified by pw backup",
            "identified by pw with backup extra"
        )) {
            for (operation in listOf("add", "update")) {
                notMatches("$operation secret 's' for client 'c' $suffix")
            }
        }
        notMatches(
            "add secret 's' for client 'c'",
            "add secret 's' for client 'c' force keystore",
            "delete secret for client 'c'",
            "delete secret for client 'c' using tag 't' identified by pw",
            "delete secret for client 'c' identified by pw force keystore",
            "delete secret for client 'c' with backup identified by pw",
            "delete secret for client 'c' force keystore force keystore identified by pw",
            "delete secret for client 'c' identified by pw identified by pw",
            "delete secret for client 'c' identified by pw with backup with backup",
            "delete secret for client 'c' identified by pw from keystore '/w'"
        )
    }

    @Test
    fun rejectsIncompleteAndConflictingSecretTargets() {
        for (target in listOf(
            "", "keystore", "keystore '/w'", "auto_login", "auto_login keystore",
            "local keystore '/w' identified by pw", "local auto_login '/w'",
            "keystore keystore '/w' identified by pw",
            "auto_login keystore keystore '/w'",
            "keystore '/w' force keystore identified by pw",
            "keystore '/w' identified by external store",
            "auto_login keystore '/w' identified by pw",
            "auto_login keystore '/w' force keystore",
            "auto_login local keystore '/w'"
        )) {
            notMatches(
                "add secret 's' for client 'c' to $target",
                "update secret 's' for client 'c' to $target",
                "delete secret for client 'c' from $target"
            )
        }
        notMatches(
            "delete secret for client 'c' to auto_login keystore '/w'",
            "add secret 's' for client 'c' from auto_login keystore '/w'"
        )
    }

    @Test
    fun preservesSecretAstBoundariesAndIdentifierCompatibility() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create table secret (client number, tag number, auto_login number); " +
            "administer key management add secret 's' for client 'c' using tag \"TAG\" " +
            "force keystore identified by pw with backup using 'b'; " +
            "administer key management update secret 's' for client 'c' to auto_login keystore '/w'; " +
            "administer key management delete secret for client 'c' from keystore '/w' identified by pw;")
        assertThatAst(tree.getDescendants(DdlGrammar.ADMINISTER_KEY_MANAGEMENT)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.SECRET_MANAGEMENT_CLAUSES)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.ADD_UPDATE_SECRET)).hasSize(2)
        assertThatAst(tree.getDescendants(DdlGrammar.DELETE_SECRET)).hasSize(1)
        for (helper in listOf(DdlGrammar.FORCE_KEYSTORE, DdlGrammar.KEYSTORE_IDENTIFIED_BY,
            DdlGrammar.KEYSTORE_WITH_BACKUP, DdlGrammar.SECRET_KEYSTORE_TARGET)) {
            assertThatAst(tree.getDescendants(helper)).isEmpty()
        }
    }

    @Test
    fun rejectsDeferredOperationsAndMalformedHeaders() {
        notMatches(
            "set keystore reopen",
            "set keystore",
            "keystore open identified by password",
            "merge keystore '/w' into new keystore '/n' identified by pw",
            "move keys to new keystore '/w' identified by pw from identified by pw",
            "move keys to new keystore '/w' identified by pw from force keystore identified by pw " +
                "with identifier in (select key_id from v\$encryption_keys)",
            "isolate keystore identified by pw from root keystore identified by pw with backup",
            "unite keystore identified by pw with root keystore identified by pw with backup"
        )
        assertThat(p).notMatches("administer key set keystore close")
    }

    @Test
    fun keepsNewKeywordsUsableAsIdentifiersAndAvoidsFallback() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create table administer (keystore number); " +
            "administer key management set keystore open identified by keystore container = all; " +
            "select administer.keystore from administer;")
        assertThatAst(tree.getDescendants(DdlGrammar.OPEN_KEYSTORE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.KEYSTORE_IDENTIFIED_BY)).isEmpty()
        assertThat(p).notMatches("administer key management set keystore close extra;")
    }
}
