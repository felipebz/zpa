//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.log

import org.slf4j.Logger

class SonarQubeLogger internal constructor(private val logger: Logger) : com.felipebz.zpa.utils.log.Logger {

    override fun trace(msg: String) {
        logger.trace(msg)
    }

    override fun trace(msg: String, vararg args: Any?) {
        logger.trace(msg, *args)
    }

    override fun debug(msg: String) {
        logger.debug(msg)
    }

    override fun debug(msg: String, vararg args: Any?) {
        logger.debug(msg, *args)
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
