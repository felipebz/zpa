//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:Suppress("DEPRECATION")

package com.felipebz.zpa.api.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import java.util.*

/**
 * Deprecated: part of the legacy symbol model, removed in the next major version. Use the read queries of
 * PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty,
 * valueCategory and typeCategory.
 */
@Deprecated(
    "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
    "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
    "regionUncertainty, valueCategory and typeCategory."
)
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
