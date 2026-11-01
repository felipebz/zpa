//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utils.log

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

    override fun debug(msg: String) {
        logger.fine(msg)
    }

    override fun debug(msg: String, vararg args: Any?) {
        logger.fine(format(msg, args))
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
