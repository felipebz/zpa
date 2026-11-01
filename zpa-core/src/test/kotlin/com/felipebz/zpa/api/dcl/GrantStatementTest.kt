//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.dcl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DclGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GrantStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DclGrammar.GRANT_STATEMENT)
    }

    @Test
    fun matchesSimpleSystemGrant() {
        assertThat(p).matches("grant connect to user1;")
    }

    @Test
    fun matchesGrantToPublic() {
        assertThat(p).matches("grant connect to public;")
    }

    @Test
    fun matchesSystemGrantWithLongPrivilegeName() {
        assertThat(p).matches("grant execute any procedure to user1;")
    }

    @Test
    fun matchesSimpleSystemGrantAllPrivileges() {
        assertThat(p).matches("grant all privileges to user1;")
    }

    @Test
    fun matchesMultipleRoleGrantToUser() {
        assertThat(p).matches("grant connect, resource to user1;")
    }

    @Test
    fun matchesSystemGrantWithGranteeIdentifiedBy() {
        assertThat(p).matches("grant connect to user1 identified by pass;")
    }

    @Test
    fun matchesSystemGrantWithMultipleGranteeIdentifiedBy() {
        assertThat(p).matches("grant connect to user1, user2 identified by pass1, pass2;")
    }

    @Test
    fun matchesSystemGrantWithAdminOption() {
        assertThat(p).matches("grant foo to bar with admin option;")
    }

    @Test
    fun matchesSystemGrantWithDelegateOption() {
        assertThat(p).matches("grant foo to bar with delegate option;")
    }

    @Test
    fun matchesSystemGrantInCurrentContainer() {
        assertThat(p).matches("grant foo to bar container = current;")
    }

    @Test
    fun matchesSystemGrantInAllContainers() {
        assertThat(p).matches("grant foo to bar container = all;")
    }

    @Test
    fun matchesLongSystemGrant() {
        assertThat(p).matches("grant foo to bar with admin option container = all;")
    }

    @Test
    fun matchesSimpleGrantOnObject() {
        assertThat(p).matches("grant execute on proc to user1;")
    }

    @Test
    fun matchesGrantOnObjectToPublic() {
        assertThat(p).matches("grant execute on proc to public;")
    }

    @Test
    fun matchesGrantWithColumnOnObject() {
        assertThat(p).matches("grant select (col) on tab to user1;")
    }

    @Test
    fun matchesGrantWithMultipleColumnsOnObject() {
        assertThat(p).matches("grant select (col, col2) on tab to user1;")
    }

    @Test
    fun matchesGrantOnObjectWithHierarchyOption() {
        assertThat(p).matches("grant execute on proc to user1 with hierarchy option;")
    }

    @Test
    fun matchesGrantOnObjectWithGrantOption() {
        assertThat(p).matches("grant execute on proc to user1 with grant option;")
    }

    @Test
    fun matchesGrantOnObjectWithHierarchyAndGrantOption() {
        assertThat(p).matches("grant execute on proc to user1 with hierarchy option with grant option;")
    }

    @Test
    fun matchesGrantRoleToFunction() {
        assertThat(p).matches("grant foo to function bar;")
    }

    @Test
    fun matchesGrantRoleToProcedure() {
        assertThat(p).matches("grant foo to procedure bar;")
    }

    @Test
    fun matchesGrantRoleToPackage() {
        assertThat(p).matches("grant foo to package bar;")
    }

    @Test
    fun matchesGrantRoleToMultiplePrograms() {
        assertThat(p).matches("grant foo, bar to package foo2, procedure bar2;")
    }

}
