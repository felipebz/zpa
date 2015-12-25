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

public class ComparisonWithNullCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        Collection<AnalyzerMessage> messages = scanFile("comparison_with_null.sql", new ComparisonWithNullCheck());
        final String messageWithIsNull = "Fix this comparison or change to \"IS NULL\".";
        final String messageWithIsNotNull = "Fix this comparison or change to \"IS NOT NULL\".";
        AnalyzerMessagesVerifier.verify(messages)
            .next().startsAt(3, 11).endsAt(3, 21).withMessage(messageWithIsNull)
            .next().startsAt(4, 11).endsAt(4, 22).withMessage(messageWithIsNotNull)
            .next().startsAt(5, 11).endsAt(5, 19).withMessage(messageWithIsNull)
            .next().startsAt(6, 11).endsAt(6, 20).withMessage(messageWithIsNotNull)
            .noMore();
    }

}
