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

/** Why a call target cannot be settled from the project; several may hold at once. */
@ZpaExperimentalApi
enum class ProjectCallUncertainty {
    /**
     * More than one namespace interpretation of the written name has project evidence. A standalone callable whose
     * heading names it but whose signature was not retained is evidence too, without being a candidate.
     */
    NAMESPACE_COMPETITION,

    /** A package interpretation has more than one retained specification. */
    DUPLICATE_SPECIFICATION,

    /**
     * Declarations of the target may exist that were not retained: a package specification in play may hold callable
     * headers that were not retained, or a standalone callable of exactly the target name was declared but its
     * signature could not be represented.
     */
    PARTIAL_DECLARATIONS
}
