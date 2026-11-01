//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.toolkit

import com.felipebz.flr.toolkit.Toolkit

fun main() {
    val toolkit = Toolkit("ZPA Toolkit", ZpaConfigurationModel())
    toolkit.run()
}
