/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.utils.log

class ZpaLogger(private val logger: java.util.logging.Logger) : Logger {

    private fun format(msg: String, args: Array<out Any?>): String {
        var result = msg
        for (arg in args) {
            result = result.replaceFirst("{}", arg.toString())
        }
        return result
    }

    override fun trace(msg: String) {
        logger.finest(msg)
    }

    override fun trace(msg: String, vararg args: Any?) {
        logger.finest(format(msg, args))
    }

    override fun info(msg: String) {
        logger.info(msg)
    }

    override fun info(msg: String, vararg args: Any?) {
        logger.info(format(msg, args))
    }

    override fun warn(msg: String) {
        logger.warning(msg)
    }

    override fun warn(msg: String, vararg args: Any?) {
        logger.warning(format(msg, args))
    }

    override fun error(msg: String) {
        logger.severe(msg)
    }

    override fun error(msg: String, vararg args: Any?) {
        logger.severe(format(msg, args))
    }

}
