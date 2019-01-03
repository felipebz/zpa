/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.plsqlopen.checks.IssueLocation;
import org.sonar.plsqlopen.checks.PlSqlCheck;

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
        } catch (AssertionError e) {
            assertThat(e).hasMessage("Use only '@+N' or '@-N' to shifts messages.");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_attribute() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute.sql",
                    getNoEffectCheck());
            Fail.fail("Test should fail");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Invalid param at line 1: invalid");
        }
    }

    @Test
    public void verify_should_fail_when_using_incorrect_attribute2() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_attribute2.sql",
                    getNoEffectCheck());
            Fail.fail("Test should fail");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Invalid param at line 1: invalid");
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
    
    @Test
    public void verify_should_fail_when_precision_location_comment_is_invalid() throws IOException {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_incorrect_comment.sql", new FakeCheck());
            Fail.fail("Test should fail");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Line 3: comments asserting a precise location should start at column 1");
        }
    }
    
    @Test
    public void verify_unexpected_precise_location() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_unexpected_precise_location.sql", new FakeCheck());
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Invalid test file: a precise location is provided at line 3 but no issue is asserted at line 2");
        }
    }
    
    @Test
    public void verify_unexpected_precise_location2() {
        try {
            PlSqlCheckVerifier.verify("src/test/resources/check_verifier_unexpected_precise_location2.sql", new FakeCheck());
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("Invalid test file: a precise location is provided at line 3 but no issue is asserted at line 2");
        }
    }
    
    private static class FakeCheck extends PlSqlCheck {

        Multimap<Integer, String> issues = LinkedListMultimap.create();
        Multimap<Integer, Set<IssueLocation>> preciseIssues = LinkedListMultimap.create();

        private FakeCheck withDefaultIssues() {
            return this.withIssue(1, "message")
                .withIssue(2, "message1")
                .withIssue(5, "message2")
                .withIssue(6, "message3")
                .withIssue(6, "message3")
                .withPreciseIssue(
                        IssueLocation.preciseLocation(mockNode(8, 9, 8, 10), "message4"),
                        IssueLocation.atLineLevel("no message", 3),
                        IssueLocation.atLineLevel("no message", 4)
                        )
                .withPreciseIssue(IssueLocation.atLineLevel("no message", 9))
                .withPreciseIssue(IssueLocation.preciseLocation(mockNode(11, 5, 12, 11), "message12"))
                .withIssue(14, "message17")
                .withPreciseIssue(IssueLocation.preciseLocation(mockNode(15, 5, 15, 9), "baseline"));
        }
        
        private FakeCheck withIssue(int line, String message) {
            issues.put(line, message);
            return this;
        }
        
        private FakeCheck withPreciseIssue(IssueLocation... message) {
            preciseIssues.put(message[0].startLine(), new LinkedHashSet<>(Arrays.asList(message)));
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
                    addLineIssue(message, line);
                }
            }
            
            for (Set<IssueLocation> locations : preciseIssues.values()) {
                PreciseIssue issue = null;
                for (IssueLocation location : locations) {
                    if (issue == null) {
                        issue = addIssue(location).withCost(3);
                    } else {
                        issue.secondary(location);
                    }
                }
            }
        }
        
        private AstNode mockNode(int startLine, int startCharacter, int endLine, int endCharacter) {
            
            Token token = mock(Token.class);
            when(token.getLine()).thenReturn(startLine);
            when(token.getColumn()).thenReturn(startCharacter - 1);
            when(token.getOriginalValue()).thenReturn("");
            when(token.getValue()).thenReturn("");
            
            Token lastToken = mock(Token.class);
            when(lastToken.getLine()).thenReturn(endLine);
            when(lastToken.getColumn()).thenReturn(endCharacter - 1);
            when(lastToken.getOriginalValue()).thenReturn("");
            when(lastToken.getValue()).thenReturn("");
            
            AstNode node = mock(AstNode.class);
            when(node.getToken()).thenReturn(token);
            when(node.getLastToken()).thenReturn(lastToken);
            
            return node;
        }
    }
}
