//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.zpa.PlSql
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.SonarQubePlSqlFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import java.nio.charset.StandardCharsets

class ObjectLocatorTest {

    @Test
    fun collectsObjectMetadataWithoutAStoredScopeGraph() {
        val source = """
            CREATE PACKAGE p AS
            END p;
            CREATE PACKAGE BODY p AS
            END p;
            CREATE TYPE t AS OBJECT (id NUMBER);
            CREATE TYPE BODY t AS
              MEMBER PROCEDURE work IS
              BEGIN
                NULL;
              END;
            END;
            CREATE PROCEDURE standalone IS
            BEGIN
              NULL;
            END;
            CREATE FUNCTION calculate RETURN NUMBER IS
            BEGIN
              RETURN 1;
            END;
            CREATE TRIGGER trigger_name BEFORE INSERT ON t
            BEGIN
              NULL;
            END;
        """.trimIndent()

        val inputFile = inputFile(source, InputFile.Type.MAIN)
        val plSqlFile = SonarQubePlSqlFile(inputFile)
        val locations = mutableListOf<MappedObject>()

        AstScanner(
            emptyList(),
            null,
            true
        ).scanFile(plSqlFile, listOf(ObjectLocationVisitor(plSqlFile, locations)))

        val locator = ObjectLocator()
        locator.setObjects(locations)
        locations.clear()

        assertThat(locator.findMainObject("p", PlSqlGrammar.CREATE_PACKAGE)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_PACKAGE)
        val packageBody = locator.findMainObject("p", PlSqlGrammar.CREATE_PACKAGE_BODY)
        assertThat(packageBody).isNotNull
        assertThat(packageBody!!.firstLine).isEqualTo(3)
        assertThat(packageBody.lastLine).isEqualTo(4)
        assertThat(locator.findMainObject("t", PlSqlGrammar.CREATE_TYPE)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_TYPE)
        assertThat(locator.findMainObject("t", PlSqlGrammar.CREATE_TYPE_BODY)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_TYPE_BODY)
        assertThat(locator.findMainObject("standalone", PlSqlGrammar.CREATE_PROCEDURE)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_PROCEDURE)
        assertThat(locator.findMainObject("calculate", PlSqlGrammar.CREATE_FUNCTION)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_FUNCTION)
        assertThat(locator.findMainObject("trigger_name", PlSqlGrammar.CREATE_TRIGGER)?.objectType)
            .isEqualTo(PlSqlGrammar.CREATE_TRIGGER)
    }

    @Test
    fun keepsMainAndTestObjectMetadataSeparate() {
        val mainFile = inputFile("CREATE PACKAGE p AS END p;", InputFile.Type.MAIN)
        val testFile = inputFile("CREATE PACKAGE p AS END p;", InputFile.Type.TEST)
        val mainLocations = mutableListOf<MappedObject>()
        val testLocations = mutableListOf<MappedObject>()

        AstScanner(
            emptyList(),
            null,
            true
        ).scanFile(
            SonarQubePlSqlFile(mainFile),
            listOf(ObjectLocationVisitor(SonarQubePlSqlFile(mainFile), mainLocations))
        )
        AstScanner(
            emptyList(),
            null,
            true
        ).scanFile(
            SonarQubePlSqlFile(testFile),
            listOf(ObjectLocationVisitor(SonarQubePlSqlFile(testFile), testLocations))
        )

        assertThat(testFile.type()).isEqualTo(InputFile.Type.TEST)
        assertThat(testLocations).hasSize(1)
        assertThat(testLocations.single().fileType).isEqualTo(PlSqlFile.Type.TEST)

        val locator = ObjectLocator()
        locator.setObjects(mainLocations + testLocations)

        assertThat(locator.findMainObject("p", PlSqlGrammar.CREATE_PACKAGE)?.fileType)
            .isEqualTo(PlSqlFile.Type.MAIN)
        assertThat(locator.findTestObject("p", PlSqlGrammar.CREATE_PACKAGE)?.fileType)
            .isEqualTo(PlSqlFile.Type.TEST)
    }

    private fun inputFile(source: String, type: InputFile.Type): InputFile =
        TestInputFileBuilder("module", "objects.sql")
            .setLanguage(PlSql.KEY)
            .setType(type)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(source)
            .build()
}
