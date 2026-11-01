//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNodeType
import org.sonar.api.scanner.ScannerSide
import java.util.*

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
