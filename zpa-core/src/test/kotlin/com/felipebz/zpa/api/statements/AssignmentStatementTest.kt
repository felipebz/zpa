//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssignmentStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ASSIGNMENT_STATEMENT)
    }

    @Test
    fun assignmentToVariable() {
        assertThat(p).matches("var := 1;")
    }
    @Test
    fun assignmentToOutIdentifier() {
        assertThat(p).matches("out := 0;")
    }

    @Test
    fun assignmentToRecordAttribute() {
        assertThat(p).matches("rec.attribute := 1;")
    }

    @Test
    fun assignmentToCollectionElement() {
        assertThat(p).matches("collection(1) := 1;")
    }

    @Test
    fun assignmentToItemInRecordCollection() {
        assertThat(p).matches("collection(1).field := 1;")
    }

    @Test
    fun assignmentToHostVariable() {
        assertThat(p).matches(":var := 1;")
    }

    @Test
    fun assignmentToIndicatorVariable() {
        assertThat(p).matches(":var:indicator := 1;")
    }

    @Test
    fun matchesLabeledAssignment() {
        assertThat(p).matches("<<foo>> var := 1;")
    }

}
