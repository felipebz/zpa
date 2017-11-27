package com.company.plsql;

import org.sonar.api.Plugin;

public class PlSqlCustomRulesPlugin implements Plugin {
    
    @Override
    public void define(Plugin.Context context) {
        context.addExtension(PlSqlCustomRulesDefinition.class);
    }

}
