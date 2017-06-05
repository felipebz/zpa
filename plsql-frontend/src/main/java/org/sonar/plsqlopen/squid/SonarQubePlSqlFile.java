package org.sonar.plsqlopen.squid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;
import org.sonar.plsqlopen.PlSqlFile;

import com.google.common.io.Files;

public abstract class SonarQubePlSqlFile implements PlSqlFile {

    private static final Version V6_0 = Version.create(6, 0);
    private static final Version V6_2 = Version.create(6, 2);

    private final InputFile inputFile;

    private SonarQubePlSqlFile(InputFile inputFile) {
      this.inputFile = inputFile;
    }

    public static PlSqlFile create(InputFile inputFile, SensorContext context) {
        Version version = context.getSonarQubeVersion();
        if (version.isGreaterThanOrEqual(V6_2)) {
            return new Sq62File(inputFile);
        }
        if (version.isGreaterThanOrEqual(V6_0)) {
            return new Sq60File(inputFile);
        }
        return new Sq56File(inputFile, context.fileSystem().encoding());
    }

    @Override
    public String fileName() {
        return inputFile.file().getName();
    }

    @Override
    public InputFile inputFile() {
        return inputFile;
    }

    private static String contentForCharset(InputFile inputFile, Charset charset) {
        try {
            return Files.toString(inputFile.file(), charset);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read content of input file " + inputFile, e);
        }
    }

    private static class Sq56File extends SonarQubePlSqlFile {

        private final Charset fileSystemEncoding;

        public Sq56File(InputFile inputFile, Charset fileSystemEncoding) {
            super(inputFile);
            this.fileSystemEncoding = fileSystemEncoding;
        }

        @Override
        public String content() {
            return contentForCharset(inputFile(), fileSystemEncoding);
        }

        @Override
        public File file() {
            return inputFile().file();
        }

    }

    private static class Sq60File extends SonarQubePlSqlFile {

        public Sq60File(InputFile inputFile) {
            super(inputFile);
        }

        @Override
        public String content() {
            return contentForCharset(inputFile(), inputFile().charset());
        }

        @Override
        public File file() {
            return inputFile().file();
        }

    }

    private static class Sq62File extends SonarQubePlSqlFile {

        public Sq62File(InputFile inputFile) {
            super(inputFile);
        }

        @Override
        public String content() {
            try {
                return inputFile().contents();
            } catch (IOException e) {
                throw new IllegalStateException("Could not read content of input file " + inputFile(), e);
            }
        }

        @Override
        public File file() {
            return inputFile().file();
        }

    }

}
