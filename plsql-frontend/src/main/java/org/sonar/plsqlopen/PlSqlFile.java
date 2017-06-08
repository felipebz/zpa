package org.sonar.plsqlopen;

import java.io.File;

import org.sonar.api.batch.fs.InputFile;

public interface PlSqlFile {

    InputFile inputFile();
    
    String content();

    String fileName();
    
    File file();
}
