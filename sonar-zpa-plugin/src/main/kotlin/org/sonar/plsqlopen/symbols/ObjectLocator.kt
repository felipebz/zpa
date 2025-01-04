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
package org.sonar.plsqlopen.symbols

import com.felipebz.flr.api.AstNodeType
import org.sonar.api.scanner.ScannerSide
import org.sonar.plsqlopen.squid.SonarQubePlSqlFile
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.symbols.Scope

@ScannerSide
class ObjectLocator {
    private var scope: Scope = ScopeImpl()

    private val mappedObjects
        get() = scope.innerScopes
            .union(scope.innerScopes
                .flatMap { it.innerScopes }
                .filter { it.type == PlSqlGrammar.CREATE_PACKAGE_BODY || it.type == PlSqlGrammar.CREATE_TYPE_BODY })
            .map {
                val plSqlFile = it.plSqlFile as SonarQubePlSqlFile? ?: return@map null
                val identifier = it.identifier ?: return@map null
                val type = it.type ?: return@map null
                val firstLine = it.firstToken?.line ?: return@map null
                val lastLine = it.lastToken?.line ?: return@map null
                MappedObject(
                    identifier,
                    type,
                    plSqlFile.type(),
                    plSqlFile.path(),
                    plSqlFile.inputFile,
                    firstLine,
                    lastLine
                )
            }.filterNotNull()

    fun setScope(scope: Scope) {
        this.scope = scope
    }

    fun findMainObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isMain && it.identifier.equals(identifier, ignoreCase = true) && it.objectType in types }
    }

    fun findTestObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isTest && it.identifier.equals(identifier, ignoreCase = true) && it.objectType in types }
    }
}
