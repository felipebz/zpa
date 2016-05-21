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
package org.sonar.plsqlopen.checks.verifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.squidbridge.SquidAstVisitor;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

public class PlSqlCheckVerifier {
    
    public static final String ISSUE_MARKER = "Noncompliant";

    public static final Map<String, IssueAttribute> ATTRIBUTE_MAP = ImmutableMap.<String, IssueAttribute>builder()
        .put("message", IssueAttribute.MESSAGE)
        .put("sc", IssueAttribute.START_COLUMN)
        .put("startColumn", IssueAttribute.START_COLUMN)
        .put("el", IssueAttribute.END_LINE)
        .put("endLine", IssueAttribute.END_LINE)
        .put("ec", IssueAttribute.END_COLUMN)
        .put("endColumn", IssueAttribute.END_COLUMN)
        .put("secondary", IssueAttribute.SECONDARY_LOCATIONS)
        .build();

    public enum IssueAttribute {
        MESSAGE,
        START_COLUMN,
        END_COLUMN,
        END_LINE,
        SECONDARY_LOCATIONS
    }

    private final ArrayListMultimap<Integer, Map<IssueAttribute, String>> expected = ArrayListMultimap.create();
    private boolean expectNoIssues = false;
    private String expectFileIssue;
    private Integer expectFileIssueOnline;
    
    public void expectNoIssues() {
        this.expectNoIssues = true;
    }
    
    public static void verify(String filename, SquidAstVisitor<Grammar> check) {
        scanFile(filename, check, new PlSqlCheckVerifier());
    }
    
    public static void verifyNoIssue(String filename, SquidAstVisitor<Grammar> check) {
        PlSqlCheckVerifier checkVerifier = new PlSqlCheckVerifier();
        checkVerifier.expectNoIssues();
        scanFile(filename, check, checkVerifier);
    }
    
    public String getExpectedIssueTrigger() {
        return "-- " + ISSUE_MARKER;
    }

    private static void scanFile(String filename, SquidAstVisitor<Grammar> check, PlSqlCheckVerifier plSqlCheckVerifier) {
        File file = new File(filename);
        
        DefaultInputFile inputFile;
        
        try {
            inputFile = new DefaultInputFile("key", filename).setLanguage("plsqlopen")
                    .initMetadata(Files.toString(file, Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        SensorContextTester context = SensorContextTester.create(new File("."));
        context.fileSystem().add(inputFile);
        
        ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
        
        SonarComponents components = new SonarComponents(resourcePerspectives, context, context.fileSystem()).getTestInstance();
        
        PlSqlCheck expectedIssueCollector = new ExpectedIssueCollector(plSqlCheckVerifier);
        
        PlSqlAstScanner.scanSingleFile(file, components, ImmutableList.of(new SymbolVisitor(), check, expectedIssueCollector));
        Collection<AnalyzerMessage> issues = ((SonarComponents.Test) components).getIssues();
        
        plSqlCheckVerifier.checkIssues(issues);
    }

    private void checkIssues(Collection<AnalyzerMessage> issues) {
        if (expectNoIssues) {
            assertNoIssues(issues);
        } else if (StringUtils.isNotEmpty(expectFileIssue)) {
            assertSingleIssue(issues);
        } else {
            assertMultipleIssue(issues);
        }
    }

    private void assertNoIssues(Collection<AnalyzerMessage> issues) {
        assertThat(issues).overridingErrorMessage("No issues expected but got: " + issues).isEmpty();
        assertThat(expected.isEmpty())
                .overridingErrorMessage("The file should not declare noncompliants when no issues are expected")
                .isTrue();
    }
    
    private void assertSingleIssue(Collection<AnalyzerMessage> issues) {
        Preconditions.checkState(issues.size() == 1, "A single issue is expected with line " + expectFileIssueOnline);
        AnalyzerMessage issue = Iterables.getFirst(issues, null);
        assertThat(issue.getLine()).isEqualTo(expectFileIssueOnline);
        assertThat(issue.getText(Locale.ENGLISH)).isEqualTo(expectFileIssue);
    }

    private void assertMultipleIssue(Collection<AnalyzerMessage> issues) {
        Preconditions.checkState(!issues.isEmpty(), "At least one issue expected");
        List<Integer> unexpectedLines = Lists.newLinkedList();
        for (AnalyzerMessage issue : issues) {
            validateIssue(expected, unexpectedLines, issue);
        }
        if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
            Collections.sort(unexpectedLines);
            String expectedMsg = !expected.isEmpty() ? ("Expected " + expected) : "";
            String unexpectedMsg = !unexpectedLines.isEmpty()
                    ? ((expectedMsg.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines) : "";
            Fail.fail(expectedMsg + unexpectedMsg);
        }
    }
    
    private static void validateIssue(Multimap<Integer, Map<IssueAttribute, String>> expected,
            List<Integer> unexpectedLines, AnalyzerMessage issue) {
        int line = issue.getLine();
        if (expected.containsKey(line)) {
            Map<IssueAttribute, String> attrs = Iterables.getLast(expected.get(line));
            assertEquals(issue.getText(Locale.ENGLISH), attrs, IssueAttribute.MESSAGE);
            validateAnalyzerMessage(attrs, issue);
            expected.remove(line, attrs);
        } else {
            unexpectedLines.add(line);
        }
    }
    
    private static void validateAnalyzerMessage(Map<IssueAttribute, String> attrs, AnalyzerMessage analyzerMessage) {
        AnalyzerMessage.TextSpan textSpan = analyzerMessage.getLocation();
        if (textSpan != null) {
            assertEquals(normalizeColumn(textSpan.startCharacter), attrs, IssueAttribute.START_COLUMN);
            assertEquals(Integer.toString(textSpan.endLine), attrs, IssueAttribute.END_LINE);
            assertEquals(normalizeColumn(textSpan.endCharacter), attrs, IssueAttribute.END_COLUMN);
        }
        if (attrs.containsKey(IssueAttribute.SECONDARY_LOCATIONS)) {
            List<AnalyzerMessage> secondaryLocations = analyzerMessage.getSecondaryLocations();
            Multiset<String> actualLines = HashMultiset.create();
            for (AnalyzerMessage secondaryLocation : secondaryLocations) {
                actualLines.add(Integer.toString(secondaryLocation.getLine()));
            }
            List<String> expected = Lists.newArrayList(Splitter.on(",").omitEmptyStrings().trimResults()
                    .split(attrs.get(IssueAttribute.SECONDARY_LOCATIONS)));
            List<String> unexpected = new ArrayList<>();
            for (String actualLine : actualLines) {
                if (expected.contains(actualLine)) {
                    expected.remove(actualLine);
                } else {
                    unexpected.add(actualLine);
                }
            }
            if (!expected.isEmpty() || !unexpected.isEmpty()) {
                Fail.fail("Secondary locations: expected: " + expected + " unexpected:" + unexpected);
            }
        }
    }
    
    private static String normalizeColumn(int startCharacter) {
        return Integer.toString(startCharacter + 1);
    }
    
    private static void assertEquals(String value, Map<IssueAttribute, String> attributes, IssueAttribute attribute) {
        if (attributes.containsKey(attribute)) {
            assertThat(value).as("attribute mismatch for " + attribute + ": " + attributes)
                    .isEqualTo(attributes.get(attribute));
        }
    }
    
    protected void collectExpectedIssues(String comment, int line) {
        String expectedStart = getExpectedIssueTrigger();
        if (comment.startsWith(expectedStart)) {
            String cleanedComment = StringUtils.remove(comment, expectedStart);

            EnumMap<IssueAttribute, String> attr = new EnumMap<>(IssueAttribute.class);
            String expectedMessage = StringUtils.substringBetween(cleanedComment, "{{", "}}");
            if (StringUtils.isNotEmpty(expectedMessage)) {
                attr.put(IssueAttribute.MESSAGE, expectedMessage);
            }
            int expectedLine = line;
            String attributesSubstr = extractAttributes(comment, attr);

            cleanedComment = StringUtils
                    .stripEnd(StringUtils.remove(StringUtils.remove(cleanedComment, "[[" + attributesSubstr + "]]"),
                            "{{" + expectedMessage + "}}"), " \t");
            if (StringUtils.startsWith(cleanedComment, "@")) {
                final int lineAdjustment;
                final char firstChar = cleanedComment.charAt(1);
                final int endIndex = cleanedComment.indexOf(' ');
                if (endIndex == -1) {
                    lineAdjustment = Integer.parseInt(cleanedComment.substring(2));
                } else {
                    lineAdjustment = Integer.parseInt(cleanedComment.substring(2, endIndex));
                }
                if (firstChar == '+') {
                    expectedLine += lineAdjustment;
                } else if (firstChar == '-') {
                    expectedLine -= lineAdjustment;
                } else {
                    Fail.fail("Use only '@+N' or '@-N' to shifts messages.");
                }
            }
            updateEndLine(expectedLine, attr);
            expected.put(expectedLine, attr);
        }
    }
    
    private static String extractAttributes(String comment, Map<IssueAttribute, String> attr) {
        String attributesSubstr = StringUtils.substringBetween(comment, "[[", "]]");
        if (!StringUtils.isEmpty(attributesSubstr)) {
            Iterable<String> attributes = Splitter.on(";").split(attributesSubstr);
            for (String attribute : attributes) {
                String[] split = StringUtils.split(attribute, '=');
                if (split.length == 2 && PlSqlCheckVerifier.ATTRIBUTE_MAP.containsKey(split[0])) {
                    attr.put(PlSqlCheckVerifier.ATTRIBUTE_MAP.get(split[0]), split[1]);
                } else {
                    Fail.fail("// Noncompliant attributes not valid: " + attributesSubstr);
                }
            }
        }
        return attributesSubstr;
    }
    
    private static void updateEndLine(int expectedLine, EnumMap<IssueAttribute, String> attr) {
        if (attr.containsKey(IssueAttribute.END_LINE)) {
            String endLineStr = attr.get(IssueAttribute.END_LINE);
            if (endLineStr.charAt(0) == '+') {
                int endLine = Integer.parseInt(endLineStr);
                attr.put(IssueAttribute.END_LINE, Integer.toString(expectedLine + endLine));
            } else {
                Fail.fail("endLine attribute should be relative to the line and must be +N with N integer");
            }
        }
    }
    
    private static class ExpectedIssueCollector extends PlSqlCheck implements AstAndTokenVisitor {

        private final PlSqlCheckVerifier verifier;

        public ExpectedIssueCollector(PlSqlCheckVerifier verifier) {
            this.verifier = verifier;
        }

        @Override
        public void visitToken(Token token) {
            for (Trivia trivia : token.getTrivia()) {
                verifier.collectExpectedIssues(trivia.getToken().getOriginalValue(), trivia.getToken().getLine());
            }
        }
    }

}
