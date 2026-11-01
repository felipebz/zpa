//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.QualifiedName

/** The name of a `CREATE PACKAGE` or `CREATE PACKAGE BODY` node, as written. */
internal fun packageNameOf(packageNode: AstNode): QualifiedName? {
    val unitName = packageNode.getFirstChildOrNull(PlSqlGrammar.UNIT_NAME) ?: return null
    val identifiers = unitName.getDescendants(PlSqlGrammar.IDENTIFIER_NAME).map { OracleIdentifier.fromSource(it.tokenOriginalValue) }
    return identifiers.takeIf { it.isNotEmpty() }?.let(::QualifiedName)
}

/** The name of the package or package body enclosing [node], if any. */
internal fun enclosingPackageName(node: AstNode): QualifiedName? =
    node.getFirstAncestorOrNull(PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY)?.let(::packageNameOf)
