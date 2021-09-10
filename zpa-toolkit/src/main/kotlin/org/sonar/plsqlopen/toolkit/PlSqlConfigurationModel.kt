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
package org.sonar.plsqlopen.toolkit

import com.sonar.sslr.api.Grammar
import com.sonar.sslr.impl.Parser
import org.sonar.plsqlopen.parser.PlSqlParser
import org.sonar.plsqlopen.squid.PlSqlConfiguration
import org.sonar.plsqlopen.utils.log.Loggers
import org.sonar.sslr.toolkit.AbstractConfigurationModel
import org.sonar.sslr.toolkit.ConfigurationProperty
import org.sonar.sslr.toolkit.Validators
import java.nio.charset.Charset

class PlSqlConfigurationModel : AbstractConfigurationModel() {

    internal var charsetProperty = ConfigurationProperty("Charset",
        CHARSET_PROPERTY_KEY,
        getPropertyOrDefaultValue(CHARSET_PROPERTY_KEY, "UTF-8"),
        Validators.charsetValidator())

    internal var errorRecoveryProperty = ConfigurationProperty("Error recovery",
        ERROR_RECOVERY_PROPERTY_KEY,
        getPropertyOrDefaultValue(ERROR_RECOVERY_PROPERTY_KEY, "true"),
        Validators.booleanValidator())

    internal val configuration: PlSqlConfiguration
        get() = PlSqlConfiguration(Charset.forName(charsetProperty.value), java.lang.Boolean.valueOf(errorRecoveryProperty.value))

    override val charset: Charset
        get() = Charset.forName(charsetProperty.value)

    override val properties: List<ConfigurationProperty>
        get() = listOf(charsetProperty, errorRecoveryProperty)

    override fun doGetParser(): Parser<Grammar> = PlSqlParser.create(configuration)

    companion object {
        private val LOG = Loggers.getLogger(PlSqlConfigurationModel::class.java)
        private const val CHARSET_PROPERTY_KEY = "sonar.sourceEncoding"
        private const val ERROR_RECOVERY_PROPERTY_KEY = "sonar.zpa.errorRecoveryEnabled"

        internal fun getPropertyOrDefaultValue(propertyKey: String, defaultValue: String): String {
            val propertyValue = System.getProperty(propertyKey)

            if (propertyValue == null) {
                LOG.info("The property \"{}\" is not set, using the default value \"{}\".", propertyKey, defaultValue)
                return defaultValue
            }
            LOG.info("The property \"{}\" is set, using its value \"{}\".", propertyKey, propertyValue)
            return propertyValue
        }
    }

}
