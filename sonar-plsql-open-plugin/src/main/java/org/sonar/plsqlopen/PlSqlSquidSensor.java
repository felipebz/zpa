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
package org.sonar.plsqlopen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plsqlopen.squid.ProgressReport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class PlSqlSquidSensor implements Sensor {

    private static final Logger LOG = Loggers.get(PlSqlSquidSensor.class);
    private final PlSqlChecks checks;
    private final boolean isErrorRecoveryEnabled;

    private SensorContext context;
    private FormsMetadata formsMetadata;
    private NoSonarFilter noSonarFilter;
    
    public PlSqlSquidSensor(CheckFactory checkFactory, Settings settings, NoSonarFilter noSonarFilter) {
        this(checkFactory, settings, noSonarFilter, null);
    }

    public PlSqlSquidSensor(CheckFactory checkFactory, Settings settings, NoSonarFilter noSonarFilter,
            @Nullable CustomPlSqlRulesDefinition[] customRulesDefinition) {
        this.noSonarFilter = noSonarFilter;
        this.checks = PlSqlChecks.createPlSqlCheck(checkFactory)
                .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
                .addCustomChecks(customRulesDefinition);
        loadMetadataFile(settings.getString(PlSqlPlugin.FORMS_METADATA_KEY));
        isErrorRecoveryEnabled = settings.getBoolean(PlSqlPlugin.ERROR_RECOVERY_KEY);
    }
    
    @VisibleForTesting
    FormsMetadata getFormsMetadata() {
        return formsMetadata;
    }
    
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("PlsqlSquidSensor")
            .onlyOnLanguage(PlSql.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        this.context = context;
        
        FilePredicates p = context.fileSystem().predicates();
        ArrayList<InputFile> inputFiles = Lists.newArrayList(context.fileSystem().inputFiles(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY))));
        
        ProgressReport progressReport = new ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10));
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, checks.all(), noSonarFilter, formsMetadata, isErrorRecoveryEnabled);
        
        progressReport.start(inputFiles);
        for (InputFile inputFile : inputFiles) {
            Collection<AnalyzerMessage> issues = scanner.scanFile(inputFile);
            for (AnalyzerMessage analyzerMessage : issues) {
                reportIssue(inputFile, analyzerMessage);
            }
            
            progressReport.nextFile();
        }
        progressReport.stop();
    }
    
    public void loadMetadataFile(String metadataFile) {
        if (Strings.isNullOrEmpty(metadataFile)) {
            return;
        }
        
        try (JsonReader reader = new JsonReader(new FileReader(metadataFile))) {
            this.formsMetadata = new Gson().fromJson(reader, FormsMetadata.class);
        } catch (FileNotFoundException e) {
            LOG.warn("The metadata file {} was not found.", metadataFile, e);
        } catch (IOException e) {
            LOG.error("Error reading the metadata file at {}.", metadataFile, e);
        }
    }
    
    @VisibleForTesting
    void reportIssue(InputFile inputFile, AnalyzerMessage message) {
        RuleKey key = checks.ruleKey((PlSqlCheck) message.getCheck());
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
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
