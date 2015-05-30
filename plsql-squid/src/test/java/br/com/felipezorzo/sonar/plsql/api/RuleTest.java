package br.com.felipezorzo.sonar.plsql.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.parser.PlSqlParser;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class RuleTest {
    protected Parser<Grammar> p = PlSqlParser.create(new PlSqlConfiguration(Charsets.UTF_8));

    protected void setRootRule(GrammarRuleKey ruleKey) {
        p.setRootRule(p.getGrammar().rule(ruleKey));
    }
}
