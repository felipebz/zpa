package org.sonar.plsqlopen.squid;

import java.io.IOException;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plsqlopen.PlSqlFile;

public class SonarQubePlSqlFile implements PlSqlFile {

    private final InputFile inputFile;

    private SonarQubePlSqlFile(InputFile inputFile) {
      this.inputFile = inputFile;
    }

    public static PlSqlFile create(InputFile inputFile, SensorContext context) {
        return new SonarQubePlSqlFile(inputFile);
    }

    @Override
    public String fileName() {
        return inputFile.filename();
    }

    @Override
    public InputFile inputFile() {
        return inputFile;
    }
    
    @Override
    public String content() {
        try {
            return inputFile().contents();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read content of input file " + inputFile(), e);
        }
    }

}
