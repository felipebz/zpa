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
package com.felipebz.zpa.symbols.local

/**
 * Internal switches through which consumers take their decision from the local semantic resolution instead of the
 * legacy lookup. Not public API and not a Sonar property: an environment variable of the analysis process is the way
 * back, and tests in this module set the switches directly.
 */
internal object LocalSemanticSwitches {
    /**
     * The project type resolution's lexical gate decides from the local outcome where it is certain. On by default;
     * `ZPA_INTERNAL_LOCAL_TYPE_GATE=false` turns it off.
     */
    @Volatile
    var typeNameGate: Boolean = System.getenv("ZPA_INTERNAL_LOCAL_TYPE_GATE") != "false"
}
