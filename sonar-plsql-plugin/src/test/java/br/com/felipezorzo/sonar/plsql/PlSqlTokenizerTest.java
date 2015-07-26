package br.com.felipezorzo.sonar.plsql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;

import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokens;

public class PlSqlTokenizerTest {

    @Test
    public void shouldWorkOnValidInput() throws URISyntaxException {
      File file = new File(getClass().getResource("/br/com/felipezorzo/sonar/plsql/code.sql").toURI());
      SourceCode source = new SourceCode(new SourceCode.FileCodeLoader(file, "key"));
      Tokens cpdTokens = new Tokens();
      PlSqlTokenizer tokenizer = new PlSqlTokenizer(Charset.forName("UTF-8"));
      tokenizer.tokenize(source, cpdTokens);
      List<TokenEntry> list = cpdTokens.getTokens();
      assertThat(list.size()).isEqualTo(28);
    }
    
}
