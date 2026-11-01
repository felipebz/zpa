//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.RuleProperty
import com.felipebz.zpa.utils.getAnnotation
import java.lang.reflect.Field

open class RuleMetadataLoader {

    open fun getRuleAnnotation(annotatedClassOrObject: Any) : RuleData? =
        RuleData.from(getAnnotation(annotatedClassOrObject, Rule::class.java))

    open fun getRulePropertyAnnotation(field: Field) : RulePropertyData? =
        RulePropertyData.from(field.getAnnotation(RuleProperty::class.java))

}
