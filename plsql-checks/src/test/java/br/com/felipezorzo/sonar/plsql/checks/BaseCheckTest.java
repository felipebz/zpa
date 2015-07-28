package br.com.felipezorzo.sonar.plsql.checks;

import java.util.Locale;

import org.junit.Before;

public class BaseCheckTest {

    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }
    
}
