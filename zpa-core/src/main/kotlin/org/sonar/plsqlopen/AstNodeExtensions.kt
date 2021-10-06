/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import org.sonar.plsqlopen.sslr.Tree
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode


fun AstNode?.typeIs(type: AstNodeType): Boolean = this?.type == type

fun AstNode?.typeIs(types: Array<out AstNodeType>): Boolean  =
    types.any { it == this?.type }

inline fun <reified T : Tree> AstNode.asTree(): T =
    this.asSemantic().tree as T

inline fun <reified T : Tree?> AstNode.tryGetAsTree(): T? =
    this.asSemantic().tree as? T

inline fun <reified T : Tree> List<AstNode>.asTree(): List<T> =
    this.asSemantic().map { it.tree as T }

inline fun <reified T : Tree> AstNode.isOf(): Boolean =
    this.asSemantic().tree is T

fun AstNode.asSemantic(): SemanticAstNode = (this as SemanticAstNode)

fun List<AstNode>.asSemantic(): List<SemanticAstNode> =
    this.map { it.asSemantic() }
