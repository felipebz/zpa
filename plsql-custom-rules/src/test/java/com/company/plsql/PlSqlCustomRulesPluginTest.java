package com.company.plsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PlSqlCustomRulesPluginTest {

    @Test
    public void testRepository() {
        PlSqlCustomRulesDefinition plugin = new PlSqlCustomRulesDefinition();
        assertThat(plugin.repositoryName()).isEqualTo("Company");
        assertThat(plugin.repositoryKey()).isEqualTo("my-rules");
        assertThat(plugin.checkClasses().length).isEqualTo(1);
    }

}
