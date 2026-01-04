/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
