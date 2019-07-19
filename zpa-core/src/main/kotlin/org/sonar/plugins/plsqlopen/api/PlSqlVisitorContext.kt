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
package org.sonar.plugins.plsqlopen.api

import com.sonar.sslr.api.AstNode
import com.sonar.sslr.api.RecognitionException
import org.sonar.plsqlopen.metadata.FormsMetadata
import org.sonar.plsqlopen.symbols.SymbolTableImpl
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable

class PlSqlVisitorContext private constructor(private val rootTree: AstNode?,
                                              private val plSqlFile: PlSqlFile?,
                                              private val parsingException: RecognitionException?,
                                              var formsMetadata: FormsMetadata?) {
    var symbolTable: SymbolTable = SymbolTableImpl()
    var currentScope: Scope? = null

    constructor(rootTree: AstNode,
                plsqlFile: PlSqlFile?,
                metadata: FormsMetadata?) :
        this(rootTree, plsqlFile, null, metadata)

    constructor(plsqlFile: PlSqlFile?,
                parsingException: RecognitionException,
                metadata: FormsMetadata?) :
        this(null, plsqlFile, parsingException, metadata)

    fun rootTree() = rootTree

    fun plSqlFile() = plSqlFile

    fun parsingException() = parsingException

}
