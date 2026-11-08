//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

fun interface ZpaActiveRules {
    fun findByRepository(repository: String): Collection<ZpaActiveRule>
}
