/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.util.log

import org.sonar.api.utils.log.Logger

class SonarQubeLogger internal constructor(private val logger: Logger) : org.sonar.plsqlopen.utils.log.Logger {

    override fun trace(msg: String) {
        logger.trace(msg)
    }

    override fun trace(msg: String, vararg args: Any?) {
        logger.trace(msg, *args)
    }

    override fun info(msg: String) {
        logger.info(msg)
    }

    override fun info(msg: String, vararg args: Any?) {
        logger.info(msg, *args)
    }

    override fun warn(msg: String) {
        logger.warn(msg)
    }

    override fun warn(msg: String, vararg args: Any?) {
        logger.warn(msg, *args)
    }

    override fun error(msg: String) {
        logger.error(msg)
    }

    override fun error(msg: String, vararg args: Any?) {
        logger.error(msg, *args)
    }

}
