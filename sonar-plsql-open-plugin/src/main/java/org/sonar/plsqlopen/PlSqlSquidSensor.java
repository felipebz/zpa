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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
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

import com.google.common.collect.Lists;

public class PlSqlSquidSensor implements Sensor {

    private final PlSqlChecks checks;
    private final boolean isErrorRecoveryEnabled;

    private FormsMetadata formsMetadata;
    private NoSonarFilter noSonarFilter;
	private FileLinesContextFactory fileLinesContextFactory;
    private static final Logger LOG = Loggers.get(PlSqlAstScanner.class);

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
    
    @Override
    public void describe(@Nonnull SensorDescriptor descriptor) {
        descriptor
            .name("Z PL/SQL Analyzer")
            .onlyOnLanguage(PlSql.KEY);
    }

    @Override
    public void execute(@Nonnull SensorContext context) {
        FilePredicates p = context.fileSystem().predicates();
        ArrayList<InputFile> inputFiles = Lists.newArrayList(context.fileSystem().inputFiles(p.hasLanguage(PlSql.KEY)));
        
        ProgressReport progressReport = new ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10));
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, checks, noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory);
        
        progressReport.start(inputFiles.stream().map(InputFile::toString).collect(Collectors.toList()));
      //(******************************** Custom Code Starts *****************************************//
        try {
			String projectDir = System.getProperty("user.dir");
	        String projectName = projectDir.substring(projectDir.lastIndexOf("\\")+1);
	    	String targetIssues = (projectDir+"\\"+projectName+"_ISSUES.csv");
	    	targetIssues = targetIssues.replaceAll("\\\\", "\\\\\\\\");
	    	
	    	String targetMetrics = (projectDir+"\\"+projectName+"_METRICS.csv");
	    	targetMetrics = targetMetrics.replaceAll("\\\\", "\\\\\\\\");
	    	
	    	
	    	File targetIssuesCsv = new File(targetIssues);
	    	File targetMetricsCsv = new File(targetMetrics);
	    	
	    	if(targetIssuesCsv.exists()) { //If file already exists
	    		Files.deleteIfExists(Paths.get(targetIssues));
	    		//LOG.debug("***************************deleted*******************************\n"+targetCsv);
	    	}
	    	
	    	if(targetMetricsCsv.exists()) { //If file already exists
	    		Files.deleteIfExists(Paths.get(targetMetrics));
	    		//LOG.debug("***************************deleted*******************************\n"+targetCsv);
	    	}
	    	
	    	BufferedWriter writerIssues = new BufferedWriter(new FileWriter(targetIssues, true));
	    	if(targetIssuesCsv.length() == 0) { //If file is empty
	    		writerIssues.append("\"ProjectDirectory\",");
	    		//writerIssues.append("\"ProjectName\",");
	    		writerIssues.append("\"FilePath\",");
	    		//writerIssues.append("\"FileDirectory\",");
	    		writerIssues.append("\"FileName\",");
	    		writerIssues.append("\"Category\",");
	    		writerIssues.append("\"Type\",");
	    		writerIssues.append("\"Object\",");
	    		writerIssues.append("\"Message\",");
	    		writerIssues.append("\"LineNo START\",");
	    		//writerIssues.append("\"StartLineCol START\",");
	    		//writerIssues.append("\"LineNo END\",");
	    		//writerIssues.append("\"EndLineCol END\",");
	    		writerIssues.append("\"LineContent\"\n");
	    		//LOG.debug("***************************appended*******************************\n"+"Headers");
	    	}

	    	BufferedWriter writerMetrics = new BufferedWriter(new FileWriter(targetMetrics, true));
	    	if(targetMetricsCsv.length() == 0) { //If file is empty
	    		writerMetrics.append("\"ProjectDirectory\",");
	    		writerMetrics.append("\"FilePath\",");
	    		writerMetrics.append("\"FileName\",");
	    		writerMetrics.append("\"TotalLines\",");
	    		writerMetrics.append("\"LineOfCodes\",");
	    		writerMetrics.append("\"Statements\",");
	    		writerMetrics.append("\"CommentLines\",");
	    		writerMetrics.append("\"Functions\",");
	    		writerMetrics.append("\"Complexity\"\n");
	    		//LOG.debug("***************************appended*******************************\n"+"Headers");
	    	}
	        for (InputFile inputFile : inputFiles) {
	            scanner.scanFile(inputFile,writerIssues,writerMetrics);
	            
	            progressReport.nextFile();
	        }
	        progressReport.stop();
	        writerIssues.close();
	        writerMetrics.close();
    	}catch(IOException e) {
    		LOG.debug(e.getStackTrace().toString());
    	}
        //(******************************** Custom Code Ends *****************************************//
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
