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
package org.sonar.plsqlopen.squid

import org.sonar.api.batch.fs.InputFile
import org.sonar.plugins.plsqlopen.api.PlSqlFile

import java.io.IOException

class SonarQubePlSqlFile private constructor(private val inputFile: InputFile) : PlSqlFile {

    override fun fileName(): String = inputFile.filename()

    override fun contents(): String =
        try {
            inputFile.contents()
        } catch (e: IOException) {
            throw IllegalStateException("Could not read contents of input file $inputFile", e)
        }

    companion object {
        fun create(inputFile: InputFile): PlSqlFile {
            return SonarQubePlSqlFile(inputFile)
        }
    }

}
