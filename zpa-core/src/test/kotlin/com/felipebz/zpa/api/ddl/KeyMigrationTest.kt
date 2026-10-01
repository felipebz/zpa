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

class KeyMigrationTest : RuleTest() {

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
    fun matchesForwardMigrationFromSetAndUseWithIndependentPasswords() {
        for (operation in listOf("set key", "set encryption key", "use key 'kid'", "use encryption key \"kid\"")) {
            for (force in listOf("", "force keystore")) {
                for (backup in listOf("", "with backup", "with backup using 'b'", "with backup using backup_name")) {
                    matches("$operation identified by \"hardware:user:password\" $force migrate using software_pw $backup")
                }
            }
        }
        matches(
            "use key bare_id identified by hardware_pw migrate using \"software:pw\"",
            "use key N'kid' identified by hardware_pw migrate using software_pw",
            "use key q'[kid]' identified by hardware_pw migrate using software_pw",
            "use key '' identified by hardware_pw migrate using software_pw",
            "set key identified by external migrate using external"
        )
    }

    @Test
    fun matchesReverseMigrationOnlyFromSet() {
        for (encryption in listOf("", "encryption")) {
            for (force in listOf("", "force keystore")) {
                for (backup in listOf("", "with backup", "with backup using 'b'", "with backup using \"BACKUP_NAME\"")) {
                    matches("set $encryption key identified by software_pw $force reverse migrate using \"hardware:user:pw\" $backup")
                }
            }
        }
        matches("set key identified by external reverse migrate using external")
    }

    @Test
    fun matchesRuntimeTagCompositionButNotAlgorithmComposition() {
        for (tag in listOf("'t'", "\"TAG\"", "bare_tag", "N't'", "q'[t]'")) {
            matches(
                "set key using tag $tag identified by hardware_pw force keystore migrate using software_pw with backup",
                "use key 'kid' using tag $tag identified by hardware_pw migrate using software_pw",
                "set key using tag $tag identified by software_pw reverse migrate using hardware_pw"
            )
        }
        notMatches(
            "set key using algorithm 'AES256' identified by hardware_pw migrate using software_pw",
            "set key using tag 't' using algorithm 'AES256' identified by hardware_pw migrate using software_pw",
            "set key using algorithm 'AES256' identified by software_pw reverse migrate using hardware_pw",
            "use key 'kid' using algorithm 'AES256' identified by hardware_pw migrate using software_pw"
        )
    }

    @Test
    fun rejectsMigrationOnWrongBaseOperationsOrWithExtraKeyMaterial() {
        notMatches(
            "create key identified by hardware_pw migrate using software_pw",
            "create key identified by software_pw reverse migrate using hardware_pw",
            "use key 'kid' identified by software_pw reverse migrate using hardware_pw",
            "set key 'kid' identified by hardware_pw migrate using software_pw",
            "set key 'mkid:mk' identified by hardware_pw migrate using software_pw",
            "set key 'kid' identified by software_pw reverse migrate using hardware_pw",
            "use key 'kid' 'other_id' identified by hardware_pw migrate using software_pw",
            "set tag 't' for 'kid' identified by hardware_pw migrate using software_pw",
            "import keys with secret s from '/f' identified by hardware_pw migrate using software_pw"
        )
    }

    @Test
    fun rejectsForceAuthenticationBackupAndContainerMisplacement() {
        notMatches(
            "set key force keystore identified by hardware_pw migrate using software_pw",
            "use key 'kid' force keystore identified by hardware_pw migrate using software_pw",
            "set key force keystore identified by software_pw reverse migrate using hardware_pw",
            "set key identified by hardware_pw migrate force keystore using software_pw",
            "set key identified by hardware_pw migrate using software_pw force keystore",
            "set key identified by hardware_pw with backup migrate using software_pw",
            "set key identified by software_pw with backup reverse migrate using hardware_pw",
            "set key identified by hardware_pw migrate using software_pw with backup container = all",
            "use key 'kid' identified by hardware_pw migrate using software_pw container = current",
            "set key identified by software_pw reverse migrate using hardware_pw with backup container = current",
            "set key identified by hardware_pw using tag 't' migrate using software_pw"
        )
    }

    @Test
    fun rejectsExternalStoreSyntaxInEitherAuthenticationRole() {
        notMatches(
            "set key identified by external store migrate using software_pw",
            "use key 'kid' identified by external store migrate using software_pw",
            "set key identified by software_pw reverse migrate using external store",
            "set key identified by external store reverse migrate using hardware_pw",
            "set key identified by hardware_pw migrate using external store"
        )
    }

    @Test
    fun rejectsWrongPasswordTokenClassesForBothDirectionsAndRoles() {
        for (password in listOf("'pw'", "N'pw'", "123", ":pw", "select")) {
            notMatches(
                "set key identified by $password migrate using software_pw",
                "use key 'kid' identified by hardware_pw migrate using $password",
                "set key identified by $password reverse migrate using hardware_pw",
                "set key identified by software_pw reverse migrate using $password"
            )
        }
    }

    @Test
    fun rejectsIncompleteRepeatedAndConflictingMigrationClauses() {
        notMatches(
            "set key migrate using software_pw",
            "set key reverse migrate using hardware_pw",
            "set key identified by hardware_pw migrate software_pw",
            "set key identified by hardware_pw migrate using",
            "set key identified by software_pw reverse using hardware_pw",
            "set key identified by software_pw reverse migrate using",
            "set key identified by hardware_pw force keystore force keystore migrate using software_pw",
            "set key using tag 't' using tag 'u' identified by hardware_pw migrate using software_pw",
            "set key identified by hardware_pw identified by other_pw migrate using software_pw",
            "set key identified by hardware_pw migrate using software_pw with backup with backup",
            "set key identified by software_pw reverse migrate using hardware_pw with backup using",
            "set key identified by hardware_pw migrate using software_pw reverse migrate using hardware_pw",
            "set key identified by software_pw reverse migrate using hardware_pw migrate using software_pw",
            "set key identified by hardware_pw migrate using software_pw extra"
        )
    }

    @Test
    fun preservesMigrationOperationAndAuthenticationRolesInAst() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("administer key management use encryption key 'kid' using tag 't' " +
            "identified by hardware_pw force keystore migrate using software_pw with backup using 'b'; " +
            "administer key management set key identified by reverse_software_pw " +
            "reverse migrate using reverse_hardware_pw;")
        val forward = tree.getDescendants(DdlGrammar.MIGRATE_KEY).single()
        val reverse = tree.getDescendants(DdlGrammar.REVERSE_MIGRATE_KEY).single()
        assertAst(forward.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { it.tokenValue })
            .containsExactly("HARDWARE_PW", "SOFTWARE_PW")
        assertAst(reverse.getChildren(PlSqlGrammar.IDENTIFIER_NAME).map { it.tokenValue })
            .containsExactly("REVERSE_SOFTWARE_PW", "REVERSE_HARDWARE_PW")
        assertAst(forward.getChildren(PlSqlGrammar.CHARACTER_LITERAL).map { it.tokenValue })
            .containsExactly("'kid'", "'t'", "'b'")
        assertAst(tree.getDescendants(DdlGrammar.KEY_MANAGEMENT_CLAUSES)
            .map { it.children.single().type })
            .containsExactly(DdlGrammar.MIGRATE_KEY, DdlGrammar.REVERSE_MIGRATE_KEY)
        assertAst(tree.getDescendants(DdlGrammar.SET_KEY)).isEmpty()
        assertAst(tree.getDescendants(DdlGrammar.USE_KEY)).isEmpty()
        for (helper in listOf(DdlGrammar.KEYSTORE_PASSWORD_IDENTIFIED_BY,
            DdlGrammar.KEYSTORE_IDENTIFIED_BY, DdlGrammar.KEYSTORE_WITH_BACKUP,
            DdlGrammar.KEYSTORE_BACKUP_IDENTIFIER, DdlGrammar.FORCE_KEYSTORE)) {
            assertAst(tree.getDescendants(helper)).isEmpty()
        }
    }
}
