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
package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.dynamicsql.DynamicSqlSupport
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.symbols.SymbolTableImpl
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.SymbolTable

open class PlSqlVisitorContext private constructor(private val rootTree: AstNode?,
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

    /** Installed by the scanner with the file's configuration and project; created on first use otherwise. */
    internal var dynamicSqlSupport: DynamicSqlSupport? = null

    private fun dynamicSqlSupport(): DynamicSqlSupport =
        dynamicSqlSupport ?: DynamicSqlSupport.standalone(plSqlFile).also { dynamicSqlSupport = it }

    /**
     * The SQL text that [expression] builds, parsed as one statement, or null when the expression has no string
     * literal, or the text does not parse as exactly one statement (with placeholders, or with every hole left out).
     *
     * [expression] is a node of this file, such as the text of `EXECUTE IMMEDIATE`, `OPEN … FOR` or `DBMS_SQL.PARSE`.
     * The operands of a concatenation are its parts; a string literal or `CHR(n)` gives known text, anything else is a
     * hole. Parsed only when asked, at most once per expression in a scan.
     */
    @ZpaExperimentalApi
    fun dynamicSql(expression: AstNode): DynamicSqlText? = dynamicSqlSupport().fromExpression(expression)

    /**
     * The SQL text built by joining [parts] in order, each a node of this file classified as in [dynamicSql]: for a
     * text gathered from several places, such as the assignments of a variable. Cached per list of nodes.
     */
    @ZpaExperimentalApi
    fun dynamicSql(parts: List<AstNode>): DynamicSqlText? = dynamicSqlSupport().fromParts(parts)

}
