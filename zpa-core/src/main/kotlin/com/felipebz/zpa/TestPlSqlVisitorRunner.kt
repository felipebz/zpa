//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.symbols.local.LocalSemanticPrepass
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object TestPlSqlVisitorRunner {

    fun scanFile(file: File, metadata: FormsMetadata?, vararg visitors: PlSqlVisitor) {
        val context = createContext(TestPlSqlFile(file), metadata)
        // The same local pre-pass as the analysis pipeline, so tests and production run the same semantic passes; the
        // walk installs a not-prepared project analysis.
        val prepass = LocalSemanticPrepass(FileId(file.path))
        val walker = PlSqlAstWalker(listOf(prepass) + visitors)
        walker.walk(context)
    }

    private fun createContext(plSqlFile: PlSqlFile, metadata: FormsMetadata?): PlSqlVisitorContext {
        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
        val rootTree = parser.parse(plSqlFile.contents())
        return PlSqlVisitorContext(rootTree, plSqlFile, metadata)
    }

    private class TestPlSqlFile(private val file: File) : PlSqlFile {
        override fun contents(): String =
            try {
                file.readText()
            } catch (e: IOException) {
                throw IllegalStateException("Cannot read $file", e)
            }

        override fun fileName(): String = file.name

        override fun path(): Path = file.toPath()

        override fun type(): PlSqlFile.Type = PlSqlFile.Type.MAIN
    }

}
