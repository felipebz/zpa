package com.felipebz.zpa.probe;

import org.sonar.plugins.plsqlopen.api.CustomPlSqlRulesDefinition;

public class OracleProbeRulesDefinition extends CustomPlSqlRulesDefinition {
    @Override
    public String repositoryName() {
        return "ZPA Oracle metadata probe";
    }

    @Override
    public String repositoryKey() {
        return "zpa-oracle-probe";
    }

    @Override
    public Class[] checkClasses() {
        return new Class[] { ExternalTypeProbeCheck.class };
    }
}
