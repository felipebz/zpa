package br.com.felipezorzo.sonar.plsql.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class ExtractDatetimeExpressionTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesExtractDay() {
        assertThat(p).matches("extract(day from foo)");
    }
    
    @Test
    public void matchesExtractMonth() {
        assertThat(p).matches("extract(month from foo)");
    }
    
    @Test
    public void matchesExtractYear() {
        assertThat(p).matches("extract(year from foo)");
    }
    
    @Test
    public void matchesExtractYour() {
        assertThat(p).matches("extract(hour from foo)");
    }
    
    @Test
    public void matchesExtractMinute() {
        assertThat(p).matches("extract(minute from foo)");
    }
    
    @Test
    public void matchesExtractSecond() {
        assertThat(p).matches("extract(second from foo)");
    }
    
    @Test
    public void matchesExtractTimezoneHour() {
        assertThat(p).matches("extract(timezone_hour from foo)");
    }
    
    @Test
    public void matchesExtractTimezoneMinute() {
        assertThat(p).matches("extract(timezone_minute from foo)");
    }
    
    @Test
    public void matchesExtractTimezoneRegion() {
        assertThat(p).matches("extract(timezone_region from foo)");
    }
    
    @Test
    public void matchesExtractTimezoneAbbreviation() {
        assertThat(p).matches("extract(timezone_abbr from foo)");
    }

}
