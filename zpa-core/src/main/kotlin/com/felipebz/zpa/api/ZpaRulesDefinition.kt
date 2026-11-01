//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

interface ZpaRulesDefinition {
    fun repositoryName(): String
    fun repositoryKey(): String
    fun checkClasses(): Array<Class<*>>
}
