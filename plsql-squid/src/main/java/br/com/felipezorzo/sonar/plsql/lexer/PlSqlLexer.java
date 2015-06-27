package br.com.felipezorzo.sonar.plsql.lexer;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.and;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.commentRegexp;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.o2n;
import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;
import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;
import br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator;
import br.com.felipezorzo.sonar.plsql.api.PlSqlTokenType;

import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.BlackHoleChannel;
import com.sonar.sslr.impl.channel.IdentifierAndKeywordChannel;
import com.sonar.sslr.impl.channel.PunctuatorChannel;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;

public class PlSqlLexer {
    public static final String INLINE_COMMENT = "--[^\\n\\r]*+";
    public static final String MULTILINE_COMMENT = "/\\*[\\s\\S]*?\\*\\/";
    public static final String COMMENT = "(?:" + INLINE_COMMENT + "|" + MULTILINE_COMMENT + ")";
    
    // Literals reference: http://docs.oracle.com/cd/B19306_01/server.102/b14200/sql_elements003.htm
    // Unlike some other languages (like Python), in PL/SQL the sign of a number is a part of the literal.
    // So, -5 is a valid numeric literal, but --5 isn't.
    public static final String INTEGER_LITERAL = "(?:"
            + "(\\+|-)?\\d++"
            + ")";
    
    public static final String REAL_LITERAL = "(?:"
            + "(\\+|-)?("
            + "\\d*+\\.\\d++"
            + "|\\d++\\.\\d*+)"
            + ")";
    
    public static final String SCIENTIFIC_LITERAL = "(?:"
            + "(\\+|-)?"
            + "\\d*+(\\.\\d*+)?[Ee](\\+|-)?\\d++"
            + ")";
    
    public static final String STRING_LITERAL = "(?:'([^']|'')*+')";
    
    public static final String DATE_LITERAL = "(?:DATE '\\d{4}-\\d{2}-\\d{2}')";
    
    private PlSqlLexer() {
    }

    public static Lexer create(PlSqlConfiguration conf) {

        return Lexer
                .builder()
                .withCharset(conf.getCharset())
                .withFailIfNoChannelToConsumeOneCharacter(true)
                .withChannel(new BlackHoleChannel("\\s"))
                .withChannel(commentRegexp(COMMENT))
                .withChannel(regexp(PlSqlTokenType.SCIENTIFIC_LITERAL, SCIENTIFIC_LITERAL))
                .withChannel(regexp(PlSqlTokenType.REAL_LITERAL, REAL_LITERAL))
                .withChannel(regexp(PlSqlTokenType.INTEGER_LITERAL, INTEGER_LITERAL))
                .withChannel(regexp(PlSqlTokenType.STRING_LITERAL, STRING_LITERAL))
                .withChannel(regexp(PlSqlTokenType.DATE_LITERAL, DATE_LITERAL))
                .withChannel(new IdentifierAndKeywordChannel(and("[a-zA-Z_]", o2n("\\w")), false, PlSqlKeyword.values()))
                .withChannel(new PunctuatorChannel(PlSqlPunctuator.values()))
                .withChannel(new UnknownCharacterChannel())
                .build();
    }
}
