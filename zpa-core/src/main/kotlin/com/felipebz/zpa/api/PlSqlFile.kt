//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import java.nio.file.Path

interface PlSqlFile {

    fun contents(): String

    fun fileName(): String

    fun path(): Path

    fun type(): Type

    enum class Type {
        MAIN, TEST
    }

}
