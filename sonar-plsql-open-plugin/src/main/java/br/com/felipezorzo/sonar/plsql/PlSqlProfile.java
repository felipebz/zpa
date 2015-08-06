package br.com.felipezorzo.sonar.plsql;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.squidbridge.annotations.AnnotationBasedProfileBuilder;

import br.com.felipezorzo.sonar.plsql.checks.CheckList;

public class PlSqlProfile extends ProfileDefinition {

    private final RuleFinder ruleFinder;

    public PlSqlProfile(RuleFinder ruleFinder) {
      this.ruleFinder = ruleFinder;
    }

    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      AnnotationBasedProfileBuilder annotationBasedProfileBuilder = new AnnotationBasedProfileBuilder(ruleFinder);
      return annotationBasedProfileBuilder.build(
          CheckList.REPOSITORY_KEY,
          CheckList.SONAR_WAY_PROFILE,
          PlSql.KEY,
          CheckList.getChecks(),
          validation);
    }

}
