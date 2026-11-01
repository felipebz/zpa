//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

object SonarQubeUtils {

    val isCommercialEdition by lazy {
        var value = false
        try {
            // is it running a commercial edition of SQ?
            Class.forName("com.sonarsource.plugins.license.api.LicensedPluginRegistration")
            value = true
        } catch (e: ClassNotFoundException) {
            // ignore
        }
        value
    }

}
