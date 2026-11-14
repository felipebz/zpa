//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.matchers.NameCriteria
import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

/** Oracle reads an unquoted name the same way whatever the client's locale: so does the analysis. */
class DefaultLocaleTest : BaseCheckTest() {

    @Test
    fun keywordsAndNamesWithAnUppercaseIUnderTheTurkishLocale() = underTurkishLocale {
        PlSqlCheckVerifier.verify(getPath("default_locale.sql"), InsertWithoutColumnsCheck())
    }

    @Test
    fun aNamePrefixWithAnIUnderTheTurkishLocale() = underTurkishLocale {
        assertThat(NameCriteria.startsWith("pkg_i").matches("PKG_ITEM")).isTrue()
    }

    @Test
    fun aNumberInAMessageUnderTheTurkishLocale() = underTurkishLocale {
        val check = PlSqlCheck()
        check.addFileIssue("{0} rows", 12345)
        assertThat(check.issues().single().primaryLocation().message()).isEqualTo("12,345 rows")
    }

    private fun underTurkishLocale(test: () -> Unit) {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        try {
            test()
        } finally {
            Locale.setDefault(previous)
        }
    }
}
