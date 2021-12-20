package com.company.plsql;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlSqlCustomRulesDefinitionTest {

    @Test
    public void test() {
        Plugin.Context context = new Plugin.Context(SonarRuntimeImpl.forSonarQube(Version.create(6, 0), SonarQubeSide.SERVER, SonarEdition.COMMUNITY));
        PlSqlCustomRulesPlugin plugin = new PlSqlCustomRulesPlugin();
        plugin.define(context);
        assertEquals(1, context.getExtensions().size());
    }

}
