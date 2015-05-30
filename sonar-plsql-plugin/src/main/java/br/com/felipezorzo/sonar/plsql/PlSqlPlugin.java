package br.com.felipezorzo.sonar.plsql;

import com.google.common.collect.ImmutableList;

import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class PlSqlPlugin extends SonarPlugin {

  public static final String FILE_SUFFIXES_KEY = "sonar.plsql.file.suffixes";

  @SuppressWarnings("rawtypes")
  @Override
  public List getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(FILE_SUFFIXES_KEY)
          .name("File Suffixes")
          .description("Comma-separated list of suffixes of PL/SQL files to analyze.")
          .category("PL/SQL")
          .onQualifiers(Qualifiers.PROJECT)
          .defaultValue("sql")
          .build());
  }

}
