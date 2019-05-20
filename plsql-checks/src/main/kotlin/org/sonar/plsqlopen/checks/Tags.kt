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
package org.sonar.plsqlopen.checks

object Tags {
    const val BRAIN_OVERLOAD = "brain-overload"
    const val BUG = "bug"
    const val CLUMSY = "clumsy"
    const val CONVENTION = "convention"
    const val OBSOLETE = "obsolete"
    const val SECURITY = "security"
    const val UNUSED = "unused"
    const val CERT = "cert"
    const val PITFALL = "pitfall"
    const val MISRA = "misra"
    const val CONFUSING = "confusing"
    const val CWE = "cwe"
    const val PERFORMANCE = "performance"
    const val UTPLSQL = "utplsql"
}
