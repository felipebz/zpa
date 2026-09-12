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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNodeType
import org.sonar.api.scanner.ScannerSide
import java.util.Collections

@ScannerSide
class ObjectLocator {
    private var mappedObjects: List<MappedObject> = emptyList()

    internal fun setObjects(objects: Collection<MappedObject>) {
        mappedObjects = Collections.unmodifiableList(objects.toList())
    }

    fun findMainObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isMain && it.identifier.equals(identifier, ignoreCase = true) && it.objectType in types }
    }

    fun findTestObject(identifier: String, vararg types: AstNodeType): MappedObject? {
        return mappedObjects.find { it.isTest && it.identifier.equals(identifier, ignoreCase = true) && it.objectType in types }
    }
}
