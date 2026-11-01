//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.CustomAnnotationBasedRulesDefinition.Companion.convertCheckClassName
import com.felipebz.zpa.api.checks.PlSqlCheck
import java.util.*

abstract class AbstractBaseCheck : PlSqlCheck() {

    private val bundle: ResourceBundle = ResourceBundle.getBundle("org.sonar.l10n.plsqlopen", Locale.getDefault())

    protected fun getLocalizedMessage(): String {
        return bundle.getString("${convertCheckClassName(this::class.java)}.message")
    }

    protected fun getLocalizedMessage(key: String): String {
        return bundle.getString("${convertCheckClassName(this::class.java)}.$key")
    }


}
