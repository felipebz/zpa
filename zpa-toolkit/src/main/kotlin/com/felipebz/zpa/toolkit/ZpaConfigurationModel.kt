/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.toolkit

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.impl.Parser
import com.felipebz.flr.toolkit.AbstractConfigurationModel
import com.felipebz.flr.toolkit.ConfigurationProperty
import com.felipebz.flr.toolkit.Validators
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.utils.log.Loggers
import java.nio.charset.Charset

class ZpaConfigurationModel : AbstractConfigurationModel() {

    internal var charsetProperty = ConfigurationProperty("Charset",
        CHARSET_PROPERTY_KEY,
        getPropertyOrDefaultValue(CHARSET_PROPERTY_KEY, "UTF-8"),
        Validators.charsetValidator())

    internal var errorRecoveryProperty = ConfigurationProperty("Error recovery",
        ERROR_RECOVERY_PROPERTY_KEY,
        getPropertyOrDefaultValue(ERROR_RECOVERY_PROPERTY_KEY, "true"),
        Validators.booleanValidator())

    internal val configuration: PlSqlConfiguration
        get() = PlSqlConfiguration(Charset.forName(charsetProperty.value), errorRecoveryProperty.value.toBoolean())

    override val charset: Charset
        get() = Charset.forName(charsetProperty.value)

    override val properties: List<ConfigurationProperty>
        get() = listOf(charsetProperty, errorRecoveryProperty)

    override fun doGetParser(): Parser<Grammar> = PlSqlParser.create(configuration)

    companion object {
        private val LOG = Loggers.getLogger(ZpaConfigurationModel::class.java)
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
