/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2010-2021 SonarSource SA
 * Copyright (C) 2021-2021 Felipe Zorzo
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
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.impl.ast.AstXmlPrinter
import com.felipebz.flr.toolkit.ConfigurationModel
import org.sonar.plsqlopen.getSemanticNode
import org.sonar.plsqlopen.symbols.DefaultTypeSolver
import org.sonar.plsqlopen.symbols.SymbolVisitor
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable
import java.io.File
import java.nio.charset.Charset
import kotlin.system.measureNanoTime

internal class SourceCodeModel(private val configurationModel: ConfigurationModel) {
    lateinit var sourceCode: String
        private set
    lateinit var astNode: AstNode
        private set
    lateinit var symbolTable: SymbolTable
        private set
    var parseTime: Long = 0
        private set

    fun setSourceCode(source: File, charset: Charset) {
        setSourceCode(source.readText(charset))
    }

    fun setSourceCode(sourceCode: String) {
        parseTime = measureNanoTime {
            astNode = getSemanticNode(configurationModel.parser.parse(sourceCode))
        }
        this.sourceCode = sourceCode
        loadSymbolTable()
    }

    val xml: String
        get() = AstXmlPrinter.print(astNode)

    private fun loadSymbolTable() {
        val symbolVisitor = SymbolVisitor(DefaultTypeSolver())
        symbolVisitor.context = PlSqlVisitorContext(astNode, null, null)
        symbolVisitor.init()
        symbolVisitor.visitFile(astNode)
        symbolTable = symbolVisitor.symbolTable
    }
}
