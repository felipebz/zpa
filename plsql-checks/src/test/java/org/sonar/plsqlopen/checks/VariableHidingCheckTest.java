/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plsqlopen.checks;

import java.util.Collection;

import org.junit.Test;
import org.sonar.plsqlopen.AnalyzerMessage;

public class VariableHidingCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("variable_hiding.sql", new VariableHidingCheck());
        final String message = "This variable \"%s\" hides the declaration on line %s.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().startsAt(7, 5).endsAt(7, 8).withMessage(String.format(message, "var", 2))
                .secondaryLocationAt(2, 3, 2, 6)
                
            .next().startsAt(13, 5).endsAt(13, 9).withMessage(String.format(message, "var2", 3))
                .secondaryLocationAt(3, 3, 3, 7)
                
            .next().startsAt(20, 5).endsAt(20, 9).withMessage(String.format(message, "var3", 4))
                .secondaryLocationAt(4, 3, 4, 7)
                
            .next().startsAt(31, 5).endsAt(31, 8).withMessage(String.format(message, "var", 28))
                .secondaryLocationAt(28, 3, 28, 6)
                
            .next().startsAt(35, 9).endsAt(35, 10).withMessage(String.format(message, "i", 33))
                .secondaryLocationAt(33, 9, 33, 10)
                
            .noMore();
    }
    
}
