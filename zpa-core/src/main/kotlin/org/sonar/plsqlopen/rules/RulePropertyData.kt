/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plsqlopen.rules

import org.sonar.plugins.plsqlopen.api.annotations.RuleProperty

class RulePropertyData(val key: String,
                       val description: String,
                       val defaultValue: String,
                       val type: String) {
    companion object {
        fun from(ruleProperty: RuleProperty?) =
            if (ruleProperty == null) null
            else
                RulePropertyData(ruleProperty.key,
                    ruleProperty.description,
                    ruleProperty.defaultValue,
                    ruleProperty.type)
    }
}
