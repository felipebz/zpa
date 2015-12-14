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
package org.sonar.plsqlopen;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.source.Symbolizable;
import org.sonar.squidbridge.SquidAstVisitor;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.sslr.api.Grammar;

public class SonarComponents implements BatchExtension {

    private static final boolean IS_SONARQUBE_52 = isSonarQube52();
    private static final Logger LOG = LoggerFactory.getLogger(SonarComponents.class);

    private final ResourcePerspectives resourcePerspectives;
    private final SensorContext context;
    private PlSqlChecks checks;
    private FileSystem fs;

    public SonarComponents(ResourcePerspectives resourcePerspectives, SensorContext context, FileSystem fs) {
        this.resourcePerspectives = resourcePerspectives;
        this.context = context;
        this.fs = fs;
    }
    
    public Issuable issuableFor(InputPath inputPath) {
        return resourcePerspectives.as(Issuable.class, inputPath);
    }
    
    public Symbolizable symbolizableFor(InputPath inputPath) {
        return resourcePerspectives.as(Symbolizable.class, inputPath);
    }
    
    public void setChecks(PlSqlChecks checks) {
        this.checks = checks;
    }

    public InputFile inputFromIOFile(File file) {
        return fs.inputFile(fs.predicates().is(file));
    }

    public void reportIssue(AnalyzerMessage message, InputFile inputFile) {    
        @SuppressWarnings("unchecked")
        RuleKey ruleKey = checks.ruleKey((SquidAstVisitor<Grammar>) message.getCheck());

        if (IS_SONARQUBE_52) {
            reportIssueAfterSQ52(inputFile, ruleKey, message);
        } else {
            reportIssueBeforeSQ52(inputFile, ruleKey, message.getText(Locale.ENGLISH), message.getLine());
        }
    }

    @VisibleForTesting
    void reportIssueAfterSQ52(InputFile inputFile, RuleKey key, AnalyzerMessage message) {
        PlSqlIssue issue = PlSqlIssue.create(context, key, message.getCost());
        String text = message.getText(Locale.ENGLISH);
        Integer line = message.getLine();
        if (line == null) {
            // either an issue at file or folder level
            issue.setPrimaryLocationOnFile(inputFile, text);
        } else {
            AnalyzerMessage.TextSpan location = message.getLocation();
            if (location != null) {
                int column = message.getLocation().startCharacter;
                int endLine = message.getLocation().endLine;
                int endColumn = message.getLocation().endCharacter;
                
                try {
                    issue.setPrimaryLocation(inputFile, text, line, column, endLine, endColumn);
                } catch (IllegalArgumentException e) {
                    // the previous setPrimaryLocation will fail if it is a multiline token
                    // for now, just fall back to old method
                    issue.setPrimaryLocation(inputFile, text, line, -1, 0, -1);
                }
            } else {
                issue.setPrimaryLocation(inputFile, text, line, -1, 0, -1);
            }
        }
        for (AnalyzerMessage location : message.getSecondaryLocations()) {
            AnalyzerMessage.TextSpan secondarySpan = location.getLocation();
            
            String secondaryText = location.getText(Locale.ENGLISH);
            try {
                issue.addSecondaryLocation(inputFile, secondarySpan.startLine, secondarySpan.startCharacter, secondarySpan.endLine, secondarySpan.endCharacter, secondaryText);
                LOG.debug("addSecondaryLocation SUCESS");
            } catch (IllegalArgumentException e) {
                LOG.debug("addSecondaryLocation FAIL", e);
            }
        }
        issue.save();
    }

    private void reportIssueBeforeSQ52(InputFile inputFile, RuleKey key, String message, @Nullable Integer line) {
        Issuable issuable = issuableFor(inputFile);

        if (issuable != null) {
            Issue issue = issuable.newIssueBuilder().ruleKey(key)
                    .line(line)
                    .message(message).build();
            issuable.addIssue(issue);
        }
    }

    private static boolean isSonarQube52() {
        try {
            Issuable.IssueBuilder.class.getMethod("newLocation");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @VisibleForTesting
    public SonarComponents getTestInstance() {
        return new Test(resourcePerspectives, context, fs);
    }

    public class Test extends SonarComponents {

        private Collection<AnalyzerMessage> messages = new HashSet<>();

        public Test(ResourcePerspectives resourcePerspectives, SensorContext context, FileSystem fs) {
            super(resourcePerspectives, context, fs);
        }

        @Override
        public void reportIssue(AnalyzerMessage message, InputFile inputFile) {    
            messages.add(message);
        }

        public Collection<AnalyzerMessage> getIssues() {
            return messages;
        }
    }
}
