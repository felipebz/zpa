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
package org.sonar.plsqlopen;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.ProgressReport;
import org.sonar.plugins.plsqlopen.api.CustomPlSqlRulesDefinition;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

public class PlSqlSquidSensor implements Sensor {

    private static final Logger LOG = Loggers.get(PlSqlSquidSensor.class);
    private final PlSqlChecks checks;
    private final boolean isErrorRecoveryEnabled;

    private FormsMetadata formsMetadata;
    private NoSonarFilter noSonarFilter;
	private FileLinesContextFactory fileLinesContextFactory;
    
    public PlSqlSquidSensor(CheckFactory checkFactory, Configuration settings, NoSonarFilter noSonarFilter) {
        this(checkFactory, settings, noSonarFilter, null, null);
    }

    public PlSqlSquidSensor(CheckFactory checkFactory, Configuration settings, NoSonarFilter noSonarFilter,
    		FileLinesContextFactory fileLinesContextFactory, 
            @Nullable CustomPlSqlRulesDefinition[] customRulesDefinition) {
        this.noSonarFilter = noSonarFilter;
		this.fileLinesContextFactory = fileLinesContextFactory;
        this.checks = PlSqlChecks.createPlSqlCheck(checkFactory)
                .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
                .addCustomChecks(customRulesDefinition);
        this.formsMetadata = FormsMetadata.loadFromFile(settings.get(PlSqlPlugin.FORMS_METADATA_KEY).orElse(null));
        isErrorRecoveryEnabled = settings.getBoolean(PlSqlPlugin.ERROR_RECOVERY_KEY).orElse(false);
    }
    
    @VisibleForTesting
    FormsMetadata getFormsMetadata() {
        return formsMetadata;
    }
    
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("Z PL/SQL Analyzer")
            .onlyOnLanguage(PlSql.KEY);
    }

    @Override
    public void execute(SensorContext context) {        
        FilePredicates p = context.fileSystem().predicates();
        ArrayList<InputFile> inputFiles = Lists.newArrayList(context.fileSystem().inputFiles(p.hasLanguage(PlSql.KEY)));
        
        ProgressReport progressReport = new ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10));
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, checks, noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory);
        
        progressReport.start(inputFiles.stream().map(InputFile::toString).collect(Collectors.toList()));
        for (InputFile inputFile : inputFiles) {
            scanner.scanFile(inputFile);
            
            progressReport.nextFile();
        }
        progressReport.stop();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
