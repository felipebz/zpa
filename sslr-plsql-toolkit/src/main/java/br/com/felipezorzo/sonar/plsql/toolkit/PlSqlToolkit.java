package br.com.felipezorzo.sonar.plsql.toolkit;
import java.util.List;

import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.toolkit.Toolkit;

import com.google.common.collect.ImmutableList;

import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;

public final class PlSqlToolkit {
    private PlSqlToolkit() {
    }

    public static void main(String[] args) {
      Toolkit toolkit = new Toolkit("SSLR :: PL/SQL :: Toolkit", new PlSqlConfigurationModel());
      toolkit.run();
    }

    public static List<Tokenizer> getPythonTokenizers() {
      return ImmutableList.of(
        (Tokenizer) new KeywordsTokenizer("<span class=\"k\">", "</span>", PlSqlKeyword.keywordValues()));
    }
}
