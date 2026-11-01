//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.impl.ast.AstXmlPrinter
import com.felipebz.zpa.project.*
import com.felipebz.zpa.tooling.ProjectOverviewNode
import com.felipebz.zpa.tooling.SemanticInspectionResult
import com.felipebz.zpa.tooling.SemanticInspectionService
import com.felipebz.zpa.tooling.SymbolTreeNode
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.system.measureNanoTime

internal class SourceCodeModel(private val configurationModel: ZpaConfigurationModel) {
    lateinit var sourceCode: String
        private set
    lateinit var astNode: AstNode
        private set
    var parseTime: Long = 0
        private set

    private var semanticInspectionResult: SemanticInspectionResult? = null
    private val currentBufferFileId = FileId("zpa-toolkit://current-buffer")
    private var currentFileId: FileId = currentBufferFileId

    private val semanticInspectionService = SemanticInspectionService()

    fun setSourceCode(source: File, charset: Charset) {
        currentFileId = FileId(source.toPath().toAbsolutePath().normalize().toString())
        setSourceCode(source.readText(charset), currentFileId)
    }

    fun setSourceCode(sourceCode: String) {
        setSourceCode(sourceCode, currentFileId)
    }

    fun inspect(node: AstNode) = semanticInspectionResult?.inspect(node)

    val projectOverview: ProjectOverviewNode?
        get() = semanticInspectionResult?.projectOverview

    val symbolTree: SymbolTreeNode?
        get() = semanticInspectionResult?.symbolTree

    private fun setSourceCode(sourceCode: String, fileId: FileId) {
        semanticInspectionResult = null
        parseTime = measureNanoTime {
            astNode = configurationModel.parser.parse(sourceCode)
        }
        this.sourceCode = sourceCode
        val charset = configurationModel.charset
        val projectSources = configurationModel.projectSourceFiles
            .filter { it.toString() != fileId.value }
            .map { path -> ProjectSource(FileId(path.toString())) { Files.readString(path, charset) } }
        val preparation = ProjectIndexPreparation(ProjectDeclarationExtractor(configurationModel.configuration)).prepare(
            projectSources + ProjectSource(fileId) { sourceCode },
            concurrent = true
        )
        val result = semanticInspectionService.analyze(
            astNode,
            fileId,
            ProjectAnalysisContext.prepared(preparation),
            sourceCode
        )
        semanticInspectionResult = result
    }

    val xml: String
        get() = AstXmlPrinter.print(astNode)

}
