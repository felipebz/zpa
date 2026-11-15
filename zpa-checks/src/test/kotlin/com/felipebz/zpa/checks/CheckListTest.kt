//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleCategory
import com.felipebz.zpa.api.annotations.RuleTemplate
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

    @Test
    fun category() {
        val (templates, rules) = CheckList.checks.partition { it.isAnnotationPresent(RuleTemplate::class.java) }
        val category = { check: Class<*> -> check.getAnnotation(Rule::class.java).category }
        assertThat(rules.filter { category(it) == RuleCategory.UNSPECIFIED }.map { it.simpleName }).isEmpty()
        assertThat(templates.filter { category(it) != RuleCategory.UNSPECIFIED }.map { it.simpleName }).isEmpty()
    }

}
