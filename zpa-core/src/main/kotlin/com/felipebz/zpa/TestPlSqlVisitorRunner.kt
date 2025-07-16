/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa

import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlAstWalker
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.checks.PlSqlVisitor
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object TestPlSqlVisitorRunner {

    fun scanFile(file: File, metadata: FormsMetadata?, vararg visitors: PlSqlVisitor) {
        val context = createContext(TestPlSqlFile(file), metadata)
        val walker = PlSqlAstWalker(visitors.toList())
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
