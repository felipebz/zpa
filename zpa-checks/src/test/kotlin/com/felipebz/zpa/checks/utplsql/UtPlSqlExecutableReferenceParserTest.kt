//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class UtPlSqlExecutableReferenceParserTest {

    @Test
    fun parsesOneTwoAndThreeComponentReferencesInSourceOrder() {
        val references = UtPlSqlExecutableReferenceParser.parse(
            " setup , helper_pkg . setup, owner . helper_pkg . setup "
        )

        assertThat(references.map { it.sourceText }).containsExactly(
            "setup",
            "helper_pkg . setup",
            "owner . helper_pkg . setup"
        )
        assertThat(references.map { it.components }).containsExactly(
            listOf("setup"),
            listOf("helper_pkg", "setup"),
            listOf("owner", "helper_pkg", "setup")
        )
    }

    @Test
    fun ignoresEmptyAndNonExecutableListEntries() {
        val references = UtPlSqlExecutableReferenceParser.parse(",, setup, 123, !, helper.setup,")

        assertThat(references.map { it.sourceText }).containsExactly("setup", "helper.setup")
    }

    @Test
    fun preservesMalformedComponentShapesProducedByUtPlSql() {
        val references = UtPlSqlExecutableReferenceParser.parse("owner.package.procedure.extra, package..procedure")

        assertThat(references.map { it.sourceText }).containsExactly(
            "owner.package.procedure.extra",
            "package..procedure"
        )
        assertThat(references.map { it.hasSupportedComponentShape }).containsExactly(false, false)
        assertThat(references.map { it.toProjectReference() }).containsOnlyNulls()
    }
}
