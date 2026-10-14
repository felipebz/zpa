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
package com.felipebz.zpa.oracle

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.oracle.metadata.OracleMetadataAcquisition
import com.felipebz.zpa.oracle.metadata.OracleMetadataException
import com.felipebz.zpa.utils.log.Loggers
import org.sonar.api.utils.MessageException
import java.io.PrintWriter
import java.net.URLClassLoader
import java.sql.Connection
import java.sql.Driver
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.*
import java.util.logging.Logger
import javax.sql.DataSource

/** Detached metadata of one acquisition, with a log-safe description of its provenance. */
@OptIn(ZpaExperimentalApi::class)
internal class AcquiredOracleMetadata(val metadata: ProjectExternalMetadata, val summary: String)

/** Acquires Oracle metadata once per analysis. Failures are [MessageException]s with sanitized messages. */
internal fun interface OracleMetadataAcquirer {
    fun acquire(settings: OracleMetadataSettings): AcquiredOracleMetadata
}

/**
 * Loads the JDBC driver with standard Java APIs and acquires the metadata in the scanner process.
 *
 * SonarQube loads this plugin in an isolated class loader, so a driver on the scanner's class path is generally not
 * visible to it, and `DriverManager` would ignore drivers of other class loaders anyway. The driver is therefore
 * looked up with [ServiceLoader] and used directly: from [OracleMetadataSettings.driverJar] through a
 * [URLClassLoader] whose parent is the plugin class loader, or else from the plugin-visible class path. That loader
 * stays open (and is the thread context class loader) for the whole acquisition and is closed afterwards; only the
 * detached metadata outlives it.
 */
@OptIn(ZpaExperimentalApi::class)
internal class JdbcOracleMetadataAcquirer : OracleMetadataAcquirer {
    private val logger = Loggers.getLogger(JdbcOracleMetadataAcquirer::class.java)

    override fun acquire(settings: OracleMetadataSettings): AcquiredOracleMetadata {
        val parent = JdbcOracleMetadataAcquirer::class.java.classLoader
        val driverLoader = settings.driverJar?.let { URLClassLoader(arrayOf(it.toUri().toURL()), parent) }
        try {
            val loader = driverLoader ?: parent
            val thread = Thread.currentThread()
            val previousContextLoader = thread.contextClassLoader
            thread.contextClassLoader = loader
            try {
                val driver = findDriver(settings, loader)
                return acquire(settings, driver)
            } finally {
                thread.contextClassLoader = previousContextLoader
            }
        } finally {
            driverLoader?.close()
        }
    }

    private fun findDriver(settings: OracleMetadataSettings, loader: ClassLoader): Driver {
        val providers = ServiceLoader.load(Driver::class.java, loader).iterator()
        var unloadable = 0
        try {
            while (true) {
                val driver = try {
                    if (!providers.hasNext()) break
                    providers.next()
                } catch (e: ServiceConfigurationError) {
                    // A broken provider entry must not hide a working driver.
                    unloadable++
                    continue
                } catch (e: LinkageError) {
                    // A provider class compiled for a newer Java or missing its dependencies: equally skipped.
                    unloadable++
                    continue
                }
                if (driver.acceptsURL(settings.url)) return driver
            }
        } catch (e: SQLException) {
            throw failure("JDBC driver lookup failed${errorCode(e)}: ${redact(e.message, settings)}")
        } catch (e: RuntimeException) {
            throw failure("JDBC driver lookup failed: ${redact(e.message, settings)}")
        } catch (e: LinkageError) {
            throw failure("the JDBC driver is incompatible with this scanner (${e.javaClass.simpleName}: ${redact(e.message, settings)})")
        }
        val where = settings.driverJar?.let { "in $it" }
            ?: "visible to the plugin; set ${OracleMetadataSettings.DRIVER_PATH_KEY} to the Oracle JDBC driver JAR (ojdbc17.jar)"
        val skipped = if (unloadable == 0) "" else " $unloadable JDBC driver provider(s) could not be loaded (broken or incompatible with this Java version) and were skipped."
        throw failure("no JDBC driver accepting ${OracleMetadataSettings.URL_KEY} was found $where.$skipped")
    }

    private fun acquire(settings: OracleMetadataSettings, driver: Driver): AcquiredOracleMetadata {
        logger.info("ZPA Oracle metadata: reading schemas ${settings.schemas} and relations " +
            "${settings.relations.map { "${it.first}.${it.second}" }} as ${settings.user}")
        val result = try {
            OracleMetadataAcquisition.acquire(DriverDataSource(driver, settings), settings.request())
        } catch (e: OracleMetadataException) {
            throw failure("acquisition failed: ${redact(e.message, settings)}")
        } catch (e: SQLException) {
            throw failure("acquisition failed${errorCode(e)}: ${redact(e.message, settings)}")
        } catch (e: RuntimeException) {
            throw failure("acquisition failed: ${redact(e.message, settings)}")
        }
        val summary = "${result.relationCount} relations, ${result.columnCount} columns from ${result.database}/${result.container} " +
            "as ${result.sessionUser}; complete schemas ${result.completeSchemas}" +
            (if (result.notVisible.isEmpty()) "" else "; not visible ${result.notVisible}")
        return AcquiredOracleMetadata(result.metadata, summary)
    }

    /** Non-pooled, non-transactional: every call opens a new connection, in auto-commit mode as the driver opens it. */
    private class DriverDataSource(private val driver: Driver, private val settings: OracleMetadataSettings) : DataSource {
        override fun getConnection(): Connection = getConnection(settings.user, settings.password)

        override fun getConnection(username: String, password: String): Connection {
            val properties = Properties()
            properties["user"] = username
            properties["password"] = password
            return driver.connect(settings.url, properties) ?: throw SQLException("the JDBC driver did not accept the URL")
        }

        override fun getLogWriter(): PrintWriter? = null
        override fun setLogWriter(out: PrintWriter?) = Unit
        override fun setLoginTimeout(seconds: Int) = Unit
        override fun getLoginTimeout(): Int = 0
        override fun getParentLogger(): Logger = throw SQLFeatureNotSupportedException()
        override fun <T : Any?> unwrap(iface: Class<T>): T = throw SQLException("not a wrapper")
        override fun isWrapperFor(iface: Class<*>): Boolean = false
        override fun toString(): String = "DriverDataSource(user=${settings.user})"
    }

    companion object {
        private fun failure(message: String) = MessageException.of("ZPA Oracle metadata: $message")

        private fun errorCode(e: SQLException) = if (e.errorCode != 0) " (error ${e.errorCode})" else ""

        /** Removes the URL, the password and any `user/password@` fragment from a driver message. */
        fun redact(message: String?, settings: OracleMetadataSettings): String {
            var text = message ?: "unknown error"
            text = text.replace(settings.url, "<oracle-url>")
            if (settings.password.isNotEmpty()) text = text.replace(settings.password, "***")
            return text.replace(Regex("[^\\s/@:]+/[^\\s@]+@"), "***@")
        }
    }
}
