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
package org.sonar.plsqlopen.utils

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*


fun <A : Annotation> getAnnotation(objectOrClass: Any, annotationClass: Class<A>): A? {
    val initialClass = objectOrClass as? Class<*> ?: objectOrClass.javaClass

    var aClass: Class<*>? = initialClass
    while (aClass != null) {
        val result = aClass.getAnnotation(annotationClass)
        if (result != null) {
            return result
        }
        aClass = aClass.superclass
    }

    for (anInterface in getAllInterfaces(initialClass)) {
        val result = anInterface.getAnnotation(annotationClass)
        if (result != null) {
            return result
        }
    }

    return null
}

fun getAllInterfaces(cls: Class<*>): Set<Class<*>> {

    val interfacesFound = LinkedHashSet<Class<*>>()
    getAllInterfaces(cls, interfacesFound)

    return interfacesFound
}

private fun getAllInterfaces(cls: Class<*>, interfacesFound: HashSet<Class<*>>) {
    var clss: Class<*>? = cls
    while (clss != null) {
        val interfaces = clss.interfaces

        for (i in interfaces) {
            if (interfacesFound.add(i)) {
                getAllInterfaces(i, interfacesFound)
            }
        }

        clss = clss.superclass
    }
}


fun getFields(clazz: Class<*>, forceAccess: Boolean): List<Field> {
    val result = ArrayList<Field>()
    var c: Class<*>? = clazz
    while (c != null) {
        for (declaredField in c.declaredFields) {
            if (!Modifier.isPublic(declaredField.modifiers)) {
                if (forceAccess) {
                    declaredField.isAccessible = true
                } else {
                    continue
                }
            }
            result.add(declaredField)
        }
        c = c.superclass
    }

    for (anInterface in getAllInterfaces(clazz)) {
        Collections.addAll(result, *anInterface.declaredFields)
    }

    return result
}
