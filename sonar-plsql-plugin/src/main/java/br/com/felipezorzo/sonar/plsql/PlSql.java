package br.com.felipezorzo.sonar.plsql;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;

import com.google.common.collect.Lists;

public class PlSql extends AbstractLanguage {
    
    public static final String KEY = "plsql";
    
    private static final String[] DEFAULT_FILE_SUFFIXES = { "sql", "pkg", "pks", "pkb" };

    private Settings settings;

    public PlSql(Settings configuration) {
      super(KEY, "PL/SQL");
      this.settings = configuration;
    }

    public Settings getSettings() {
      return this.settings;
    }

    @Override
    public String[] getFileSuffixes() {
      String[] suffixes = filterEmptyStrings(settings.getStringArray(PlSqlPlugin.FILE_SUFFIXES_KEY));
      return suffixes.length == 0 ? DEFAULT_FILE_SUFFIXES : suffixes;
    }

    private String[] filterEmptyStrings(String[] stringArray) {
      List<String> nonEmptyStrings = Lists.newArrayList();
      for (String string : stringArray) {
        if (StringUtils.isNotBlank(string.trim())) {
          nonEmptyStrings.add(string.trim());
        }
      }
      return nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]);
    }

}
