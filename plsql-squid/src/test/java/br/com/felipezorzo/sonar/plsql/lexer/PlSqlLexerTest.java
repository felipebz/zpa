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
}
