//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.annotations

@Retention
@Target(AnnotationTarget.FIELD)
annotation class RuleProperty(val key: String = "",
                              val description: String = "",
                              val defaultValue: String = "",
                              val type: String = "")
