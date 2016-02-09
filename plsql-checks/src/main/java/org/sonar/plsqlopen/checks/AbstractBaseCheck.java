/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.squidbridge.checks.SquidCheck;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;

public abstract class AbstractBaseCheck extends SquidCheck<Grammar> {
    
    private final ResourceBundle bundle;
    
    protected AbstractBaseCheck() {
        bundle = ResourceBundle.getBundle("org.sonar.l10n.plsqlopen", Locale.getDefault());
    }
   
    protected String getLocalizedMessage(String checkKey) {
        return bundle.getString("rule.plsql." + checkKey + ".message"); 
    }
    
    protected PlSqlVisitorContext getPlSqlContext() {
        return (PlSqlVisitorContext)getContext();
    }
    
    protected PlSqlVisitorContext.Location newLocation(String message, AstNode node) {
        return new PlSqlVisitorContext.Location(message, node);
    }
    
}
