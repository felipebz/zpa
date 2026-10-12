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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** CREATE discovery, dispatch and advancement, on hand-lexed sources and through the public facade. */
class UnitDispatcherTest {
    private val fileId = FileId("units.sql")

    private fun extract(text: String) =
        UnitDispatcher(fileId, PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text), DiscardedSourceText(text))
            .extract().declarations

    private fun described(text: String) = extract(text).map { it.kind to it.qualifiedName().toString() }

    @Test
    fun emptyAndTriviaOnlySourcesYieldNothing() {
        assertThat(extract("")).isEmpty()
        assertThat(extract("   \n-- only a comment\n/* block */\n")).isEmpty()
        assertThat(extract("SELECT 1 FROM dual;")).isEmpty()
        assertThat(extract("CREATE")).isEmpty()
    }

    @Test
    fun everySupportedUnitKindIsDispatchedInSourceOrder() {
        val text = "CREATE SEQUENCE s;\nCREATE TYPE o AS OBJECT (a NUMBER);\nCREATE TYPE BODY o AS MEMBER PROCEDURE m IS BEGIN NULL; END; END;\n" +
            "CREATE PROCEDURE sp IS BEGIN NULL; END;\nCREATE FUNCTION sf RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "CREATE PACKAGE p AS PROCEDURE q; END p;\n/\nCREATE PACKAGE BODY p AS PROCEDURE q IS BEGIN NULL; END; END p;\n/\n"
        val facts = extract(text)
        assertThat(facts.map { it.kind to it.qualifiedName().toString() }).containsExactly(
            ProjectDeclarationKind.SEQUENCE to "s", ProjectDeclarationKind.STANDALONE_TYPE to "o",
            ProjectDeclarationKind.STANDALONE_PROCEDURE to "sp", ProjectDeclarationKind.STANDALONE_FUNCTION to "sf",
            ProjectDeclarationKind.PACKAGE to "p", ProjectDeclarationKind.PACKAGE_PROCEDURE to "p.q", ProjectDeclarationKind.PACKAGE_PROCEDURE to "p.q"
        )
        assertThat(facts.map { it.role }).containsExactly(
            DeclarationRole.STANDALONE, DeclarationRole.STANDALONE, DeclarationRole.STANDALONE, DeclarationRole.STANDALONE,
            DeclarationRole.SPECIFICATION, DeclarationRole.SPECIFICATION, DeclarationRole.BODY
        )
        assertThat(facts.map { it.fileId }).containsOnly(fileId)
        assertThat(facts[0].sourceRange).isEqualTo(SourceRange(fileId, 1, 0, 1, 18))
        assertThat(facts[1].sourceRange.startLine).isEqualTo(2)
    }

    @Test
    fun prefixesAndNamesAreRecognizedAsBefore() {
        assertThat(described("CREATE OR REPLACE EDITIONABLE PROCEDURE app.p IS BEGIN NULL; END;")).containsExactly(ProjectDeclarationKind.STANDALONE_PROCEDURE to "app.p")
        assertThat(described("CREATE OR REPLACE NONEDITIONABLE PACKAGE \"Mixed\" AS END;")).containsExactly(ProjectDeclarationKind.PACKAGE to "\"Mixed\"")
        assertThat(described("CREATE SEQUENCE IF NOT EXISTS s.q;")).containsExactly(ProjectDeclarationKind.SEQUENCE to "s.q")
        assertThat(described("CREATE PACKAGE IF NOT EXISTS p AS END;")).containsExactly(ProjectDeclarationKind.PACKAGE to "p")
        assertThat(described("CREATE PACKAGE BODY IF NOT EXISTS p AS PROCEDURE q IS BEGIN NULL; END; END p;"))
            .containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE to "p.q")
        // IF NOT EXISTS cannot be combined with OR REPLACE.
        assertThat(extract("CREATE OR REPLACE PACKAGE IF NOT EXISTS p AS END;")).isEmpty()
        assertThat(extract("CREATE OR REPLACE SEQUENCE s;")).describedAs("sequences take no OR REPLACE").isEmpty()
        assertThat(extract("CREATE PACKAGE \"BODY\" AS END;").single().role).describedAs("a quoted BODY is a name").isEqualTo(DeclarationRole.SPECIFICATION)
    }

    @Test
    fun rejectedCandidatesDoNotSuppressLaterUnitsAndAdvanceAtLeastOneToken() {
        val text = "CREATE INDEX i ON t (c);\nCREATE PROCEDURE bad(a NUMBER b) IS BEGIN NULL; END;\nCREATE PACKAGE broken\n" +
            "CREATE PACKAGE good AS g NUMBER; END good;\n/\nCREATE SEQUENCE after;"
        assertThat(described(text)).containsExactly(
            ProjectDeclarationKind.PACKAGE to "good", ProjectDeclarationKind.PACKAGE_VARIABLE to "good.g", ProjectDeclarationKind.SEQUENCE to "after"
        )
        assertThat(extract("CREATE CREATE CREATE")).isEmpty()
    }

    @Test
    fun specificationAndBodyOfTheSamePackageKeepOverloadOrder() {
        val facts = extract(
            "CREATE PACKAGE p AS PROCEDURE o; PROCEDURE o(a NUMBER); END p;\n/\n" +
                "CREATE PACKAGE BODY p AS PROCEDURE o IS BEGIN NULL; END; PROCEDURE o(a NUMBER) IS BEGIN NULL; END; END p;"
        )
        assertThat(facts.map { it.role }).containsExactly(
            DeclarationRole.SPECIFICATION, DeclarationRole.SPECIFICATION, DeclarationRole.SPECIFICATION, DeclarationRole.BODY, DeclarationRole.BODY
        )
        assertThat(facts.drop(1).map { it.sourceRange.startLine }).containsExactly(1, 1, 3, 3)
        assertThat(facts.filterIsInstance<StandaloneTypeDeclaration>()).isEmpty()
        assertThat(facts.filterIsInstance<SequenceDeclaration>()).isEmpty()
    }

    @Test
    fun repeatedFacadeExtractionsAreIndependent() {
        val extractor = ProjectDeclarationExtractor()
        val text = "CREATE PACKAGE p AS g NUMBER := 1; PROCEDURE q; END p;"
        val first = extractor.extract(fileId, text)
        val second = extractor.extract(fileId, text)
        assertThat(second).isEqualTo(first)
        assertThat(extractor.extract(FileId("other.sql"), text).map { it.fileId }).containsOnly(FileId("other.sql"))
    }
}
