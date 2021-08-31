package com.company.plsql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlSqlCustomRulesPluginTest {

    @Test
    public void testRepository() {
        PlSqlCustomRulesDefinition plugin = new PlSqlCustomRulesDefinition();
        assertEquals("Company", plugin.repositoryName());
        assertEquals("my-rules", plugin.repositoryKey());
        assertEquals(1, plugin.checkClasses().length);
    }

}
