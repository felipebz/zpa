package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CheckList {

    public static final String REPOSITORY_KEY = "plsql";

    public static final String SONAR_WAY_PROFILE = "Sonar way";

    private CheckList() {
    }
    
    @SuppressWarnings("rawtypes")
    public static List<Class> getChecks() {
        return ImmutableList.<Class>of(
            EmptyBlockCheck.class,
            ParsingErrorCheck.class,
            CollapsibleIfStatementsCheck.class,
            InequalityUsageCheck.class);
    }
}
