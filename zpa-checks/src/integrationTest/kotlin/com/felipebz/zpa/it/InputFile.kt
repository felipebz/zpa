//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
