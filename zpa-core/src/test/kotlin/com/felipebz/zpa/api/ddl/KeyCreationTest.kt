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

class KeyCreationTest : RuleTest() {

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
    fun matchesGeneratedAndExternallySuppliedKeys() {
        for (operation in listOf("set", "create")) {
            matches(
                "$operation key identified by pw",
                "$operation encryption key identified by pw;",
                "$operation key 'mkid:mk' identified by pw",
                "$operation key using tag 'tag_value' identified by pw",
                "$operation key using algorithm 'AES256' identified by pw",
                "$operation key force keystore identified by pw",
                "$operation key identified by external store",
                "$operation key identified by \"user_id:pw\" with backup",
                "$operation key identified by pw with backup using 'backup_name'",
                "$operation key identified by pw container = current",
                "$operation key identified by pw with backup container = all",
                "$operation encryption key 'mkid:mk' using tag 'tag_value' " +
                    "using algorithm 'AES256' force keystore identified by external store " +
                    "with backup using 'backup_name' container = all;"
            )
        }
    }

    @Test
    fun matchesUseAndTagWithOptionalBackup() {
        matches(
            "use key 'key_id' identified by pw",
            "use encryption key 'key_id' identified by external store;",
            "use key 'key_id' using tag 'tag_value' identified by pw",
            "use key 'key_id' force keystore identified by pw with backup",
            "use encryption key 'key_id' using tag tag_value force keystore " +
                "identified by external store with backup using 'backup_name';",
            "set tag 'tag_value' for 'key_id' identified by pw",
            "set tag 'tag_value' for 'key_id' identified by external store",
            "set tag 'tag_value' for 'key_id' force keystore identified by pw with backup",
            "set tag tag_value for key_id force keystore identified by \"user_id:pw\" " +
                "with backup using backup_name;"
        )
    }

    @Test
    fun matchesNormalLiteralAndIdentifierTokens() {
        for (operation in listOf("set", "create")) {
            for (material in listOf("'mkid:mk'", "N'mkid:mk'", "q'[mkid:mk]'", "nq'[mkid:mk]'")) {
                matches("$operation key $material identified by pw")
            }
            for (algorithm in listOf("'AES256'", "q'[ARIA256]'", "N'GOST256'", "nq'[SEED128]'")) {
                matches("$operation key using algorithm $algorithm identified by pw")
            }
            for (tag in listOf("'tag_value'", "N'tag_value'", "q'[tag_value]'", "nq'[tag_value]'",
                "tag_value", "\"TAG_VALUE\"")) {
                matches("$operation key using tag $tag identified by pw")
            }
        }
        for (keyId in listOf("'key_id'", "N'key_id'", "q'[key_id]'", "nq'[key_id]'",
            "key_id", "\"KEY_ID\"")) {
            matches(
                "use key $keyId using tag \"TAG_VALUE\" identified by pw",
                "set tag q'[tag_value]' for $keyId identified by pw"
            )
        }
        matches(
            "set key 'mkid:mk' /* material */ using /* option */ tag tag_value " +
                "using algorithm q'[AES256]' force keystore identified by pw with backup using backup_name",
            "create key identified by pw with backup using \"BACKUP_NAME\"",
            "use key 'key_id' identified by pw with backup using N'backup_name'",
            "set tag \"TAG_VALUE\" for key_id identified by pw with backup using q'[backup_name]'"
        )
    }

    @Test
    fun leavesMaterialAndOtherValueValidationToOracle() {
        for (operation in listOf("set", "create")) {
            matches(
                "$operation key '' identified by pw",
                "$operation key 'not-a-valid-mkid:not-a-valid-key' identified by pw",
                "$operation key 'a:b:c' identified by pw",
                "$operation key using algorithm 'unsupported_algorithm' identified by pw",
                "$operation key using algorithm '' identified by pw",
                "$operation key using tag '' identified by pw"
            )
        }
        matches(
            "use key '' identified by pw",
            "set tag '' for '' identified by pw"
        )
    }

    @Test
    fun rejectsSplitMaterialAndExtraKeyTokens() {
        for (operation in listOf("set", "create")) {
            for (material in listOf("'mkid':'mk'", "'mkid' : 'mk'", "mkid:mk", "mkid : mk",
                "\"mkid:mk\"", "\"mkid\":\"mk\"", "'mkid':mk", "mkid:'mk'", "mkid",
                "123", ":material", "('mkid:mk')", "'mkid:mk' 'extra'", "'mkid','mk'")) {
                notMatches("$operation key $material identified by pw")
            }
        }
        notMatches(
            "use key 'key_id' 'extra_id' identified by pw",
            "use key 'mkid':'mk' identified by pw",
            "set tag 'tag_value' for 'key_id' 'extra_id' identified by pw"
        )
    }

    @Test
    fun rejectsWrongCreationOptionOrderAndIncompleteClauses() {
        for (operation in listOf("set", "create")) {
            notMatches(
                "$operation key",
                "$operation key with backup",
                "$operation key identified by",
                "$operation key identified by 'pw'",
                "$operation key identified by external",
                "$operation key using tag identified by pw",
                "$operation key using algorithm identified by pw",
                "$operation key using algorithm AES256 identified by pw",
                "$operation key using algorithm \"AES256\" identified by pw",
                "$operation key using tag 123 identified by pw",
                "$operation key using algorithm 'AES256' using tag 'tag_value' identified by pw",
                "$operation key force keystore using tag 'tag_value' identified by pw",
                "$operation key using tag 'tag_value' 'mkid:mk' identified by pw",
                "$operation key using tag 'one' using tag 'two' identified by pw",
                "$operation key using algorithm 'AES256' using algorithm 'ARIA256' identified by pw",
                "$operation key identified by pw force keystore",
                "$operation key identified by pw using tag 'tag_value'",
                "$operation key identified by pw container = all with backup",
                "$operation key identified by pw with backup using",
                "$operation key identified by pw with backup with backup",
                "$operation key identified by pw container =",
                "$operation key identified by pw container = all container = current",
                "$operation key identified by pw extra",
                "$operation key identified by pw; extra"
            )
        }
    }

    @Test
    fun rejectsUnsupportedUseAndTagOptionsAndMissingOperands() {
        notMatches(
            "use key identified by pw",
            "use key 123 identified by pw",
            "use key :key_id identified by pw",
            "use key 'key_id'",
            "use key 'key_id' using algorithm 'AES256' identified by pw",
            "use key 'key_id' force keystore using tag 'tag_value' identified by pw",
            "use key 'key_id' using tag 'one' using tag 'two' identified by pw",
            "set encryption tag 'tag_value' for 'key_id' identified by pw",
            "set tag for 'key_id' identified by pw",
            "set tag 'tag_value' 'key_id' identified by pw",
            "set tag 'tag_value' for identified by pw",
            "set tag 'tag_value' for 123 identified by pw",
            "set tag 'tag_value' for 'key_id' using tag 'other' identified by pw",
            "set tag 'tag_value' for 'key_id' using algorithm 'AES256' identified by pw"
        )
        for (prefix in listOf("use key 'key_id'", "set tag 'tag_value' for 'key_id'")) {
            notMatches(
                "$prefix identified by 'pw'",
                "$prefix identified by pw force keystore",
                "$prefix identified by pw container = current",
                "$prefix identified by pw with backup container = all",
                "$prefix identified by pw with backup using",
                "$prefix identified by pw with backup with backup",
                "$prefix identified by pw extra"
            )
        }
    }

    @Test
    fun preservesDistinctOperationAstNodesInACompleteFile() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("administer key management set key 'mkid:mk' using tag 'new' " +
            "using algorithm 'AES256' identified by pw container = current; " +
            "administer key management create encryption key 'other_id:other_key' identified by pw; " +
            "administer key management use encryption key 'key_id' using tag tag_value identified by pw; " +
            "administer key management set tag 'replacement' for 'key_id' identified by pw with backup;")
        val operations = tree.getDescendants(DdlGrammar.KEY_MANAGEMENT_CLAUSES)
        assertThatAst(operations).hasSize(4)
        for ((index, rule) in listOf(DdlGrammar.SET_KEY, DdlGrammar.CREATE_KEY,
            DdlGrammar.USE_KEY, DdlGrammar.SET_KEY_TAG).withIndex()) {
            assertThatAst(operations[index].getFirstChild(rule)).isNotNull()
            assertThatAst(tree.getDescendants(rule)).hasSize(1)
        }
    }
}
