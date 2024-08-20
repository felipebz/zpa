/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package org.sonar.plugins.plsqlopen.api

import com.felipebz.flr.api.GenericTokenType.EOF
import com.felipebz.flr.grammar.GrammarRuleKey
import org.sonar.plsqlopen.sslr.PlSqlGrammarBuilder
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar.*
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword.*
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator.*
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType.INTEGER_LITERAL

enum class DdlGrammar : GrammarRuleKey {

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
    COMPILE_CLAUSE,
    COMPILER_PARAMETERS_CLAUSE,
    ALTER_PROCEDURE,
    ALTER_FUNCTION,
    ALTER_TRIGGER,
    ALTER_PACKAGE,
    PACKAGE_COMPILE_CLAUSE,
    DROP_COMMAND,
    CREATE_SYNONYM,
    CREATE_SEQUENCE,
    PARTITION_BY_RANGE,
    PARTITION_BY_HASH,
    RANGE_VALUES_CLAUSE,
    TABLE_PARTITION_DESCRIPTION,
    SEGMENT_ATTRIBUTES_CLAUSE,
    PHISICAL_ATRIBUTES_CLAUSE,
    TABLE_COMPRESSION,
    KEY_COMPRESSION,
    LOB_STORAGE_CLAUSE,
    VARRAY_COL_PROPERTIES,
    PARTITION_LEVEL_SUBPARTITION,
    //    HASH_SUBPARTITION_QUANTITY,
    SUBPARTITION_SPEC,
    LIST_VALUES_CLAUSE,
    PARTITIONING_STORAGE_CLAUSE,
    SUBSTITUTABLE_COLUMN_CLAUSE,
    LOB_PARAMETERS,
    STORAGE_CLAUSE,
    LOGGING_CLAUSE,
    SIZE_CLAUSE,
    INDIVIDUAL_HASH_PARTITIONS,
    HASH_PARTITIONS_BY_QUANTITY,
    PARTITION_BY_LIST,
    PARTITION_COMPOSITE,
    SUBPARTITION_BY_LIST,
    SUBPARTITION_BY_HASH,
    SUBPARTITION_TEMPLATE,
    CREATE_DIRECTORY,
    DROP_DIRECTORY,
    TRUNCATE_TABLE;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createDdlCommands(b)
        }

        private fun createDdlCommands(b: PlSqlGrammarBuilder) {
            b.rule(DDL_COMMENT).define(
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
                    IS, CHARACTER_LITERAL, b.optional(SEMICOLON))

            b.rule(ONE_OR_MORE_IDENTIFIERS).define(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS).skip()

            b.rule(REFERENCES_CLAUSE).define(
                    REFERENCES, MEMBER_EXPRESSION,
                    b.optional(ONE_OR_MORE_IDENTIFIERS),
                    b.optional(ON, DELETE, b.firstOf(CASCADE, b.sequence(SET, NULL)))
            )

            b.rule(INLINE_CONSTRAINT).define(
                    b.optional(CONSTRAINT, IDENTIFIER_NAME),
                    b.firstOf(
                            b.sequence(b.optional(NOT), NULL),
                            UNIQUE,
                            b.sequence(PRIMARY, KEY),
                            REFERENCES_CLAUSE,
                            b.sequence(CHECK, EXPRESSION)))

            b.rule(TABLE_COLUMN_DEFINITION).define(
                    IDENTIFIER_NAME, DATATYPE,
                    b.optional(SORT),
                    b.optional(DEFAULT, EXPRESSION),
                    b.optional(ENCRYPT),
                    b.zeroOrMore(INLINE_CONSTRAINT))

            b.rule(OUT_OF_LINE_CONSTRAINT).define(
                    b.optional(CONSTRAINT, IDENTIFIER_NAME),
                    b.firstOf(
                            b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                            b.sequence(PRIMARY, KEY, ONE_OR_MORE_IDENTIFIERS, b.optional(b.sequence(USING, INDEX))),
                            b.sequence(FOREIGN, KEY, ONE_OR_MORE_IDENTIFIERS, REFERENCES_CLAUSE),
                            b.sequence(CHECK, EXPRESSION)))

            b.rule(TABLE_RELATIONAL_PROPERTIES).define(
                    b.oneOrMore(b.firstOf(OUT_OF_LINE_CONSTRAINT, TABLE_COLUMN_DEFINITION), b.optional(COMMA)))

            b.rule(PHISICAL_ATRIBUTES_CLAUSE).define(
                    b.oneOrMore(b.firstOf(
                            b.sequence(PCTFREE, INTEGER_LITERAL),
                            b.sequence(PCTUSED, INTEGER_LITERAL),
                            b.sequence(INITRANS, INTEGER_LITERAL),
                            STORAGE_CLAUSE)))

            b.rule(SEGMENT_ATTRIBUTES_CLAUSE).define(
                    b.oneOrMore(b.firstOf(
                            PHISICAL_ATRIBUTES_CLAUSE,
                            b.sequence(TABLESPACE, IDENTIFIER_NAME),
                            LOGGING_CLAUSE)))

            b.rule(TABLE_COMPRESSION).define(
                    b.firstOf(COMPRESS, NOCOMPRESS))

            b.rule(KEY_COMPRESSION).define(
                    b.firstOf(
                            b.sequence(MAPPING, TABLE),
                            NOMAPPING))

            b.rule(LOB_STORAGE_CLAUSE).define(
                    b.sequence(LOB,
                            b.firstOf(
                                    b.sequence(
                                            LPARENTHESIS,
                                            b.oneOrMore(
                                                    IDENTIFIER_NAME,
                                                    b.optional(COMMA)),
                                            RPARENTHESIS,
                                            STORE,
                                            AS,
                                            LPARENTHESIS,
                                            LOB_PARAMETERS,
                                            RPARENTHESIS),
                                    b.sequence(
                                            LPARENTHESIS,
                                            IDENTIFIER_NAME,
                                            RPARENTHESIS,
                                            STORE,
                                            AS,
                                            IDENTIFIER_NAME,
                                            b.optional(b.sequence(
                                                    LPARENTHESIS,
                                                    LOB_PARAMETERS,
                                                    RPARENTHESIS))))))

            b.rule(SUBSTITUTABLE_COLUMN_CLAUSE).define(
                    b.firstOf(
                            b.sequence(
                                    b.optional(ELEMENT),
                                    IS,
                                    OF,
                                    b.optional(TYPE),
                                    LPARENTHESIS,
                                    ONLY,
                                    DATATYPE,
                                    RPARENTHESIS),
                            b.sequence(
                                    b.optional(NOT),
                                    SUBSTITUTABLE,
                                    AT,
                                    ALL,
                                    LEVELS)))

            b.rule(SIZE_CLAUSE).define(
                    b.sequence(INTEGER_LITERAL, b.firstOf("K", "M", "G", "T", "P", "E")))

            b.rule(STORAGE_CLAUSE).define(
                    b.sequence(STORAGE,
                            LPARENTHESIS,
                            b.firstOf(
                                    b.sequence(INITIAL, SIZE_CLAUSE),
                                    b.sequence(NEXT, SIZE_CLAUSE),
                                    b.sequence(MINEXTENTS, INTEGER_LITERAL),
                                    b.sequence(MAXEXTENTS, b.firstOf(INTEGER_LITERAL, UNLIMITED)),
                                    b.sequence(PCTINCREASE, INTEGER_LITERAL),
                                    b.sequence(FREELISTS, INTEGER_LITERAL),
                                    b.sequence(FREELIST, GROUPS, INTEGER_LITERAL),
                                    b.sequence(OPTIMAL, b.optional(b.firstOf(SIZE_CLAUSE, NULL))),
                                    b.sequence(BUFFER_POOL, b.firstOf(KEEP, RECYCLE, DEFAULT))),
                            RPARENTHESIS))

            b.rule(LOGGING_CLAUSE).define(
                    b.firstOf(LOGGING, NOLOGGING))

            b.rule(LOB_PARAMETERS).define(
                    b.oneOrMore(b.firstOf(
                            b.sequence(
                                    TABLESPACE,
                                    IDENTIFIER_NAME),
                            b.sequence(
                                    b.firstOf(
                                            ENABLE,
                                            DISABLE),
                                    STORAGE,
                                    IN,
                                    NOW),
                            STORAGE_CLAUSE,
                            b.sequence(
                                    CHUNK,
                                    INTEGER_LITERAL),
                            b.sequence(
                                    PCTVERSION,
                                    INTEGER_LITERAL),
                            RETENTION,
                            b.sequence(
                                    FREEPOOLS,
                                    INTEGER_LITERAL),
                            b.firstOf(
                                    b.sequence(CACHE,
                                            b.optional(b.sequence(
                                                    READS,
                                                    b.optional(LOGGING_CLAUSE)))),
                                    b.sequence(
                                            NOCACHE,
                                            b.optional(LOGGING_CLAUSE))))))

            b.rule(VARRAY_COL_PROPERTIES).define(
                    b.sequence(VARRAY,
                            IDENTIFIER_NAME,
                            b.firstOf(b.sequence(
                                    b.optional(SUBSTITUTABLE_COLUMN_CLAUSE),
                                    STORE,
                                    AS,
                                    LOB,
                                    b.firstOf(b.sequence(
                                            b.optional(IDENTIFIER_NAME),
                                            LPARENTHESIS,
                                            LOB_PARAMETERS,
                                            RPARENTHESIS),
                                            IDENTIFIER_NAME)),
                                    SUBSTITUTABLE_COLUMN_CLAUSE)))

            b.rule(LIST_VALUES_CLAUSE).define(
                    b.sequence(
                            VALUES,
                            LPARENTHESIS,
                            b.firstOf(
                                    b.oneOrMore(
                                            b.firstOf(
                                                    LITERAL,
                                                    NULL),
                                            b.optional(COMMA)),
                                    DEFAULT),
                            RPARENTHESIS))

            b.rule(PARTITIONING_STORAGE_CLAUSE).define(
                    b.optional(
                            b.oneOrMore(
                                    b.firstOf(
                                            b.sequence(
                                                    TABLESPACE,
                                                    IDENTIFIER_NAME),
                                            b.sequence(
                                                    OVERFLOW,
                                                    b.optional(
                                                            b.sequence(
                                                                    TABLESPACE,
                                                                    IDENTIFIER_NAME))),
                                            //				        				b.sequence(
                                            //				        						LOB,
                                            //				        						LPARENTHESIS,
                                            //				        						IDENTIFIER_NAME,
                                            //				        						RPARENTHESIS,
                                            //				        						STORE,
                                            //				        						AS,
                                            //				        						b.firstOf(
                                            //				        								b.sequence(
                                            //				        										IDENTIFIER_NAME,
                                            //				        										b.optional(
                                            //				        												b.sequence(
                                            //						        												LPARENTHESIS,
                                            //						        												TABLESPACE,
                                            //						        												IDENTIFIER_NAME,
                                            //						        												RPARENTHESIS))),
                                            //						        						b.sequence(
                                            //						        								LPARENTHESIS,
                                            //						        								TABLESPACE,
                                            //						        								IDENTIFIER_NAME,
                                            //						        								RPARENTHESIS))),
                                            LOB_STORAGE_CLAUSE,
                                            //				        				b.sequence(
                                            //				        						VARRAY,
                                            //				        						IDENTIFIER_NAME,
                                            //				        						STORE,
                                            //				        						AS,
                                            //				        						LOB,
                                            //				        						IDENTIFIER_NAME)))));
                                            VARRAY_COL_PROPERTIES))))

            b.rule(SUBPARTITION_SPEC).define(
                    b.sequence(SUBPARTITION, b.optional(IDENTIFIER_NAME), b.optional(LIST_VALUES_CLAUSE), b.optional(PARTITIONING_STORAGE_CLAUSE)))

            b.rule(PARTITION_LEVEL_SUBPARTITION).define(
                    b.firstOf(
                            b.sequence(
                                    SUBPARTITIONS,
                                    INTEGER_LITERAL,
                                    b.optional(
                                            b.sequence(
                                                    STORE,
                                                    IN,
                                                    b.sequence(
                                                            LPARENTHESIS,
                                                            b.oneOrMore(
                                                                    IDENTIFIER_NAME,
                                                                    b.optional(COMMA)),
                                                            RPARENTHESIS)))),
                            b.sequence(
                                    LPARENTHESIS,
                                    b.oneOrMore(
                                            SUBPARTITION_SPEC,
                                            b.optional(COMMA)),
                                    RPARENTHESIS)))

            b.rule(RANGE_VALUES_CLAUSE).define(
                    b.sequence(VALUES, LESS, THAN,
                            b.sequence(
                                    LPARENTHESIS,
                                    b.oneOrMore(
                                            b.firstOf(
                                                    METHOD_CALL,
                                                    IDENTIFIER_NAME,
                                                    MAXVALUE),
                                            b.optional(COMMA)),
                                    RPARENTHESIS)))

            b.rule(TABLE_PARTITION_DESCRIPTION).define(
                    b.sequence(
                            b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                            b.optional(
                                    b.firstOf(
                                            TABLE_COMPRESSION,
                                            KEY_COMPRESSION)),
                            b.optional(
                                    b.sequence(
                                            OVERFLOW,
                                            b.optional(SEGMENT_ATTRIBUTES_CLAUSE))),
                            b.optional(
                                    b.oneOrMore(
                                            b.firstOf(
                                                    LOB_STORAGE_CLAUSE,
                                                    VARRAY_COL_PROPERTIES))),
                            b.optional(PARTITION_LEVEL_SUBPARTITION)))

            b.rule(INDIVIDUAL_HASH_PARTITIONS).define(
                    b.sequence(
                            LPARENTHESIS,
                            b.oneOrMore(
                                    b.sequence(
                                            PARTITION,
                                            b.optional(
                                                    b.sequence(
                                                            IDENTIFIER_NAME,
                                                            PARTITIONING_STORAGE_CLAUSE)),
                                            b.optional(COMMA))),
                            RPARENTHESIS))

            b.rule(HASH_PARTITIONS_BY_QUANTITY).define(
                    b.sequence(
                            PARTITIONS,
                            INTEGER_LITERAL,
                            b.optional(
                                    b.sequence(
                                            STORE,
                                            IN,
                                            LPARENTHESIS,
                                            b.oneOrMore(b.sequence(
                                                    IDENTIFIER_NAME,
                                                    b.optional(COMMA))),
                                            RPARENTHESIS)),
                            b.optional(
                                    b.sequence(
                                            OVERFLOW,
                                            STORE,
                                            IN,
                                            LPARENTHESIS,
                                            b.oneOrMore(b.sequence(
                                                    IDENTIFIER_NAME,
                                                    b.optional(COMMA))),
                                            RPARENTHESIS))))

            b.rule(SUBPARTITION_TEMPLATE).define(
                    b.sequence(
                            SUBPARTITION,
                            TEMPLATE,
                            b.firstOf(
                                    b.sequence(
                                            LPARENTHESIS,
                                            b.oneOrMore(
                                                    b.sequence(
                                                            SUBPARTITION,
                                                            IDENTIFIER_NAME,
                                                            b.optional(LIST_VALUES_CLAUSE),
                                                            b.optional(PARTITIONING_STORAGE_CLAUSE),
                                                            b.optional(COMMA))),
                                            RPARENTHESIS),
                                    INTEGER_LITERAL)))

            b.rule(SUBPARTITION_BY_LIST).define(
                    b.sequence(SUBPARTITION, BY, LIST, LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS, b.optional(SUBPARTITION_TEMPLATE)))

            b.rule(SUBPARTITION_BY_HASH).define(
                    b.sequence(
                            SUBPARTITION,
                            BY,
                            HASH,
                            LPARENTHESIS,
                            b.oneOrMore(
                                    IDENTIFIER_NAME,
                                    b.optional(COMMA)),
                            RPARENTHESIS,
                            b.optional(
                                    b.firstOf(
                                            b.sequence(
                                                    SUBPARTITIONS,
                                                    INTEGER_LITERAL,
                                                    b.optional(
                                                            b.sequence(
                                                                    STORE,
                                                                    IN,
                                                                    LPARENTHESIS,
                                                                    b.oneOrMore(
                                                                            IDENTIFIER_NAME,
                                                                            b.optional(COMMA)),
                                                                    RPARENTHESIS))),
                                            SUBPARTITION_TEMPLATE))))

            b.rule(PARTITION_BY_RANGE).define(
                    b.sequence(
                            PARTITION,
                            BY,
                            RANGE_KEYWORD,
                            LPARENTHESIS,
                            b.oneOrMore(
                                    IDENTIFIER_NAME,
                                    b.optional(COMMA)),
                            RPARENTHESIS,
                            LPARENTHESIS,
                            b.oneOrMore(
                                    PARTITION,
                                    b.optional(IDENTIFIER_NAME),
                                    RANGE_VALUES_CLAUSE,
                                    TABLE_PARTITION_DESCRIPTION,
                                    b.optional(COMMA)),
                            RPARENTHESIS))

            b.rule(PARTITION_BY_HASH).define(
                    b.sequence(
                            PARTITION,
                            BY,
                            HASH,
                            LPARENTHESIS,
                            b.oneOrMore(
                                    b.sequence(
                                            IDENTIFIER_NAME,
                                            b.optional(COMMA))),
                            RPARENTHESIS,
                            b.firstOf(
                                    INDIVIDUAL_HASH_PARTITIONS,
                                    HASH_PARTITIONS_BY_QUANTITY)))

            b.rule(PARTITION_BY_LIST).define(
                    b.sequence(
                            PARTITION,
                            BY,
                            LIST,
                            LPARENTHESIS,
                            IDENTIFIER_NAME,
                            RPARENTHESIS,
                            LPARENTHESIS,
                            b.oneOrMore(
                                    b.sequence(
                                            PARTITION,
                                            b.optional(IDENTIFIER_NAME),
                                            LIST_VALUES_CLAUSE,
                                            TABLE_PARTITION_DESCRIPTION,
                                            b.optional(COMMA))),
                            RPARENTHESIS))

            b.rule(PARTITION_COMPOSITE).define(
                    b.sequence(
                            PARTITION,
                            BY,
                            RANGE_KEYWORD,
                            b.oneOrMore(
                                    LPARENTHESIS,
                                    IDENTIFIER_NAME,
                                    b.optional(COMMA),
                                    RPARENTHESIS),
                            b.firstOf(
                                    SUBPARTITION_BY_LIST,
                                    SUBPARTITION_BY_HASH),
                            LPARENTHESIS,
                            b.oneOrMore(
                                    b.sequence(
                                            PARTITION,
                                            b.optional(IDENTIFIER_NAME),
                                            RANGE_VALUES_CLAUSE,
                                            TABLE_PARTITION_DESCRIPTION,
                                            b.optional(COMMA))),
                            RPARENTHESIS))

            b.rule(CREATE_TABLE).define(
                    CREATE,
                    b.optional(
                            GLOBAL,
                            TEMPORARY),
                    TABLE,
                    UNIT_NAME,
                    b.optional(
                            LPARENTHESIS,
                            TABLE_RELATIONAL_PROPERTIES,
                            RPARENTHESIS),
                    b.optional(b.firstOf(
                            PARTITION_BY_RANGE,
                            PARTITION_BY_HASH,
                            PARTITION_BY_LIST,
                            PARTITION_COMPOSITE)),
                    b.optional(
                            TABLESPACE,
                            IDENTIFIER_NAME),
                    b.optional(
                            ON,
                            COMMIT,
                            b.firstOf(
                                    DELETE,
                                    PRESERVE),
                            ROWS),
                    b.optional(SEMICOLON))

            b.rule(ALTER_TABLE).define(
                    ALTER, TABLE, UNIT_NAME, b.firstOf(ADD, DROP), TABLE_RELATIONAL_PROPERTIES, b.optional(SEMICOLON))

            b.rule(COMPILE_CLAUSE).define(
                COMPILE, b.optional(DEBUG),
                b.zeroOrMore(COMPILER_PARAMETERS_CLAUSE),
                b.optional(REUSE, SETTINGS))

            b.rule(COMPILER_PARAMETERS_CLAUSE).define(
                    IDENTIFIER_NAME, EQUALS_OPERATOR, CHARACTER_LITERAL)

            b.rule(ALTER_TRIGGER).define(
                    ALTER, TRIGGER, b.optional(IF, EXISTS), UNIT_NAME,
                    b.firstOf(
                            ENABLE,
                            DISABLE,
                            b.sequence(RENAME, TO, IDENTIFIER_NAME),
                            EDITIONABLE,
                            NONEDITIONABLE,
                            COMPILE_CLAUSE),
                    b.optional(SEMICOLON))

            b.rule(ALTER_PROCEDURE).define(
                ALTER, PROCEDURE, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    EDITIONABLE,
                    NONEDITIONABLE,
                    COMPILE_CLAUSE),
                b.optional(SEMICOLON))

            b.rule(ALTER_FUNCTION).define(
                ALTER, FUNCTION, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    EDITIONABLE,
                    NONEDITIONABLE,
                    COMPILE_CLAUSE),
                b.optional(SEMICOLON))

            b.rule(ALTER_PACKAGE).define(
                    ALTER, PACKAGE, b.optional(IF, EXISTS), UNIT_NAME,
                    b.firstOf(
                        EDITIONABLE,
                        NONEDITIONABLE,
                        PACKAGE_COMPILE_CLAUSE),
                    b.optional(SEMICOLON))

            b.rule(PACKAGE_COMPILE_CLAUSE).define(
                    COMPILE, b.optional(DEBUG),
                    b.optional(b.firstOf(PACKAGE, SPECIFICATION, BODY)),
                    b.zeroOrMore(COMPILER_PARAMETERS_CLAUSE),
                    b.optional(REUSE, SETTINGS))

            b.rule(DROP_COMMAND).define(DROP, b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))), b.optional(SEMICOLON))

            b.rule(CREATE_SYNONYM).define(
                    CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE)),
                    b.optional(PUBLIC), SYNONYM, UNIT_NAME,
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    FOR, DmlGrammar.TABLE_REFERENCE, b.optional(SEMICOLON))

            b.rule(CREATE_SEQUENCE).define(
                    CREATE, SEQUENCE, UNIT_NAME,
                    b.optional(START, WITH, NUMERIC_LITERAL),
                    b.optional(MAXVALUE, NUMERIC_LITERAL),
                    b.optional(MINVALUE, NUMERIC_LITERAL),
                    b.optional(INCREMENT, BY, NUMERIC_LITERAL),
                    b.optional(b.firstOf(CYCLE, NOCYCLE)),
                    b.optional(b.firstOf(NOCACHE, b.sequence(CACHE, NUMERIC_LITERAL)),
                            b.optional(b.firstOf(ORDER, NOORDER))),
                    b.optional(SEMICOLON))

            b.rule(CREATE_DIRECTORY).define(
                    CREATE, b.optional(OR, REPLACE), DIRECTORY,
                    b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME,
                    b.optional(SHARING, EQUALS_OPERATOR, b.firstOf(METADATA, NONE)),
                    AS, CHARACTER_LITERAL,
                    b.optional(SEMICOLON))

            b.rule(DROP_DIRECTORY).define(
                    DROP, DIRECTORY, b.optional(IF, EXISTS), IDENTIFIER_NAME, b.optional(SEMICOLON))

            b.rule(TRUNCATE_TABLE).define(
                TRUNCATE, TABLE, UNIT_NAME,
                b.optional(
                    b.firstOf(
                        PRESERVE, PURGE
                    ),
                    MATERIALIZED, VIEW, LOG
                ),
                b.optional(
                    b.firstOf(
                        b.sequence(
                            DROP, b.optional(ALL)
                        ),
                        REUSE
                    ),
                    STORAGE
                ),
                b.optional(CASCADE),
                b.optional(SEMICOLON)
            )

            b.rule(DDL_COMMAND).define(b.firstOf(
                DDL_COMMENT,
                CREATE_TABLE,
                ALTER_TABLE,
                ALTER_TRIGGER,
                ALTER_PROCEDURE,
                ALTER_FUNCTION,
                ALTER_PACKAGE,
                CREATE_SYNONYM,
                CREATE_SEQUENCE,
                CREATE_DIRECTORY,
                DROP_DIRECTORY,
                DROP_COMMAND,
                TRUNCATE_TABLE))
        }
    }

}
