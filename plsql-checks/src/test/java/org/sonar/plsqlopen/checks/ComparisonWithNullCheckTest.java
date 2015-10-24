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

import org.junit.Test;
import org.sonar.plsqlopen.checks.ComparisonWithNullCheck;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ComparisonWithNullCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("comparison_with_null.sql", new ComparisonWithNullCheck());
        final String messageWithIsNull = "Fix this comparison or change to \"IS NULL\".";
        final String messageWithIsNotNull = "Fix this comparison or change to \"IS NOT NULL\".";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage(messageWithIsNull)
            .next().atLine(4).withMessage(messageWithIsNotNull)
            .next().atLine(5).withMessage(messageWithIsNull)
            .next().atLine(6).withMessage(messageWithIsNotNull)
            .noMore();
    }

}
