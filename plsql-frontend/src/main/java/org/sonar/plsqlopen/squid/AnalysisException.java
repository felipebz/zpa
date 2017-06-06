package org.sonar.plsqlopen.squid;

public class AnalysisException extends RuntimeException {

    public AnalysisException(String string, Throwable e) {
        super(string, e);
    }

}
