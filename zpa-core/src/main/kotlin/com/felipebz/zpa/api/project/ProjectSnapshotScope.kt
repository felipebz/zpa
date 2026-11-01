//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

/** Private identity token issued once per snapshot. Deliberately fieldless: no snapshot/index back-reference. */
internal class ProjectSnapshotScope private constructor() {
    companion object {
        @JvmSynthetic
        fun issue() = ProjectSnapshotScope()
    }
}

