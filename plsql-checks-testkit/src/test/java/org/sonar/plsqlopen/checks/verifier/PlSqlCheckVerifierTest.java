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
package org.sonar.plsqlopen.checks.verifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.AnalyzerMessage.TextSpan;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.squid.AnalysisException;
import org.sonar.plsqlopen.PlSqlVisitorContext;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public class PlSqlCheckVerifierTest {

    private static final String FILENAME_ISSUES = "src/test/resources/check_verifier.sql";
    private static final String FILENAME_NO_ISSUE = "src/test/resources/check_verifier_no_issue.sql";

    private static PlSqlCheck getNoEffectCheck() {
        return new PlSqlCheck() { };
    }
    
    @Test
    public void verify_line_issues() {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues();
        PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor);
    }

    @Test
    public void verify_unexpected_issue() {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues().withIssue(4, "extra message");

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor);
            Fail.fail("Test should fail");
        } catch (AssertionError e) {
            assertThat(e).hasMessage("Unexpected issue at line 4: \"extra message\"");
        }
    }

    @Test
    public void verify_combined_missing_expected_and_unexpected_issues() {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues().withIssue(4, "extra message")
                .withoutIssue(1);

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor);
            Fail.fail("Test should fail");
        } catch (AssertionError e) {
            assertThat(e).hasMessage("Missing issue at line 1");
        }
    }

    @Test
    public void verify_missing_expected_issue() {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues().withoutIssue(1);

        try {
            PlSqlCheckVerifier.verify(FILENAME_ISSUES, visitor);
            Fail.fail("Test should fail");
        } catch (AssertionError e) {
            assertThat(e).hasMessage("Missing issue at line 1");
        }
    }


    @Test
    public void verify_no_issue() {
        PlSqlCheckVerifier.verify(FILENAME_NO_ISSUE, getNoEffectCheck());
    }

    @Test
    public void verify_should_fail_when_using_incorrect_shift() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_shift.sql", getNoEffectCheck());
            Fail.fail("Test should fail");
        } catch (AnalysisException e) {
            assertThat(e.getCause()).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_attribute() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute.sql",
                    getNoEffectCheck());
            Fail.fail("Test should fail");
        } catch (AnalysisException e) {
            assertThat(e.getCause()).hasMessage("Invalid param at line 1: invalid");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute2.sql",
                    getNoEffectCheck());
            Fail.fail("Test should fail");
        } catch (AnalysisException e) {
            assertThat(e.getCause()).hasMessage("Invalid param at line 1: invalid");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_secondaryLocation() throws IOException {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues();
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_secondary_location.sql", visitor);
            Fail.fail("Test should fail");
        } catch (AssertionError e) {
            assertThat(e).hasMessage("[Bad secondary locations at line 8] expected:<[[]4]> but was:<[[3, ]4]>");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_secondaryLocation2() throws IOException {
        PlSqlCheck visitor = new FakeCheck().withDefaultIssues();
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_secondary_location2.sql", visitor);
            Fail.fail("Test should fail");
        } catch (AssertionError e) {
            assertThat(e).hasMessage("[Bad secondary locations at line 8] expected:<[3, 4[, 5]]> but was:<[3, 4[]]>");
        }
    }
    
    private static class FakeCheck extends PlSqlCheck {

        Multimap<Integer, String> issues = LinkedListMultimap.create();
        Multimap<Integer, AnalyzerMessage> preciseIssues = LinkedListMultimap.create();

        private FakeCheck withDefaultIssues() {
            AnalyzerMessage withMultipleLocation = new AnalyzerMessage(this, "message4", new AnalyzerMessage.TextSpan(8, 9, 8, 10));
            withMultipleLocation.getSecondaryLocations().add(new AnalyzerMessage(this, "no message", 3));
            withMultipleLocation.getSecondaryLocations().add(new AnalyzerMessage(this, "no message", 4));
            return this.withIssue(1, "message")
                .withIssue(2, "message1")
                .withIssue(5, "message2")
                .withIssue(6, "message3")
                .withIssue(6, "message3")
                .withPreciseIssue(withMultipleLocation)
                .withPreciseIssue(new AnalyzerMessage(this, "no message", 9))
                .withPreciseIssue(new AnalyzerMessage(this, "message12", new AnalyzerMessage.TextSpan(11, 5, 12, 11)))
                .withIssue(14, "message17")
                .withPreciseIssue(new AnalyzerMessage(this, "baseline", new AnalyzerMessage.TextSpan(15, 5, 15, 9)));
        }
        
        private FakeCheck withIssue(int line, String message) {
            issues.put(line, message);
            return this;
        }
        
        private FakeCheck withPreciseIssue(AnalyzerMessage message) {
            preciseIssues.put(message.getLine(), message);
            return this;
        }
        
        private FakeCheck withoutIssue(int line) {
            issues.removeAll(line);
            preciseIssues.removeAll(line);
            return this;
        }
        
        @Override
        public void visitFile(AstNode astNode) {
            for (Integer line : issues.keySet()) {
                for (String message : issues.get(line)) {
                    getContext().createViolation(this, message, mockLine(line));
                }
            }
            
            for (AnalyzerMessage analyzerMessage : preciseIssues.values()) {
                List<PlSqlVisitorContext.Location> secLocations = new ArrayList<>();
                for (AnalyzerMessage secondaryLocation : analyzerMessage.getSecondaryLocations()) {
                    secLocations.add(new PlSqlVisitorContext.Location("", mockPreciseLocation(secondaryLocation)));
                }
                getContext().createViolation(this, analyzerMessage.getText(Locale.ENGLISH), mockPreciseLocation(analyzerMessage), secLocations);
            }
        }
        
        private static AstNode mockLine(int line) {
            Token token = mock(Token.class);
            when(token.getLine()).thenReturn(line);
            when(token.getColumn()).thenReturn(1);
            when(token.getOriginalValue()).thenReturn("");
            
            AstNode node = mock(AstNode.class);
            when(node.getToken()).thenReturn(token);
            when(node.getLastToken()).thenReturn(token);
            
            return node;
        }
        
        private AstNode mockPreciseLocation(AnalyzerMessage message) {
            TextSpan location = message.getLocation();
            
            Token token = mock(Token.class);
            when(token.getLine()).thenReturn(location.startLine);
            when(token.getColumn()).thenReturn(location.startCharacter - 1);
            when(token.getOriginalValue()).thenReturn("");
            
            Token lastToken = mock(Token.class);
            when(lastToken.getLine()).thenReturn(location.endLine);
            when(lastToken.getColumn()).thenReturn(location.endCharacter - 1);
            when(lastToken.getOriginalValue()).thenReturn("");
            
            AstNode node = mock(AstNode.class);
            when(node.getToken()).thenReturn(token);
            when(node.getLastToken()).thenReturn(lastToken);
            
            return node;
        }
    }
}
