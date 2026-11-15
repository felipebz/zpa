//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.annotations

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Rule(val key: String = "",
                      val name: String = "",
                      val description: String = "",
                      val priority: Priority = Priority.MAJOR,
                      val tags: Array<String> = [],
                      val status: String = "READY",
                      /** The kind of problem the rule finds; [RuleCategory.UNSPECIFIED] when not declared. */
                      val category: RuleCategory = RuleCategory.UNSPECIFIED)
