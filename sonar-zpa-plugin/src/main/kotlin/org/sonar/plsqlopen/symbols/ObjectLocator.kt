/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import com.felipebz.flr.api.AstNodeType
import org.sonar.api.scanner.ScannerSide
import org.sonar.plsqlopen.squid.SonarQubePlSqlFile
import org.sonar.plugins.plsqlopen.api.symbols.Scope

@ScannerSide
class ObjectLocator {
    private var scope: Scope = ScopeImpl()

    private val mappedObjects
        get() = scope.innerScopes.map {
            val plSqlFile = it.plSqlFile as SonarQubePlSqlFile? ?: return@map null
            val identifier = it.identifier ?: return@map null
            val type = it.type ?: return@map null
            MappedObject(identifier, type, plSqlFile.type(), plSqlFile.path(), plSqlFile.inputFile)
        }.filterNotNull()

    fun setScope(scope: Scope) {
        this.scope = scope
    }

    fun findMainObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isMain && it.identifier == identifier && it.objectType in types }
    }

    fun findTestObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isTest && it.identifier == identifier && it.objectType in types }
    }
}
