//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A lightweight public view over one node in ZPA's existing parser AST. */
@ZpaExperimentalApi
public interface SyntaxView {

    /**
     * The backing AST node, available as an explicit escape hatch when the
     * typed view does not expose a required detail.
     */
    public val astNode: AstNode
}
