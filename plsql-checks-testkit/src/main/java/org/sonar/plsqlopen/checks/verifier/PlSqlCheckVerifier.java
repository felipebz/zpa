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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

public class PlSqlCheckVerifier extends PlSqlCheck {

    private List<TestIssue> expectedIssues = new ArrayList<>();
    
    public static void verify(String filename, PlSqlCheck check) {
        verify(filename, check, null);
    }
    
    public static void verify(String filename, PlSqlCheck check, FormsMetadata metadata) {
        File file = new File(filename);
        PlSqlCheckVerifier verifier = new PlSqlCheckVerifier();
        
        DefaultInputFile inputFile;
        
        try {
            inputFile = new TestInputFileBuilder("key", filename)
                    .setLanguage("plsqlopen")
                    .setCharset(StandardCharsets.UTF_8)
                    .initMetadata(Files.toString(file, StandardCharsets.UTF_8))
                    .setModuleBaseDir(Paths.get(""))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        SensorContextTester context = SensorContextTester.create(new File("."));
        context.fileSystem().add(inputFile);
        
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, ImmutableList.of(check, verifier), new NoSonarFilter(), metadata, false);
        Collection<AnalyzerMessage> issues = scanner.scanFile(inputFile);
        
        Iterator<AnalyzerMessage> actualIssues = getActualIssues(issues);
        List<TestIssue> expectedIssues = Ordering.natural().onResultOf(TestIssue::line).sortedCopy(verifier.expectedIssues);

        for (TestIssue expected : expectedIssues) {
            if (actualIssues.hasNext()) {
                verifyIssue(expected, actualIssues.next());
            } else {
                throw new AssertionError("Missing issue at line " + expected.line());
            }
        }

        if (actualIssues.hasNext()) {
            AnalyzerMessage issue = actualIssues.next();
            throw new AssertionError(
                    "Unexpected issue at line " + line(issue) + ": \"" + issue.getText(Locale.ENGLISH) + "\"");
        }

    }

    private static int line(AnalyzerMessage issue) {
        return issue.getLine();
    }

    private static void verifyIssue(TestIssue expected, AnalyzerMessage actual) {
        if (line(actual) > expected.line()) {
            fail("Missing issue at line " + expected.line());
        }
        if (line(actual) < expected.line()) {
            fail("Unexpected issue at line " + line(actual) + ": \"" + actual.getText(Locale.ENGLISH) + "\"");
        }
        if ((expected.startColumn() != null || expected.endColumn() != null || expected.endLine() != null) && actual.getLocation() == null) {
            fail("The current rule does not support precise location.");
        }
        if (expected.message() != null) {
            assertThat(actual.getText(Locale.ENGLISH)).as("Bad message at line " + expected.line())
                    .isEqualTo(expected.message());
        }
        if (expected.effortToFix() != null) {
            assertThat(actual.getCost()).as("Bad effortToFix at line " + expected.line())
                    .isEqualTo(expected.effortToFix());
        }
        if (expected.startColumn() != null) {
            assertThat(actual.getLocation().startCharacter + 1).as("Bad start column at line " + expected.line())
                    .isEqualTo(expected.startColumn());
        }
        if (expected.endColumn() != null) {
            assertThat(actual.getLocation().endCharacter + 1).as("Bad end column at line " + expected.line())
                    .isEqualTo(expected.endColumn());
        }
        if (expected.endLine() != null) {
            assertThat(actual.getLocation().endLine).as("Bad end line at line " + expected.line())
                    .isEqualTo(expected.endLine());
        }
        if (expected.secondaryLines() != null) {
            assertThat(secondary(actual)).as("Bad secondary locations at line " + expected.line())
                    .isEqualTo(expected.secondaryLines());
        }
    }

    private static List<Integer> secondary(AnalyzerMessage issue) {
        List<Integer> result = new ArrayList<>();

        for (AnalyzerMessage issueLocation : issue.getSecondaryLocations()) {
            result.add(issueLocation.getLine());
        }

        return Ordering.natural().sortedCopy(result);
    }

    private static Iterator<AnalyzerMessage> getActualIssues(Collection<AnalyzerMessage> issues) {
        List<AnalyzerMessage> sortedIssues = Ordering.natural().onResultOf(PlSqlCheckVerifier::line).sortedCopy(issues);
        return sortedIssues.iterator();
    }

    @Override
    public void visitToken(Token token) {
        for (Trivia trivia : token.getTrivia()) {
            String text = trivia.getToken().getValue().substring(2).trim();
            String marker = "Noncompliant";

            if (text.startsWith(marker)) {
                int issueLine = trivia.getToken().getLine();
                String paramsAndMessage = text.substring(marker.length()).trim();

                if (paramsAndMessage.startsWith("@")) {
                    String[] spaceSplit = paramsAndMessage.split("[\\s\\[{]", 2);
                    
                    String shiftValue = spaceSplit[0];
                    
                    if (shiftValue.charAt(1) != '+' && shiftValue.charAt(1) != '-') {
                        fail("Use only '@+N' or '@-N' to shifts messages.");
                    }
                    
                    issueLine += Integer.valueOf(shiftValue.substring(1));
                    paramsAndMessage = spaceSplit.length > 1 ? spaceSplit[1] : "";
                }

                TestIssue issue = TestIssue.create(null, issueLine);
                
                if (paramsAndMessage.startsWith("{{")) {
                    int endIndex = paramsAndMessage.indexOf("}}");
                    String message = paramsAndMessage.substring(2, endIndex);
                    issue.message(message);
                    paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim();
                }
                
                if (paramsAndMessage.startsWith("[[")) {
                    int endIndex = paramsAndMessage.indexOf("]]");
                    addParams(issue, paramsAndMessage.substring(2, endIndex));
                }

                expectedIssues.add(issue);

            } else if (text.startsWith("^")) {
                addPreciseLocation(trivia);
            }
        }
    }

    private static void addParams(TestIssue issue, String params) {
        for (String param : Splitter.on(';').split(params)) {
            int equalIndex = param.indexOf('=');
            if (equalIndex == -1) {
                throw new IllegalStateException("Invalid param at line 1: " + param);
            }
            String name = param.substring(0, equalIndex);
            String value = param.substring(equalIndex + 1);

            if ("effortToFix".equalsIgnoreCase(name)) {
                issue.effortToFix(Integer.valueOf(value));

            } else if ("sc".equalsIgnoreCase(name)) {
                issue.startColumn(Integer.valueOf(value));

            } else if ("ec".equalsIgnoreCase(name)) {
                issue.endColumn(Integer.valueOf(value));

            } else if ("el".equalsIgnoreCase(name)) {
                issue.endLine(lineValue(issue.line(), value));

            } else if ("secondary".equalsIgnoreCase(name)) {
                addSecondaryLines(issue, value);

            } else {
                throw new IllegalStateException("Invalid param at line 1: " + name);
            }
        }
    }

    private static void addSecondaryLines(TestIssue issue, String value) {
        List<Integer> secondaryLines = new ArrayList<>();
        if (!"".equals(value)) {
            for (String secondary : Splitter.on(',').split(value)) {
                secondaryLines.add(lineValue(issue.line(), secondary));
            }
        }
        issue.secondary(secondaryLines);
    }

    private static int lineValue(int baseLine, String shift) {
        if (shift.startsWith("+")) {
            return baseLine + Integer.valueOf(shift.substring(1));
        }
        if (shift.startsWith("-")) {
            return baseLine - Integer.valueOf(shift.substring(1));
        }
        return Integer.valueOf(shift);
    }

    private void addPreciseLocation(Trivia trivia) {
        Token token = trivia.getToken();
        int line = token.getLine();
        String text = token.getValue();
        if (token.getColumn() > 1) {
            throw new IllegalStateException(
                    "Line " + line + ": comments asserting a precise location should start at column 1");
        }
        String missingAssertionMessage = String.format(
                "Invalid test file: a precise location is provided at line %s but no issue is asserted at line %s",
                line, line - 1);
        if (expectedIssues.isEmpty()) {
            throw new IllegalStateException(missingAssertionMessage);
        }
        TestIssue issue = expectedIssues.get(expectedIssues.size() - 1);
        if (issue.line() != line - 1) {
            throw new IllegalStateException(missingAssertionMessage);
        }
        issue.endLine(issue.line());
        issue.startColumn(text.indexOf('^') + 1);
        issue.endColumn(text.lastIndexOf('^') + 2);
    }

}
