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
