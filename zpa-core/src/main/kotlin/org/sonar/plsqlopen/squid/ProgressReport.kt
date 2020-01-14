/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
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
package org.sonar.plsqlopen.squid

import org.sonar.plsqlopen.utils.log.Logger
import org.sonar.plsqlopen.utils.log.Loggers
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ProgressReport @JvmOverloads constructor(threadName: String,
                                               private val period: Long,
                                               private val logger: Logger = Loggers.getLogger(ProgressReport::class.java)) :
    Runnable {

    internal val lock = ReentrantLock()
    internal val condition: Condition = lock.newCondition()

    private var count: Int = 0
    private var currentFileNumber = -1
    private var currentFile: String? = null
    private lateinit var iterator: Iterator<String>
    private val thread: Thread = Thread(this)
    private var success = false

    init {
        thread.name = threadName
        thread.isDaemon = true
    }

    override fun run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(period)
                lock.withLock {
                    log("$currentFileNumber/$count files analyzed, current file: $currentFile")
                }
            } catch (e: InterruptedException) {
                break
            }

        }

        lock.withLock {
            if (success) {
                log("$count/$count source files have been analyzed")
            }
        }
    }

    @Synchronized
    fun start(files: Collection<String>) {
        count = files.size
        iterator = files.iterator()

        nextFile()

        log("$count source files to be analyzed")
        thread.start()
    }

    @Synchronized
    fun nextFile() {
        if (iterator.hasNext()) {
            currentFileNumber++
            currentFile = iterator.next()
        }
    }

    @Synchronized
    fun stop() {
        success = true
        thread.interrupt()
    }

    @Synchronized
    fun cancel() {
        thread.interrupt()
    }

    @Throws(InterruptedException::class)
    fun join() {
        thread.join()
    }

    private fun log(message: String) {
        lock.withLock {
            logger.info(message)
            condition.signalAll()
        }
    }

}
