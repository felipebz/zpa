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

import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import javax.annotation.Nullable;

import org.hamcrest.Matcher;
import org.sonar.plsqlopen.AnalyzerMessage;

import com.google.common.base.Objects;
import com.google.common.collect.Ordering;

public class AnalyzerMessagesVerifier {

    private final Iterator<AnalyzerMessage> iterator;
    private AnalyzerMessage current;
    private Iterator<AnalyzerMessage> secondaryLocations;

    private static final Comparator<AnalyzerMessage> ORDERING = new Comparator<AnalyzerMessage>() {
        @Override
        public int compare(AnalyzerMessage left, AnalyzerMessage right) {
            if (Objects.equal(left.getLine(), right.getLine())) {
                return left.getDefaultMessage().compareTo(right.getDefaultMessage());
            } else if (left.getLine() == null) {
                return -1;
            } else if (right.getLine() == null) {
                return 1;
            } else {
                return left.getLine().compareTo(right.getLine());
            }
        }
    };

    private AnalyzerMessagesVerifier(Collection<AnalyzerMessage> messages) {
        iterator = Ordering.from(ORDERING).sortedCopy(messages).iterator();
    }

    public static AnalyzerMessagesVerifier verify(Collection<AnalyzerMessage> messages) {
        return new AnalyzerMessagesVerifier(messages);
    }

    public AnalyzerMessagesVerifier next() {
        if (!iterator.hasNext()) {
            throw new AssertionError("\nExpected violation");
        }
        current = iterator.next();
        secondaryLocations = current.getSecondaryLocations().iterator();
        return this;
    }

    public void noMore() {
        if (iterator.hasNext()) {
            AnalyzerMessage next = iterator.next();
            throw new AssertionError("\nNo more violations expected\ngot: at line " + next.getLine());
        }
    }

    private void checkStateOfCurrent() {
        if (current == null) {
            throw new IllegalStateException("Prior to this method you should call next()");
        }
    }

    public AnalyzerMessagesVerifier atLine(@Nullable Integer expectedLine) {
        checkStateOfCurrent();
        if (!Objects.equal(expectedLine, current.getLocation().startLine)) {
            throw assertionError(expectedLine, current.getLine());
        }
        return this;
    }

    public AnalyzerMessagesVerifier withMessage(String expectedMessage) {
        checkStateOfCurrent();
        String actual = current.getText(Locale.ENGLISH);
        if (!actual.equals(expectedMessage)) {
            throw assertionError("\"" + expectedMessage + "\"", "\"" + actual + "\"");
        }
        return this;
    }

    public AnalyzerMessagesVerifier withMessageThat(Matcher<String> matcher) {
        checkStateOfCurrent();
        String actual = current.getText(Locale.ENGLISH);
        assertThat(actual, matcher);
        return this;
    }

    public AnalyzerMessagesVerifier withCost(Double expectedCost) {
        checkStateOfCurrent();
        if (!Objects.equal(expectedCost, current.getCost())) {
            throw assertionError(expectedCost, current.getCost());
        }
        return this;
    }
    
    public AnalyzerMessagesVerifier startsAt(int expectedLine, int expectedColumn) {
        checkStateOfCurrent();
        if (!Objects.equal(expectedLine, current.getLocation().startLine)) {
            throw assertionError(expectedLine, current.getLocation().startLine);
        }
        if (!Objects.equal(expectedColumn, current.getLocation().startCharacter + 1)) {
            throw assertionError(expectedColumn, current.getLocation().startCharacter + 1);
        }
        return this;
    }
    
    public AnalyzerMessagesVerifier endsAt(int expectedLine, int expectedColumn) {
        checkStateOfCurrent();
        if (!Objects.equal(expectedLine, current.getLocation().endLine)) {
            throw assertionError(expectedLine, current.getLocation().endLine);
        }
        if (!Objects.equal(expectedColumn, current.getLocation().endCharacter + 1)) {
            throw assertionError(expectedColumn, current.getLocation().endCharacter + 1);
        }
        return this;
    }
    
    public AnalyzerMessagesVerifier secondaryLocationAt(int startLine, int startColumn, int endLine, int endColumn) {
        if (!secondaryLocations.hasNext()) {
            throw new AssertionError("\nExpected secondary location");
        }
        AnalyzerMessage secondary = secondaryLocations.next();
        if (!Objects.equal(startLine, secondary.getLocation().startLine)) {
            throw assertionError(startLine, secondary.getLocation().startLine);
        }
        if (!Objects.equal(startColumn, secondary.getLocation().startCharacter + 1)) {
            throw assertionError(startColumn, secondary.getLocation().startCharacter + 1);
        }
        if (!Objects.equal(endLine, secondary.getLocation().endLine)) {
            throw assertionError(endLine, secondary.getLocation().endLine);
        }
        if (!Objects.equal(endColumn, secondary.getLocation().endCharacter + 1)) {
            throw assertionError(endColumn, secondary.getLocation().endCharacter + 1);
        }
        return this;
    }

    private static AssertionError assertionError(Object expected, Object actual) {
        return new AssertionError("\nExpected: " + expected + "\ngot: " + actual);
    }
}
