//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.squid

object PlSqlCommentAnalyzer {

    fun getContents(comment: String) =
        when (comment[0]) {
            '-' -> {
                comment.substring(2)
            }
            '/' -> {
                comment.substring(2, comment.length - 2)
            }
            else -> {
                throw IllegalArgumentException()
            }
        }

}
