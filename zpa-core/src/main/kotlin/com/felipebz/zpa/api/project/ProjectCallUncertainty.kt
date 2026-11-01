//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
