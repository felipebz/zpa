package br.com.felipezorzo.sonar.plsql;

import java.nio.charset.Charset;

public interface CharsetAwareVisitor {
    void setCharset(Charset charset);
}
