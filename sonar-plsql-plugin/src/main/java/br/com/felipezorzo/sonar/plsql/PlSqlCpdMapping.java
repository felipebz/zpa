package br.com.felipezorzo.sonar.plsql;

import java.nio.charset.Charset;

import net.sourceforge.pmd.cpd.Tokenizer;

import org.sonar.api.batch.AbstractCpdMapping;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.resources.Language;

public class PlSqlCpdMapping extends AbstractCpdMapping {

    private final PlSql language;
    private final Charset charset;

    public PlSqlCpdMapping(PlSql language, FileSystem fs) {
      this.language = language;
      this.charset = fs.encoding();
    }

    @Override
    public Tokenizer getTokenizer() {
      return new PlSqlTokenizer(charset);
    }

    @Override
    public Language getLanguage() {
      return language;
    }

}
