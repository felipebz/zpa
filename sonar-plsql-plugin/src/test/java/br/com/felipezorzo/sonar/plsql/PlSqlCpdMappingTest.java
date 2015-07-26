package br.com.felipezorzo.sonar.plsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;

public class PlSqlCpdMappingTest {

    @Test
    public void test() {
        PlSql language = mock(PlSql.class);
        FileSystem fs = mock(FileSystem.class);
        PlSqlCpdMapping mapping = new PlSqlCpdMapping(language, fs);
        assertThat(mapping.getLanguage()).isSameAs(language);
        assertThat(mapping.getTokenizer()).isInstanceOf(PlSqlTokenizer.class);
    }

}
