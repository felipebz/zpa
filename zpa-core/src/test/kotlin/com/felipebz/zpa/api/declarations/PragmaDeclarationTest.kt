//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.declarations

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PragmaDeclarationTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.PRAGMA_DECLARATION)
    }

    @Test
    fun matchesAutonomousTransaction() {
        assertThat(p).matches("pragma autonomous_transaction;")
    }

    @Test
    fun matchesExceptionInit() {
        assertThat(p).matches("pragma exception_init(1, 1);")
    }

    @Test
    fun matchesSeriallyReusable() {
        assertThat(p).matches("pragma serially_reusable;")
    }

    @Test
    fun matchesInterface() {
        assertThat(p).matches("pragma interface(c, func, 1);")
    }

    @Test
    fun matchesRestrictReferencesPragma() {
        assertThat(p).matches("pragma restrict_references(foo, rnds, wnds);")
    }

    @Test
    fun matchesUdfPragma() {
        assertThat(p).matches("pragma udf;")
    }

    @Test
    fun matchesDeprecatePragma() {
        assertThat(p).matches("pragma deprecate(object);")
        assertThat(p).matches("pragma deprecate(object, 'object is deprecated');")
    }

    @Test
    fun matchesCoveragePragma() {
        assertThat(p).matches("pragma coverage('NOT_FEASIBLE_START');")
    }

}
