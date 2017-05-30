package org.sonar.plsqlopen;

import java.io.File;

public interface PlSqlFile {

    String content();

    String fileName();
    
    File file();
}
