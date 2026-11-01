//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/** A Java- and Kotlin-friendly callback invoked for a matching syntax view. */
@ZpaExperimentalApi
public fun interface SyntaxViewConsumer<T : SyntaxView> {

    public fun accept(view: T)
}
