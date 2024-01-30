/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.io.File
import java.nio.charset.StandardCharsets

class SonarQubeSymbolTableTest {

    @Test
    fun test() {
        val inputFile = TestInputFileBuilder("key", File("test.sql").path)
                .setContents("abcde")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .build()
        val key = inputFile.key()

        val context = SensorContextTester.create(File(""))
        context.fileSystem().add(inputFile)

        val symbol = createSymbol(createNode(1))
        symbol.addUsage(createNode(2))
        symbol.addUsage(createNode(3))

        val symbolTable = SonarQubeSymbolTable(context, inputFile)
        symbolTable.save(listOf(symbol))

        assertThat(context.referencesForSymbolAt(key, 1, 1))
                .extracting("start.line", "start.lineOffset")
                .containsExactly(tuple(1, 2), tuple(1, 3))
    }

    private fun createNode(character: Int): AstNode {
        val token = Token.builder()
                .setLine(1)
                .setColumn(character)
                .setValueAndOriginalValue(" ")
                .setType(GenericTokenType.IDENTIFIER)
                .build()

        val lastToken = Token.builder()
                .setLine(1)
                .setColumn(character + 2)
                .setValueAndOriginalValue(" ")
                .setType(GenericTokenType.IDENTIFIER)
                .build()

        val node = AstNode(token)
        node.addChild(AstNode(token))
        node.addChild(AstNode(lastToken))

        return node
    }

    private fun createSymbol(node: AstNode): Symbol {
        return Symbol(node, Symbol.Kind.VARIABLE, mock(Scope::class.java), null)
    }

}
