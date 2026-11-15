//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.annotations

/** The kind of problem a rule finds in PL/SQL code. */
enum class RuleCategory {
    /** Not declared by the rule. */
    UNSPECIFIED,

    /** The code does something other than what it seems to do. */
    LOGIC_ERROR,

    /** Something the code needs is missing. */
    INCOMPLETE,

    /** The code works but says it in a roundabout way. */
    READABILITY,

    /** A PL/SQL idiom is not followed, or an obsolete form is used. */
    CONVENTION,

    /** A responsibility is placed in the wrong unit or layer. */
    DESIGN,

    /** The code does more work than it needs to. */
    PERFORMANCE,

    /** A name does not follow the naming convention of its kind of declaration. */
    NAMING,

    /** A unit test does not test what it should, or cannot be followed. */
    TESTING,

    /** The layout of the code. */
    FORMATTING,

    /** The same code is written more than once instead of being shared. */
    DUPLICATION,

    /** A unit is too long, too nested or does too many things to be followed easily. */
    COMPLEXITY
}
