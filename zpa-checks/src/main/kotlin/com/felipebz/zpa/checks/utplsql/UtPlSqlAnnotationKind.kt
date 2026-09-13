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
package com.felipebz.zpa.checks.utplsql

import java.util.Locale

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
