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

import java.util.Collection;

import org.junit.Test;
import org.sonar.plsqlopen.AnalyzerMessage;

public class UnnecessaryAliasInQueryCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("unnecessary_alias_in_query.sql", new UnnecessaryAliasInQueryCheck());
        String message = "This query has only one reference to the table \"tab\". Remove the alias \"x\" to improve the readability.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().atLine(3).withMessage(message)
            .next().atLine(6).withMessage(message)
            .next().atLine(10).withMessage(message)
            .next().atLine(13).withMessage(message)
            .noMore();
    }
    
    @Test
    public void test2() {
        UnnecessaryAliasInQueryCheck check = new UnnecessaryAliasInQueryCheck();
        check.acceptedLength = 4;
        
        Collection<AnalyzerMessage> messages = scanFile("unnecessary_alias_in_query_custom_length.sql", check);
        String message = "This query has only one reference to the table \"tab\". Remove the alias \"x\" to improve the readability.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().atLine(3).withMessage(message)
            .noMore();
    }

}
