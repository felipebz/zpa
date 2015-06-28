package br.com.felipezorzo.sonar.plsql;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.squidbridge.annotations.AnnotationBasedRulesDefinition;

import br.com.felipezorzo.sonar.plsql.checks.CheckList;

public class PlSqlRuleRepository implements RulesDefinition {

    private static final String REPOSITORY_NAME = "PL/SQL";

    @Override
    public void define(Context context) {
      NewRepository repository = context
          .createRepository(CheckList.REPOSITORY_KEY, PlSql.KEY)
          .setName(REPOSITORY_NAME);
      AnnotationBasedRulesDefinition.load(repository, PlSql.KEY, CheckList.getChecks());
      repository.done();
    }

}
