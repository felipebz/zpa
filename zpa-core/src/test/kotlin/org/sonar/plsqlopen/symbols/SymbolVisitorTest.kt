/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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

import org.assertj.core.api.Assertions.*
import org.assertj.core.groups.Tuple
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.io.File

class SymbolVisitorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun scanFile(eol: String) {
        val baseDir = temp.newFolder()
        val file = File(baseDir, "test.sql")
        val content = File("src/test/resources/symbols/symbols.sql").readText()
        file.writeText(content.replace("\r\n", "\n").replace("\n", eol))

        val visitor = SymbolVisitor(DefaultTypeSolver())
        TestPlSqlVisitorRunner.scanFile(file, null, visitor)

        val symbols = visitor.symbols

        assertThat(symbols).hasSize(10)

        assertThat(symbols.referencesForSymbolAt(2, 3)).containsExactly(tuple(4, offset(3)))
        assertThat(symbols.referencesForSymbolAt(6, 7)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(11, 22)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(12, 10)).containsExactly(tuple(15, offset(8)))
        assertThat(symbols.referencesForSymbolAt(12, 14)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(18, 21)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(24, 3)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(28, 3)).isEmpty()
        assertThat(symbols.referencesForSymbolAt(32, 3)).containsExactly(tuple(36, offset(8)))
        assertThat(symbols.referencesForSymbolAt(41, 3)).containsExactly(tuple(43, offset(3)))
    }

    private fun List<Symbol>.referencesForSymbolAt(line: Int, column: Int): List<Tuple> {
        val symbol = this.firstOrNull {
            it.declaration().token.line == line && it.declaration().token.column == offset(column)
        }

        if (symbol == null) {
            fail("No symbol was found at line $line and column ${offset(column)}")
        } else {
            return symbol.usages().map { tuple(it.token.line, it.token.column) }
        }
        return emptyList()
    }

    @Test
    fun shouldAnalyse_lf() {
        scanFile("\n")
    }

    @Test
    fun shouldAnalyse_crlf() {
        scanFile("\r\n")
    }

    @Test
    fun shouldAnalyse_cr() {
        scanFile("\r")
    }

    private fun offset(offset: Int): Int {
        return offset - 1
    }

}
