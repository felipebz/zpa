package br.com.felipezorzo.sonar.plsql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.sonar.api.config.Settings;

import com.google.common.collect.Maps;

public class PlSqlTest {
    
    @Test
    public void test() {
        PlSql language = new PlSql(new Settings());
        assertThat(language.getKey()).isEqualTo("plsql");
        assertThat(language.getName()).isEqualTo("PL/SQL");
        assertThat(language.getFileSuffixes())
            .hasSize(4)
            .contains("sql")
            .contains("pkg")
            .contains("pks")
            .contains("pkb");
    }
    
    @Test
    public void custom_file_suffixes() {
        Map<String, String> props = Maps.newHashMap();
        props.put(PlSqlPlugin.FILE_SUFFIXES_KEY, "sql, custom");

        Settings settings = new Settings();
        settings.addProperties(props);

        PlSql language = new PlSql(settings);
        assertThat(language.getFileSuffixes()).hasSize(2).contains("custom");
    }
    
}
