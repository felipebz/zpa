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
package org.sonar.plugins.plsqlopen.api;

import java.nio.charset.StandardCharsets;

import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.sslr.grammar.GrammarRuleKey;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class RuleTest {
    private boolean errorRecoveryEnabled;
    
    protected Parser<Grammar> p;
    
    protected void setRootRule(GrammarRuleKey ruleKey) {
        p = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8, errorRecoveryEnabled));
        p.setRootRule(p.getGrammar().rule(ruleKey));
    }
    
    protected void setErrorRecoveryEnabled(boolean value) {
        errorRecoveryEnabled = value;
    }
    
    protected static String lines(String... lines) {
        return String.join("\n", lines);
    }
}
