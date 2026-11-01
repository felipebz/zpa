//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import java.awt.Dimension
import javax.swing.JTextPane

class NoWrapJTextPane : JTextPane() {
    override fun getScrollableTracksViewportWidth(): Boolean {
        return (getUI().getPreferredSize(this).width
            <= parent.size.width)
    }

    override fun getPreferredSize(): Dimension {
        return getUI().getPreferredSize(this)
    }
}
