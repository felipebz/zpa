//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * A table reference from a SQL table source.
 *
 * Component properties contain the original spelling of each identifier. In
 * particular, quoted identifier delimiters and case are preserved. Separators
 * are not included in the individual properties; [databaseLink] contains all
 * of the link's identifier components joined with dots. This view describes
 * syntax only and does not resolve the referenced database object.
 */
@ZpaExperimentalApi
public interface TableReference : SyntaxView {

    /** The optional schema component, preserving its original spelling. */
    public val schema: String?

    /** The table-reference name, preserving its original spelling. */
    public val name: String

    /** The optional database-link suffix, preserving its original spelling. */
    public val databaseLink: String?
}
