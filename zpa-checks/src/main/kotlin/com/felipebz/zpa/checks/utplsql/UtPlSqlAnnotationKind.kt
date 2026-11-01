//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.utplsql

import java.util.*

internal enum class UtPlSqlAnnotationKind(private val annotationName: String) {
    SUITE("suite"),
    SUITEPATH("suitepath"),
    DISPLAYNAME("displayname"),
    TEST("test"),
    THROWS("throws"),
    BEFOREALL("beforeall"),
    AFTERALL("afterall"),
    BEFOREEACH("beforeeach"),
    AFTEREACH("aftereach"),
    BEFORETEST("beforetest"),
    AFTERTEST("aftertest"),
    ROLLBACK("rollback"),
    DISABLED("disabled"),
    CONTEXT("context"),
    NAME("name"),
    ENDCONTEXT("endcontext"),
    TAGS("tags"),
    UNKNOWN("");

    companion object {
        private val knownAnnotations = values().associateBy { it.annotationName }

        fun from(name: String): UtPlSqlAnnotationKind =
            knownAnnotations[name.lowercase(Locale.ROOT)] ?: UNKNOWN
    }
}
