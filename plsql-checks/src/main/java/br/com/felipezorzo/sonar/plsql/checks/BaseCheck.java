package br.com.felipezorzo.sonar.plsql.checks;

import java.util.Locale;
import java.util.ResourceBundle;

import org.sonar.squidbridge.checks.SquidCheck;

import com.sonar.sslr.api.Grammar;

public class BaseCheck extends SquidCheck<Grammar> {
    
    private final ResourceBundle bundle;
    
    protected BaseCheck() {
        bundle = ResourceBundle.getBundle("org.sonar.l10n.plsql", Locale.getDefault());
    }
   
    protected String getLocalizedMessage(String checkKey) {
        String key = "rule.plsql." + checkKey + ".message";
        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        return null;
    }
    
}
