//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight view of a SELECT INTO clause. */
@ZpaExperimentalApi
public interface SelectIntoClause : SyntaxView {

    /** Whether this is a BULK COLLECT INTO clause. */
    public val isBulkCollect: Boolean
}
