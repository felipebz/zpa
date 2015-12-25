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

public class SameBranchCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("same_branch.sql", new SameBranchCheck());
        final String message = "Either merge this branch with the identical one on line %d or change one of the implementations.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().startsAt(5, 5).endsAt(5, 14).withMessage(String.format(message, 3))
                .secondaryLocationAt(3, 5, 3, 14)
            
            .next().startsAt(11, 5).endsAt(11, 14).withMessage(String.format(message, 9))
                .secondaryLocationAt(9, 5, 9, 14)
            
            .noMore();
    }
    
}
