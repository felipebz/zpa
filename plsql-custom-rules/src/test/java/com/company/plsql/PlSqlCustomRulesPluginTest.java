package com.company.plsql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeVersion;

import com.company.plsql.PlSqlCustomRulesPlugin;

public class PlSqlCustomRulesPluginTest {

    @Test
    public void test() {
        Plugin.Context context = new Plugin.Context(SonarQubeVersion.V5_6);
        PlSqlCustomRulesPlugin plugin = new PlSqlCustomRulesPlugin();
        plugin.define(context);
        assertThat(context.getExtensions().size()).isEqualTo(1);
    }
    
    @Test
    public void testRepository() {
        PlSqlCustomRulesPlugin plugin = new PlSqlCustomRulesPlugin();
        assertThat(plugin.repositoryName()).isEqualTo("Company");
        assertThat(plugin.repositoryKey()).isEqualTo("my-rules");
        assertThat(plugin.checkClasses().length).isEqualTo(1);
    }
    
}
