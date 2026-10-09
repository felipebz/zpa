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

/** Preparation lifecycle state, not a claim of complete PL/SQL coverage or validity. */
@ZpaExperimentalApi
enum class ProjectPreparationState {
    /** No preparation has occurred; reserved for capabilities supplied by internal lifecycles. */
    NOT_PREPARED,

    /** Read/extraction completed without failures but retained no supported declarations. */
    PREPARED_EMPTY,

    /** Read/extraction completed without failures and retained supported declarations. */
    PREPARED_WITH_DECLARATIONS,

    /** At least one source failed; takes precedence over empty/nonempty declaration counts. */
    PREPARED_WITH_FAILURES
}
