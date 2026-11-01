//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class CheckListTest {

    /**
     * Enforces that each check declared in list.
     */
    @Test
    fun count() {
        val files = File("src/main/kotlin/com/felipebz/zpa/checks/")
            .list { _, name -> name.contains("Check.") && !name.startsWith("Abstract") }
            .map { File(it).nameWithoutExtension }
            .toTypedArray()

        assertThat(CheckList.checks.map { it.simpleName }).containsExactlyInAnyOrder(*files)
    }

}
