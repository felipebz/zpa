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
