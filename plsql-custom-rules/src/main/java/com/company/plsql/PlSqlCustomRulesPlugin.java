package com.company.plsql;

import org.sonar.api.Plugin;
import org.sonar.plsqlopen.CustomPlSqlRulesDefinition;

public class PlSqlCustomRulesPlugin extends CustomPlSqlRulesDefinition implements Plugin {
    
    @Override
    public void define(org.sonar.api.Plugin.Context context) {
        context.addExtension(PlSqlCustomRulesPlugin.class);
    }
    
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
