//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class RuleParam(override val key: String) : ZpaRuleParam {

    override var defaultValue: String = ""

    override var description: String = ""

}
