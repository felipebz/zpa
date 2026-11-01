//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.PlSqlFile
import org.sonar.api.batch.fs.InputFile
import java.nio.file.Path

data class MappedObject(
    val identifier: String,
    val objectType: AstNodeType,
    val fileType: PlSqlFile.Type,
    val path: Path,
    val inputFile: InputFile,
    val firstLine: Int = -1,
    val lastLine: Int = -1
) {
    val isMain: Boolean
        get() = fileType === PlSqlFile.Type.MAIN
    val isTest: Boolean
        get() = fileType === PlSqlFile.Type.TEST
}
