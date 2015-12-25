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

public class SameConditionCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("same_condition.sql", new SameConditionCheck());
        String message = "This condition duplicates the one on line %s.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().startsAt(3, 21).endsAt(3, 26).withMessage(String.format(message, 3))
                .secondaryLocationAt(3, 11, 3, 16)
            
            .next().startsAt(4, 20).endsAt(4, 25).withMessage(String.format(message, 4))
                .secondaryLocationAt(4, 11, 4, 16)
            
            .next().startsAt(10, 10).endsAt(10, 21).withMessage(String.format(message, 9))
                .secondaryLocationAt(9, 10, 9, 21)
            
            .next().startsAt(16, 10).endsAt(16, 40).withMessage(String.format(message, 15))
                .secondaryLocationAt(15, 10, 15, 40)
            
            .noMore();
    }
    
}
