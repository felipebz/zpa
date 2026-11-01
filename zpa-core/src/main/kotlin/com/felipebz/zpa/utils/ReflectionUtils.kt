//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utils

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
