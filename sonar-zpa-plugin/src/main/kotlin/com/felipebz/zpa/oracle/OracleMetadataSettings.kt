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
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.oracle.metadata.OracleMetadataRequest
import org.sonar.api.config.Configuration
import org.sonar.api.utils.MessageException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Validated settings for Oracle metadata acquisition. [url] and [password] are secret-bearing: they are never part of
 * [toString], log lines or failure messages.
 *
 * The `sonar.zpa.oracle.*` keys are analysis parameters supplied by the analysis pipeline (sonar-project.properties or
 * `-D`), read from the scanner [Configuration]. They are intentionally not declared as `PropertyDefinition`s: a
 * declared property is offered in the server's settings (globally by default), where connection details do not
 * belong. The password is never a property, only the name of an environment variable.
 */
@OptIn(ZpaExperimentalApi::class)
internal class OracleMetadataSettings(
    val url: String,
    val user: String,
    val password: String,
    val schemas: List<String>,
    val relations: List<Pair<String, String>>,
    /** An explicitly configured driver JAR, or null to use the drivers visible to the plugin. */
    val driverJar: Path?
) {
    fun request(): OracleMetadataRequest {
        val builder = OracleMetadataRequest.builder()
        schemas.forEach { builder.schema(it) }
        relations.forEach { (schema, name) -> builder.relation(schema, name) }
        return builder.build()
    }

    override fun toString(): String = "OracleMetadataSettings(user=$user, schemas=$schemas, relations=$relations, driverJar=$driverJar)"

    companion object {
        const val URL_KEY = "sonar.zpa.oracle.url"
        const val USER_KEY = "sonar.zpa.oracle.user"
        const val PASSWORD_ENV_KEY = "sonar.zpa.oracle.passwordEnv"
        const val SCHEMAS_KEY = "sonar.zpa.oracle.schemas"
        const val RELATIONS_KEY = "sonar.zpa.oracle.relations"
        const val COMPILATION_SCHEMA_KEY = "sonar.zpa.oracle.compilationSchema"
        const val DRIVER_PATH_KEY = "sonar.zpa.oracle.driverPath"
        const val DEFAULT_PASSWORD_ENV = "ZPA_ORACLE_PASSWORD"

        /** `jdbc:oracle:thin:user/password@...` or a `password=` connection property. */
        private val CREDENTIALS_IN_URL = Regex("(?:^jdbc:oracle:[a-z0-9]+:[^@]*/[^@]*@)|(?:password\\s*=)", RegexOption.IGNORE_CASE)

        private fun failure(message: String) = MessageException.of("ZPA Oracle metadata: $message")

        /**
         * The settings when acquisition is requested ([URL_KEY] is set), null otherwise: without it nothing connects.
         * Any other acquisition setting without [URL_KEY] is a configuration error.
         *
         * @throws MessageException for invalid or incomplete settings
         */
        fun from(configuration: Configuration, environment: (String) -> String?, baseDir: Path): OracleMetadataSettings? {
            val url = configuration.get(URL_KEY).orElse("").trim()
            val user = configuration.get(USER_KEY).orElse("").trim()
            val schemaValues = list(configuration, SCHEMAS_KEY)
            val relationValues = list(configuration, RELATIONS_KEY)
            val driverPath = configuration.get(DRIVER_PATH_KEY).orElse("").trim()
            if (url.isEmpty()) {
                if (user.isNotEmpty() || schemaValues.isNotEmpty() || relationValues.isNotEmpty() || driverPath.isNotEmpty()) {
                    throw failure("$USER_KEY, $SCHEMAS_KEY, $RELATIONS_KEY and $DRIVER_PATH_KEY require $URL_KEY")
                }
                return null
            }
            if (CREDENTIALS_IN_URL.containsMatchIn(url)) {
                // The URL is deliberately not echoed: it carries a credential.
                throw failure("$URL_KEY must not contain credentials; use $USER_KEY and the password environment variable")
            }
            if (user.isEmpty()) throw failure("$URL_KEY requires $USER_KEY")
            val schemas = schemaValues.onEach { requireDictionaryName(it, SCHEMAS_KEY) }.distinct()
            val relations = relationValues.map { relation ->
                val parts = relation.split('.')
                if (parts.size != 2 || parts.any { it.isEmpty() }) {
                    throw failure("invalid $RELATIONS_KEY entry '$relation': expected SCHEMA.RELATION")
                }
                parts.forEach { requireDictionaryName(it, RELATIONS_KEY) }
                parts[0] to parts[1]
            }.distinct()
            if (schemas.isEmpty() && relations.isEmpty()) throw failure("$URL_KEY requires $SCHEMAS_KEY or $RELATIONS_KEY")
            val variable = configuration.get(PASSWORD_ENV_KEY).orElse(DEFAULT_PASSWORD_ENV).trim()
            if (variable.isEmpty()) throw failure("$PASSWORD_ENV_KEY must name an environment variable")
            val password = environment(variable)
            if (password.isNullOrEmpty()) {
                throw failure("the password environment variable '$variable' is not set in the scanner environment")
            }
            val driverJar = driverPath.takeIf { it.isNotEmpty() }?.let { baseDir.resolve(it).normalize() }?.also {
                if (!Files.isRegularFile(it)) throw failure("$DRIVER_PATH_KEY does not name a JAR file: $it")
            }
            return OracleMetadataSettings(url, user, password, schemas, relations, driverJar)
        }

        /** The configured default compilation schema as an exact identifier, or null. Independent of acquisition. */
        fun compilationSchema(configuration: Configuration): ProjectIdentifier? {
            val name = configuration.get(COMPILATION_SCHEMA_KEY).orElse("").trim()
            if (name.isEmpty()) return null
            requireDictionaryName(name, COMPILATION_SCHEMA_KEY)
            return ProjectIdentifier.fromSource("\"$name\"")
        }

        /** A comma-separated parameter; undeclared keys are not multi-valued for the platform, so it is split here. */
        private fun list(configuration: Configuration, key: String): List<String> =
            configuration.get(key).orElse("").split(',').map { it.trim() }.filter { it.isNotEmpty() }

        private fun requireDictionaryName(name: String, key: String) {
            if (name.contains('"') || name.any { it.isWhitespace() }) {
                throw failure("invalid $key entry '$name': use exact dictionary names (e.g. HR), without quotes")
            }
        }
    }
}
