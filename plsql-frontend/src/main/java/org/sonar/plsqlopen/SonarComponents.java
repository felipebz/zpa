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
package org.sonar.plsqlopen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.metadata.FormsMetadata;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

@BatchSide
public class SonarComponents {

    private static final Logger LOG = Loggers.get(SonarComponents.class);

    private final SensorContext context;
    private PlSqlChecks checks;
    private FileSystem fs;
    private FormsMetadata formsMetadata;
    
    public SonarComponents(SensorContext context) {
        this.context = context;
        this.fs = context.fileSystem();
    }
    
    public FormsMetadata getFormsMetadata() {
        return this.formsMetadata;
    }
    
    @VisibleForTesting
    public void setFormsMetadata(FormsMetadata metadata) {
        this.formsMetadata = metadata;
    }
    
    public NewSymbolTable symbolizableFor(InputFile inputPath) {
        return context.newSymbolTable().onFile(inputPath);
    }
    
    public void setChecks(PlSqlChecks checks) {
        this.checks = checks;
    }

    public InputFile inputFromIOFile(File file) {
        return fs.inputFile(fs.predicates().is(file));
    }

    public void reportIssue(AnalyzerMessage message, InputFile inputFile) {    
        RuleKey ruleKey = checks.ruleKey((PlSqlCheck) message.getCheck());
        reportIssue(inputFile, ruleKey, message);
    }

    @VisibleForTesting
    void reportIssue(InputFile inputFile, RuleKey key, AnalyzerMessage message) {
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
                    LOG.debug("Fail to set primary location", e);
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
            } catch (IllegalArgumentException e) {
                LOG.debug("addSecondaryLocation FAIL", e);
            }
        }
        issue.save();
    }
    
    public void loadMetadataFile(String metadataFile) {
        if (Strings.isNullOrEmpty(metadataFile)) {
            return;
        }
        
        try (JsonReader reader = new JsonReader(new FileReader(metadataFile))) {
            this.formsMetadata = new Gson().fromJson(reader, FormsMetadata.class);
        } catch (FileNotFoundException e) {
            LOG.warn("The metadata file {} was not found.", metadataFile);
        } catch (IOException e) {
            LOG.error("Error reading the metadata file at {}.", metadataFile, e);
        }
    }
    
    @VisibleForTesting
    public SonarComponents getTestInstance() {
        return new Test(context);
    }

    public class Test extends SonarComponents {

        private Collection<AnalyzerMessage> messages = new HashSet<>();

        public Test(SensorContext context) {
            super(context);
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
