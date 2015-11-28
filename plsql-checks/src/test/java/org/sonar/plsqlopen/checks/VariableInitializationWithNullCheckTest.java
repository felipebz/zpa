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

public class VariableInitializationWithNullCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("variable_initialization_with_null.sql", new VariableInitializationWithNullCheck());
        final String message = "Remove this unnecessary initialization to NULL.";
        AnalyzerMessagesVerifier.verify(messages)
            .next().atLine(2).withMessage(message)
            .next().atLine(3).withMessage(message)
            .next().atLine(4).withMessage(message)
            .next().atLine(5).withMessage(message)
            .next().atLine(7).withMessage(message)
            .noMore();
    }

}
