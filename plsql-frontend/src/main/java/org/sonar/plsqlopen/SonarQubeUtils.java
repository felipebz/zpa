/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen;

import org.sonar.api.utils.Version;

public class SonarQubeUtils {

    private SonarQubeUtils() { }

    private static boolean commercialEdition;
    private static boolean isSQ71OrGreater;

    static {
        commercialEdition = false;
        try {
            // is it running a commercial edition of SQ?
            Class.forName("com.sonarsource.plugins.license.api.LicensedPluginRegistration");
            commercialEdition = true;
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    public static boolean isCommercialEdition() {
        return commercialEdition;
    }

    static void setSonarQubeVersion(Version sonarQubeVersion) {
        isSQ71OrGreater = sonarQubeVersion.isGreaterThanOrEqual(Version.create(7, 1));
    }

    public static boolean isIsSQ71OrGreater() {
        return isSQ71OrGreater;
    }
}
