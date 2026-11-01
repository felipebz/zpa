//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** The project index and the AST select the same conditional compilation branches when given the same settings. */
class ConditionalCompilationSettingsExtractionTest {

    private val source = "CREATE PACKAGE p AS\n  \$IF \$\$f \$THEN PROCEDURE a; \$ELSE PROCEDURE b; \$END\nEND p;"

    private fun extracted(configuration: PlSqlConfiguration) =
        ProjectDeclarationExtractor(configuration).extract(FileId("p.sql"), source)
            .filterIsInstance<PackageProcedureDeclaration>().map { it.name.lookupName }

    private fun parsed(configuration: PlSqlConfiguration) =
        PlSqlParser.create(configuration).parse(source).getDescendants(PlSqlGrammar.PROCEDURE_DECLARATION)
            .map { it.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue }

    @Test
    fun extractorAndParserFollowTheSameSettings() {
        val configured = PlSqlConfiguration(StandardCharsets.UTF_8, true, ConditionalCompilationSettings.of("f:false", null))
        assertThat(extracted(configured)).containsExactly("B")
        assertThat(parsed(configured)).containsExactly("B")

        val unconfigured = PlSqlConfiguration(StandardCharsets.UTF_8, true)
        assertThat(extracted(unconfigured)).containsExactly("A")
        assertThat(parsed(unconfigured)).containsExactly("A")
    }
}
