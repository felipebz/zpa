package br.com.felipezorzo.sonar.plsql.lexer;

import static com.sonar.sslr.test.lexer.LexerMatchers.hasComment;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasToken;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.api.PlSqlTokenType;

import com.google.common.base.Charsets;
import com.sonar.sslr.impl.Lexer;

public class PlSqlLexerTest {
    private static Lexer lexer;

    @BeforeClass
    public static void init() {
      lexer = PlSqlLexer.create(new PlSqlConfiguration(Charsets.UTF_8));
    }
    
    @Test
    public void multilineComment() {
      assertThat(lexer.lex("/* multine \n comment */"), hasComment("/* multine \n comment */"));
      assertThat(lexer.lex("/**/"), hasComment("/**/"));
    }

    @Test
    public void inlineComment() {
      assertThat(lexer.lex("before -- inline \n new line"), hasComment("-- inline "));
      assertThat(lexer.lex("--"), hasComment("--"));
    }
    
    @Test
    public void simpleStringLiteral() {
        assertThat(lexer.lex("'Test'"), hasToken("'Test'", PlSqlTokenType.STRING_LITERAL));
    }
    
    @Test
    public void stringLiteralWithDoubleQuotationMarks() {
        assertThat(lexer.lex("'I''m a string'"), hasToken("'I''m a string'", PlSqlTokenType.STRING_LITERAL));
    }
    
    @Test @Ignore
    public void stringLiteralWithUserDefinedDelimiters() {
        assertThat(lexer.lex("q'!I'm a string!'"), hasToken("q'!I'm a string!'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("q'[I'm a string]'"), hasToken("q'[I'm a string]'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("q'{I'm a string}'"), hasToken("q'{I'm a string}'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("q'<I'm a string>'"), hasToken("q'<I'm a string>'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("q'(I'm a string)'"), hasToken("q'(I'm a string)'", PlSqlTokenType.STRING_LITERAL));
        
        assertThat(lexer.lex("nq'!I'm a string!'"), hasToken("nq'!I'm a string!'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("nq'[I'm a string]'"), hasToken("nq'[I'm a string]'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("nq'{I'm a string}'"), hasToken("nq'{I'm a string}'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("nq'<I'm a string>'"), hasToken("nq'<I'm a string>'", PlSqlTokenType.STRING_LITERAL));
        assertThat(lexer.lex("nq'(I'm a string)'"), hasToken("nq'(I'm a string)'", PlSqlTokenType.STRING_LITERAL));
    }
    
    @Test
    public void simpleIntegerLiteral() {
        assertThat(lexer.lex("6"), hasToken("6", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void negativeIntegerLiteral() {
        assertThat(lexer.lex("-14"), hasToken("-14", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void positiveIntegerLiteral() {
        assertThat(lexer.lex("+32767"), hasToken("+32767", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void simpleRealLiteral() {
        assertThat(lexer.lex("3.14159"), hasToken("3.14159", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void negativeRealLiteral() {
        assertThat(lexer.lex("-12.0"), hasToken("-12.0", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void positiveRealLiteral() {
        assertThat(lexer.lex("+8300.00"), hasToken("+8300.00", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void realLiteralWithDecimalPointOnly() {
        assertThat(lexer.lex(".5"), hasToken(".5", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void realLiteralWithWholePartOnly() {
        assertThat(lexer.lex("25."), hasToken("25.", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void simpleScientificNotationLiteral() {
        assertThat(lexer.lex("2E5"), hasToken("2E5", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void scientificNotationLiteralWithNegativeExponent() {
        assertThat(lexer.lex("1.0E-7"), hasToken("1.0E-7", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void negativeScientificNotationLiteral() {
        assertThat(lexer.lex("-1E38"), hasToken("-1E38", PlSqlTokenType.NUMERIC_LITERAL));
    }
    
    @Test
    public void scientificNotationWithLowercaseSuffix() {
        assertThat(lexer.lex("9.5e-3"), hasToken("9.5e-3", PlSqlTokenType.NUMERIC_LITERAL));
    }
}
