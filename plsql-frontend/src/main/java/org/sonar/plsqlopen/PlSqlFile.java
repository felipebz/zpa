package org.sonar.plsqlopen;

import org.sonar.api.batch.fs.InputFile;

public interface PlSqlFile {

    InputFile inputFile();
    
    String content();

    String fileName();
}
