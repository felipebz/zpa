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
package org.sonar.plsqlopen

import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.parser.PlSqlParser
import org.sonar.plsqlopen.squid.PlSqlAstWalker
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plugins.plsqlopen.api.PlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object TestPlSqlVisitorRunner {

    fun scanFile(file: File, metadata: FormsMetadata?, vararg visitors: PlSqlVisitor) {
        val context = createContext(TestPlSqlFile.fromFile(file), metadata)
        val walker = PlSqlAstWalker(visitors.toList())
        walker.walk(context)
    }

    private fun createContext(plSqlFile: PlSqlFile, metadata: FormsMetadata?): PlSqlVisitorContext {
        val parser = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8))
        val rootTree = getSemanticNode(parser.parse(plSqlFile.contents()))
        return PlSqlVisitorContext(rootTree, plSqlFile, metadata)
    }

    private class TestPlSqlFile private constructor(private val contents: String,
                                                    private val name: String) : PlSqlFile {
        override fun contents(): String = contents
        override fun fileName(): String = name
        override fun type(): PlSqlFile.Type = PlSqlFile.Type.MAIN

        companion object {
            fun fromFile(file: File): PlSqlFile =
                try {
                    TestPlSqlFile(String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), file.name)
                } catch (e: IOException) {
                    throw IllegalStateException("Cannot read $file", e)
                }
        }
    }

}
