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
package org.sonar.plsqlopen.sslr

import org.sonar.plsqlopen.asSemantic
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode

class Statements(override val astNode: SemanticAstNode) : TreeImpl(astNode), List<SemanticAstNode> {

    private val children by lazy { astNode.children.asSemantic() }

    override val size: Int =
        children.size

    override fun contains(element: SemanticAstNode): Boolean  =
        children.contains(element)

    override fun containsAll(elements: Collection<SemanticAstNode>): Boolean =
        children.containsAll(elements)

    override fun get(index: Int): SemanticAstNode =
        children[index]

    override fun indexOf(element: SemanticAstNode): Int =
        children.indexOf(element)

    override fun isEmpty(): Boolean =
        children.isEmpty()

    override fun iterator(): Iterator<SemanticAstNode> =
        children.iterator()

    override fun lastIndexOf(element: SemanticAstNode): Int =
        children.lastIndexOf(element)

    override fun listIterator(): ListIterator<SemanticAstNode>  =
        children.listIterator()

    override fun listIterator(index: Int): ListIterator<SemanticAstNode> =
        children.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<SemanticAstNode> =
        children.subList(fromIndex, toIndex)

}
