//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utils.log

interface Logger {

    fun trace(msg: String)

    fun trace(msg: String, vararg args: Any?)

    fun debug(msg: String)

    fun debug(msg: String, vararg args: Any?)

    fun info(msg: String)

    fun info(msg: String, vararg args: Any?)

    fun warn(msg: String)

    fun warn(msg: String, vararg args: Any?)

    fun error(msg: String)

    fun error(msg: String, vararg args: Any?)

}
