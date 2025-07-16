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
package com.felipebz.zpa.it

import com.felipebz.zpa.api.PlSqlFile
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

class InputFile(private val type: PlSqlFile.Type,
                baseDirPath: Path,
                private val file: File,
                private val charset: Charset
) : PlSqlFile {

    override fun contents(): String =
        file.inputStream().use {
            return it.bufferedReader(charset).use { r -> r.readText() }
        }

    override fun fileName(): String  = file.name

    override fun path(): Path = file.toPath()

    override fun type(): PlSqlFile.Type = type

    val pathRelativeToBase: String = baseDirPath.relativize(path()).invariantSeparatorsPathString

    override fun hashCode(): Int {
        return file.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is InputFile) return false
        return file == other.file
    }

    override fun toString(): String {
        return pathRelativeToBase
    }

}
