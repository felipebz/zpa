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
package org.sonar.plsqlopen.checks;

import java.util.Locale;
import java.util.ResourceBundle;

import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;

import com.sonar.sslr.api.AstNode;

public abstract class AbstractBaseCheck extends PlSqlCheck {
    
    private ResourceBundle bundle;
    
    /***
     * Creates an issue location.
     * @param message Description.
     * @param node Node related to the location.
     * @return The new issue location.
     * @deprecated since 2.3.0. Use {@link IssueLocation}.
     */
    @Deprecated
    protected PlSqlVisitorContext.Location newLocation(String message, AstNode node) {
        return new PlSqlVisitorContext.Location(message, node);
    }
    
    protected String getLocalizedMessage(String checkKey) {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("org.sonar.l10n.plsqlopen", Locale.getDefault());
        }
        return bundle.getString(checkKey + ".message");
    }
    
}
