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
package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import java.util.*

open class Symbol(val node: AstNode?,
                  val kind: Kind,
                  val scope: Scope,
                  datatype: PlSqlDatatype?,
                  name: String = "") {
    private val internalUsages = mutableListOf<AstNode>()
    private var internalModifiers = mutableListOf<AstNode>()

    enum class Kind(val value: String) {
        VARIABLE("variable"),
        PARAMETER("parameter"),
        CURSOR("cursor"),
        TYPE("type"),
        PACKAGE("package"),
        PROCEDURE("procedure"),
        FUNCTION("function"),
        TRIGGER("trigger"),
    }

    val declaration by lazy { node ?: throw IllegalStateException("Symbol must have a declaration") }

    val name: String = node?.tokenValue ?: name

    val type: PlSqlType = datatype?.type ?: PlSqlType.UNKNOWN

    val datatype: PlSqlDatatype = datatype ?: UnknownDatatype

    val modifiers: List<AstNode>
        get() = Collections.unmodifiableList(internalModifiers)

    val usages: List<AstNode>
        get() = Collections.unmodifiableList(internalUsages)

    val isGlobal: Boolean = if (kind in arrayOf(Kind.VARIABLE, Kind.CURSOR, Kind.TYPE)) {
        scope.isGlobal && scope.type == PlSqlGrammar.CREATE_PACKAGE
    } else {
        scope.isGlobal
    }

    var innerScope: Scope? = null

    fun hasModifier(modifier: String): Boolean {
        for (syntaxToken in modifiers) {
            if (syntaxToken.tokenValue.equals(modifier, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun addModifiers(modifiers: List<AstNode>) {
        internalModifiers.addAll(modifiers)
    }

    fun addUsage(usage: AstNode) {
        internalUsages.add(usage)
    }

    fun `is`(kind: Kind) = kind == this.kind

    fun called(name: String) = if (name.startsWith('"')) {
        name == this.name
    } else {
        name.equals(this.name, ignoreCase = true)
    }

    override fun toString() = "Symbol name=$name kind=$kind datatype=$datatype"
}
