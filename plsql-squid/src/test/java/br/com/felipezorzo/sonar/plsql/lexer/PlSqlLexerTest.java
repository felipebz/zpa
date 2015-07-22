package br.com.felipezorzo.sonar.plsql.lexer;

import static com.sonar.sslr.test.lexer.LexerMatchers.hasComment;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasToken;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.not;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.api.PlSqlTokenType;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.TokenType;
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
        assertThatIsToken("'Test'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void stringLiteralWithDoubleQuotationMarks() {
        assertThatIsToken("'I''m a string'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test @Ignore
    public void stringLiteralWithUserDefinedDelimiters() {
        assertThatIsToken("q'!I'm a string!'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'[I'm a string]'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'{I'm a string}'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'<I'm a string>'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("q'(I'm a string)'", PlSqlTokenType.STRING_LITERAL);
        
        assertThatIsToken("nq'!I'm a string!'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'[I'm a string]'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'{I'm a string}'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'<I'm a string>'", PlSqlTokenType.STRING_LITERAL);
        assertThatIsToken("nq'(I'm a string)'", PlSqlTokenType.STRING_LITERAL);
    }
    
    @Test
    public void simpleIntegerLiteral() {
        assertThatIsToken("6", PlSqlTokenType.INTEGER_LITERAL);
    }
    
    @Test
    public void simpleRealLiteral() {
        assertThatIsToken("3.14159", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void realLiteralWithDecimalPointOnly() {
        assertThatIsToken(".5", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void realLiteralWithWholePartOnly() {
        assertThatIsToken("25.", PlSqlTokenType.REAL_LITERAL);
    }
    
    @Test
    public void simpleScientificNotationLiteral() {
        assertThatIsToken("2E5", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void scientificNotationLiteralWithNegativeExponent() {
        assertThatIsToken("1.0E-7", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void scientificNotationWithLowercaseSuffix() {
        assertThatIsToken("9.5e-3", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void cornerCases() {
        assertThatIsNotToken("1..", PlSqlTokenType.REAL_LITERAL);
        assertThatIsNotToken("..2", PlSqlTokenType.REAL_LITERAL);
        
        assertThatIsNotToken("e1", PlSqlTokenType.SCIENTIFIC_LITERAL);
    }
    
    @Test
    public void dateLiteral() {
        assertThatIsToken("DATE '2015-01-01'", PlSqlTokenType.DATE_LITERAL);
    }
    
    private void assertThatIsToken(String sourceCode, TokenType tokenType) {
        assertThat(lexer.lex(sourceCode), hasToken(sourceCode, tokenType));
    }
    
    private void assertThatIsNotToken(String sourceCode, TokenType tokenType) {
        assertThat(lexer.lex(sourceCode), not(hasToken(sourceCode, tokenType)));
    }
}
