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
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.impl.ast.AstXmlPrinter
import com.felipebz.flr.toolkit.ConfigurationModel
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.tooling.SemanticInspectionResult
import com.felipebz.zpa.tooling.SemanticInspectionService
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

    private var semanticInspectionResult: SemanticInspectionResult? = null
    private val currentBufferFileId = FileId("zpa-toolkit://current-buffer")

    private val semanticInspectionService = SemanticInspectionService()

    fun setSourceCode(source: File, charset: Charset) {
        val fileId = FileId(source.toPath().toAbsolutePath().normalize().toString())
        setSourceCode(source.readText(charset), fileId)
    }

    fun setSourceCode(sourceCode: String) {
        setSourceCode(sourceCode, currentBufferFileId)
    }

    fun inspect(node: AstNode) = semanticInspectionResult?.inspect(node)

    private fun setSourceCode(sourceCode: String, fileId: FileId) {
        semanticInspectionResult = null
        parseTime = measureNanoTime {
            astNode = configurationModel.parser.parse(sourceCode)
        }
        this.sourceCode = sourceCode
        val preparation = ProjectIndexPreparation().prepare(
            listOf(ProjectSource(fileId) { sourceCode }),
            concurrent = false
        )
        val result = semanticInspectionService.analyze(
            astNode,
            fileId,
            ProjectAnalysisContext.prepared(preparation)
        )
        semanticInspectionResult = result
        symbolTable = result.symbolTable
    }

    val xml: String
        get() = AstXmlPrinter.print(astNode)

}
