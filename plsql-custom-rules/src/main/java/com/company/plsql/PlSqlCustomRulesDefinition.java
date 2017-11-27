package com.company.plsql;

import org.sonar.plsqlopen.CustomPlSqlRulesDefinition;

public class PlSqlCustomRulesDefinition extends CustomPlSqlRulesDefinition {
    
    @Override
    public String repositoryName() {
        return "Company";
    }

    @Override
    public String repositoryKey() {
        return "my-rules";
    }

    @Override
    public Class[] checkClasses() {
        return new Class[] {
            ForbiddenDmlCheck.class
        };
    }

}
