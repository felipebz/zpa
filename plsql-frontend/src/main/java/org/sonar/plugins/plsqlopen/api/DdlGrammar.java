/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

import static org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*;
import static org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

public enum DdlGrammar implements GrammarRuleKey {
    
    DDL_COMMENT,
    DDL_COMMAND,
    TABLE_COLUMN_DEFINITION,
    TABLE_RELATIONAL_PROPERTIES,
    CREATE_TABLE;
    
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
        
        b.rule(TABLE_COLUMN_DEFINITION).is(
                IDENTIFIER_NAME, DATATYPE,
                b.optional(SORT),
                b.optional(DEFAULT, EXPRESSION),
                b.optional(ENCRYPT));
        
        b.rule(TABLE_RELATIONAL_PROPERTIES).is(b.oneOrMore(TABLE_COLUMN_DEFINITION, b.optional(COMMA)));
        
        b.rule(CREATE_TABLE).is(
                CREATE, b.optional(GLOBAL, TEMPORARY), TABLE, UNIT_NAME,
                b.optional(LPARENTHESIS, TABLE_RELATIONAL_PROPERTIES, RPARENTHESIS),
                b.optional(ON, COMMIT, b.firstOf(DELETE, PRESERVE), ROWS),
                SEMICOLON);
        
        b.rule(DDL_COMMAND).is(DDL_COMMENT);
    }

}
