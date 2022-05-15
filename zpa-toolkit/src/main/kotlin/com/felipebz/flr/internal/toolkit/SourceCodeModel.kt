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
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

internal class SourceCodeModel(private val configurationModel: ConfigurationModel) {
    lateinit var sourceCode: String
        private set
    lateinit var astNode: AstNode
        private set

    fun setSourceCode(source: File, charset: Charset) {
        astNode = configurationModel.parser.parse(source)
        try {
            sourceCode = String(Files.readAllBytes(Paths.get(source.path)), charset)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun setSourceCode(sourceCode: String) {
        astNode = configurationModel.parser.parse(sourceCode)
        this.sourceCode = sourceCode
    }

    val xml: String
        get() = AstXmlPrinter.print(astNode)
}
