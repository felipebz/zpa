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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * Caller-owned source acquisition used only during synchronous project preparation.
 * Each source invokes its reader at most once per preparation; concurrent preparation
 * may invoke readers on worker threads. Shared captured state must support that concurrency. The caller owns
 * decoding and resource closure. Neither this callback nor its captures enter the snapshot.
 */
@ZpaExperimentalApi
fun interface ProjectSourceReader {
    /** Returns non-null source text; an Exception becomes a per-file failure summary. */
    @Throws(Exception::class)
    fun read(): String
}
