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

public class AddParenthesesInNestedExpressionCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("add_parentheses_in_nested_expression.sql", new AddParenthesesInNestedExpressionCheck());
        final String message = "Add parentheses around this AND condition.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().startsAt(2, 20).endsAt(2,  35).withMessage(message)
            .next().startsAt(3, 20).endsAt(3,  35).withMessage(message)
            .next().startsAt(3, 39).endsAt(3,  54).withMessage(message)
            .next().startsAt(9, 10).endsAt(10, 15).withMessage(message)
            .noMore();
    }
    
}
