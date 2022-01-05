/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.symbols

import com.felipebz.flr.api.AstNode
import java.util.*

open class Symbol(private val declaration: AstNode, private val kind: Kind, private val scope: Scope, type: PlSqlType?) {

    private val name: String = declaration.tokenOriginalValue
    private val usages = ArrayList<AstNode>()
    private val modifiers = ArrayList<AstNode>()
    private var type: PlSqlType? = null

    enum class Kind(val value: String) {
        VARIABLE("variable"),
        PARAMETER("parameter"),
        CURSOR("cursor"),
        TYPE("type"),
    }

    init {
        if (type != null) {
            this.type = type
        } else {
            this.type = PlSqlType.UNKNOWN
        }
    }

    fun modifiers(): List<AstNode> = Collections.unmodifiableList(modifiers)

    fun hasModifier(modifier: String): Boolean {
        for (syntaxToken in modifiers) {
            if (syntaxToken.tokenOriginalValue.equals(modifier, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun addModifiers(modifiers: List<AstNode>) {
        this.modifiers.addAll(modifiers)
    }

    fun addUsage(usage: AstNode) {
        usages.add(usage)
    }

    fun usages(): List<AstNode> = usages

    fun scope() = scope

    fun name() = name

    fun declaration() = declaration

    fun `is`(kind: Kind) = kind == this.kind

    fun called(name: String) = name.equals(this.name, ignoreCase = true)

    fun kind() = kind

    fun type() = type

    override fun toString() = "Symbol{name='$name', kind=$kind, scope=$scope}"
}
