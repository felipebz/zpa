/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plsqlopen.checks;

import java.util.Locale;
import java.util.ResourceBundle;

public abstract class AbstractBaseCheck extends PlSqlCheck {
    
    private ResourceBundle bundle;
    
    protected String getLocalizedMessage(String checkKey) {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("org.sonar.l10n.plsqlopen", Locale.getDefault());
        }
        return bundle.getString("rule.plsql." + checkKey + ".message"); 
    }
    
}
