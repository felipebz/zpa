//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.PlSqlFile
import org.sonar.api.batch.fs.InputFile
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.toPath

class SonarQubePlSqlFile(val inputFile: InputFile) : PlSqlFile {

    override fun contents(): String =
        try {
            inputFile.contents()
        } catch (e: IOException) {
            throw IllegalStateException("Could not read contents of input file $inputFile", e)
        }

    override fun fileName(): String = inputFile.filename()

    override fun path(): Path = inputFile.uri().toPath()

    override fun type(): PlSqlFile.Type =
        when (inputFile.type()) {
            InputFile.Type.MAIN -> PlSqlFile.Type.MAIN
            InputFile.Type.TEST -> PlSqlFile.Type.TEST
            else -> PlSqlFile.Type.MAIN
        }

    override fun toString(): String = inputFile.toString()

}
