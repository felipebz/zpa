/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.plsqlopen.api;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum DdlGrammar implements GrammarRuleKey {
    
    DDL_COMMENT,
    DDL_COMMAND,
    ONE_OR_MORE_IDENTIFIERS,
    REFERENCES_CLAUSE,
    INLINE_CONSTRAINT,
    OUT_OF_LINE_CONSTRAINT,
    TABLE_COLUMN_DEFINITION,
    TABLE_RELATIONAL_PROPERTIES,
    CREATE_TABLE,
    ALTER_TABLE,
    ALTER_PLSQL_UNIT,
    ALTER_PROCEDURE_FUNCTION,
    COMPILE_CLAUSE,
    ALTER_TRIGGER,
    ALTER_PACKAGE,
    DROP_COMMAND,
    CREATE_SYNONYM,
    CREATE_SEQUENCE;
    
    public static void buildOn(LexerfulGrammarBuilder b) {
        createDdlCommands(b);
    }
    
    private static void createDdlCommands(LexerfulGrammarBuilder b) {
        b.rule(DDL_COMMENT).is(
                COMMENT, ON,
                b.firstOf(
                        b.sequence(
                                COLUMN,
                                IDENTIFIER_NAME, 
                                b.optional(DOT, IDENTIFIER_NAME), 
                                b.optional(DOT, IDENTIFIER_NAME)),
                        b.sequence(
                                b.firstOf(
                                        TABLE,
                                        COLUMN,
                                        OPERATOR,
                                        INDEXTYPE,
                                        b.sequence(MATERIALIZED, VIEW),
                                        b.sequence(MINING, MODEL)),
                                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
                        ),
                IS, CHARACTER_LITERAL, b.optional(SEMICOLON));
        
        b.rule(ONE_OR_MORE_IDENTIFIERS).is(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS).skip();
        
        b.rule(REFERENCES_CLAUSE).is(
                REFERENCES, MEMBER_EXPRESSION,
                b.optional(ONE_OR_MORE_IDENTIFIERS),
                b.optional(ON, DELETE, b.firstOf(CASCADE, b.sequence(SET, NULL)))
                );
        
        b.rule(INLINE_CONSTRAINT).is(
                b.optional(CONSTRAINT, IDENTIFIER_NAME),
                b.firstOf(
                        b.sequence(b.optional(NOT), NULL),
                        UNIQUE,
                        b.sequence(PRIMARY, KEY),
                        REFERENCES_CLAUSE,
                        b.sequence(CHECK, EXPRESSION)));
        
        b.rule(TABLE_COLUMN_DEFINITION).is(
                IDENTIFIER_NAME, DATATYPE,
                b.optional(SORT),
                b.optional(DEFAULT, EXPRESSION),
                b.optional(ENCRYPT),
                b.zeroOrMore(INLINE_CONSTRAINT));
        
        b.rule(OUT_OF_LINE_CONSTRAINT).is(
                b.optional(CONSTRAINT, IDENTIFIER_NAME),
                b.firstOf(
                        b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                        b.sequence(PRIMARY, KEY, ONE_OR_MORE_IDENTIFIERS, b.optional( b.sequence(USING, INDEX))),
                        b.sequence(FOREIGN, KEY, ONE_OR_MORE_IDENTIFIERS, REFERENCES_CLAUSE),
                        b.sequence(CHECK, EXPRESSION)));
        
        b.rule(TABLE_RELATIONAL_PROPERTIES).is(b.oneOrMore(b.firstOf(OUT_OF_LINE_CONSTRAINT, TABLE_COLUMN_DEFINITION), b.optional(COMMA)));
        
        b.rule(CREATE_TABLE).is(
                CREATE, b.optional(GLOBAL, TEMPORARY), TABLE, UNIT_NAME,
                b.optional(LPARENTHESIS, TABLE_RELATIONAL_PROPERTIES, RPARENTHESIS),
                b.optional(ON, COMMIT, b.firstOf(DELETE, PRESERVE), ROWS),
                b.optional(SEMICOLON));
        
        b.rule(ALTER_TABLE).is(
        		ALTER, TABLE, UNIT_NAME, b.firstOf(ADD, DROP),  TABLE_RELATIONAL_PROPERTIES, b.optional(SEMICOLON));
        
        b.rule(COMPILE_CLAUSE).is(COMPILE, b.optional(DEBUG), b.optional(REUSE, SETTINGS));
        
        b.rule(ALTER_TRIGGER).is(
                TRIGGER, UNIT_NAME,
                b.firstOf(ENABLE, 
                          DISABLE, 
                          b.sequence(RENAME, TO, IDENTIFIER_NAME), 
                          COMPILE_CLAUSE)
                );
        
        b.rule(ALTER_PROCEDURE_FUNCTION).is(
                b.firstOf(PROCEDURE, FUNCTION)
                , UNIT_NAME, COMPILE_CLAUSE
                );
        
        b.rule(ALTER_PACKAGE).is(
                PACKAGE, UNIT_NAME, COMPILE, 
                b.optional(DEBUG), 
                b.optional(b.firstOf(PACKAGE, SPECIFICATION, BODY)),
                b.optional(REUSE, SETTINGS)
                );
        
        b.rule(ALTER_PLSQL_UNIT).is(ALTER, b.firstOf(ALTER_TRIGGER, ALTER_PROCEDURE_FUNCTION, ALTER_PACKAGE), b.optional(SEMICOLON));
        
        b.rule(DROP_COMMAND).is(DROP, b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))), b.optional(SEMICOLON));
        
        b.rule(CREATE_SYNONYM).is(
                CREATE, b.optional(OR, REPLACE),
                b.optional(PUBLIC), SYNONYM, UNIT_NAME,
                FOR, DmlGrammar.TABLE_REFERENCE, b.optional(SEMICOLON));
        
        b.rule(CREATE_SEQUENCE).is(
        		CREATE, SEQUENCE, UNIT_NAME, 
        		b.optional(START, WITH,NUMERIC_LITERAL),
        		b.optional(MAXVALUE, NUMERIC_LITERAL),
        		b.optional(MINVALUE, NUMERIC_LITERAL),
        		b.optional(INCREMENT, BY,NUMERIC_LITERAL),
        		b.optional(b.firstOf(CYCLE, NOCYCLE)),
        		b.optional(b.firstOf(NOCACHE, b.sequence(CACHE, NUMERIC_LITERAL)),
        		b.optional(b.firstOf(ORDER, NOORDER))),
        		b.optional(SEMICOLON));
        
        b.rule(DDL_COMMAND).is(b.firstOf(DDL_COMMENT, CREATE_TABLE, ALTER_TABLE, ALTER_PLSQL_UNIT, DROP_COMMAND, CREATE_SYNONYM, CREATE_SEQUENCE));
    }

}
