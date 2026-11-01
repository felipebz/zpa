//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
import java.util.concurrent.atomic.AtomicBoolean
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
    private val thread: Thread = Thread(this, threadName).apply { isDaemon = true }
    private var success = false
    private val interrupted = AtomicBoolean().apply { set(false) }

    override fun run() {
        while (!(interrupted.get() || Thread.currentThread().isInterrupted)) {
            try {
                Thread.sleep(period)
                lock.withLock {
                    log("$currentFileNumber/$count files analyzed, current file: $currentFile")
                }
            } catch (e: InterruptedException) {
                interrupted.set(true)
                thread.interrupt()
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
        interrupted.set(true)
        success = true
        thread.interrupt()
        join()
    }

    @Synchronized
    fun cancel() {
        interrupted.set(true)
        thread.interrupt()
        join()
    }

    fun join() {
        try {
            thread.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private fun log(message: String) {
        lock.withLock {
            logger.info(message)
            condition.signalAll()
        }
    }

}
