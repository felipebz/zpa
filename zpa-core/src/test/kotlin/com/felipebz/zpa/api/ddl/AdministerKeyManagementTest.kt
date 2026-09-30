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
    fun rejectsOtherOperationsAndHeaders() {
        notMatches(
            "set keystore reopen",
            "set keystore",
            "keystore open identified by password",
            "create keystore '/wallet' identified by password",
            "backup keystore identified by password"
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
        assertThat(p).notMatches("administer key management create keystore '/w' identified by pw;")
    }
}
