package br.com.felipezorzo.sonar.plsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PlSqlPluginTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testGetExtensions() {
      assertThat(new PlSqlPlugin().getExtensions()).hasSize(6);
    }
    
}
