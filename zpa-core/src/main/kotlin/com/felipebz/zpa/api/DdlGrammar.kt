/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api

import com.felipebz.flr.api.GenericTokenType.EOF
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder
import com.felipebz.zpa.api.PlSqlGrammar.*
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*
import com.felipebz.zpa.api.PlSqlTokenType.INTEGER_LITERAL

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
    CREATE_INDEX,
    CREATE_INDEX_SCHEMA_OBJECT_NAME,
    CREATE_INDEX_ON_CLAUSE,
    CREATE_INDEX_CLUSTER_CLAUSE,
    CREATE_INDEX_TABLE_CLAUSE,
    CREATE_INDEX_BITMAP_JOIN_CLAUSE,
    CREATE_INDEX_EXPR,
    CREATE_INDEX_ATTRIBUTES,
    CREATE_INDEX_ATTRIBUTE,
    CREATE_INDEX_PROPERTIES,
    CREATE_INDEX_PARTITIONING_CLAUSE,
    CREATE_INDEX_PARTITION_STORAGE_CLAUSE,
    CREATE_INDEX_GLOBAL_PARTITIONED,
    CREATE_INDEX_HASH_PARTITIONS_BY_QUANTITY,
    CREATE_INDEX_HASH_PARTITIONS,
    CREATE_INDEX_LOCAL_PARTITIONED,
    CREATE_INDEX_LOCAL_RANGE_PARTITIONS,
    CREATE_INDEX_LOCAL_HASH_PARTITIONS,
    CREATE_INDEX_LOCAL_COMPOSITE_PARTITIONS,
    CREATE_INDEX_SUBPARTITION_CLAUSE,
    CREATE_INDEX_SUBPARTITION,
    CREATE_INDEX_DOMAIN_CLAUSE,
    CREATE_INDEX_LOCAL_DOMAIN_CLAUSE,
    CREATE_INDEX_XMLINDEX_CLAUSE,
    CREATE_INDEX_LOCAL_XMLINDEX_CLAUSE,
    ALTER_TABLE,
    ALTER_INDEX,
    ALTER_INDEX_ACTION,
    INDEX_SIZE_CLAUSE,
    INDEX_PARAMETERS_CLAUSE,
    INDEX_STORAGE_CLAUSE,
    INDEX_PHYSICAL_ATTRIBUTES_CLAUSE,
    INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
    INDEX_PARALLEL_CLAUSE,
    INDEX_COMPRESSION_CLAUSE,
    INDEX_PARTIAL_CLAUSE,
    INDEX_DEALLOCATE_UNUSED_CLAUSE,
    INDEX_ALLOCATE_EXTENT_CLAUSE,
    INDEX_SHRINK_CLAUSE,
    INDEX_REBUILD_CLAUSE,
    INDEX_SEGMENT_ATTRIBUTES_CLAUSE,
    INDEX_PARTITION_DESCRIPTION,
    INDEX_ANNOTATIONS_CLAUSE,
    INDEX_ANNOTATION,
    INDEX_ILM_CLAUSE,
    INDEX_ILM_ACTION,
    INDEX_ILM_POLICY_CLAUSE,
    INDEX_ILM_CONDITION_CLAUSE,
    INDEX_TRACKING_STATISTICS_CLAUSE,
    ALTER_INDEX_PARTITIONING,
    MODIFY_INDEX_DEFAULT_ATTRS,
    ADD_HASH_INDEX_PARTITION,
    MODIFY_INDEX_PARTITION,
    RENAME_INDEX_PARTITION,
    DROP_INDEX_PARTITION,
    SPLIT_INDEX_PARTITION,
    COALESCE_INDEX_PARTITION,
    MODIFY_INDEX_SUBPARTITION,
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
    TRUNCATE_TABLE,
    CONSTRAINT_STATE,
    PRECHECK_STATE,
    EXCEPTIONS_CLAUSE;

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
                b.optional(b.firstOf(CONSTRAINT, CONSTRAINTS), IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(b.optional(NOT), NULL),
                            UNIQUE,
                            b.sequence(PRIMARY, KEY),
                            REFERENCES_CLAUSE
                        ), b.optional(CONSTRAINT_STATE)
                    ),
                    b.sequence(
                        CHECK, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                        b.optional(CONSTRAINT_STATE),
                        b.optional(PRECHECK_STATE)
                    )
                )
            )

            b.rule(CONSTRAINT_STATE).define(
                b.optional(b.firstOf(
                    b.sequence(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE), b.optional(b.optional(NOT), DEFERRABLE)),
                    b.sequence(b.optional(NOT), DEFERRABLE, b.optional(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE))),
                )),
                b.optional(b.firstOf(RELY, NORELY)),
                // TODO b.optional(USING_INDEX_CLAUSE),
                b.optional(b.firstOf(ENABLE, DISABLE)),
                b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                b.optional(EXCEPTIONS_CLAUSE)
            )

            b.rule(PRECHECK_STATE).define(b.firstOf(PRECHECK, NOPRECHECK))

            b.rule(EXCEPTIONS_CLAUSE).define(EXCEPTIONS, INTO, UNIT_NAME)

            b.rule(TABLE_COLUMN_DEFINITION).define(
                    IDENTIFIER_NAME, DATATYPE,
                    
                    b.optional(SORT),
                    b.optional(DEFAULT, b.optional(
                        b.sequence(ON, NULL,
                            b.optional(FOR, INSERT,
                                b.firstOf(ONLY, b.sequence(AND, UPDATE))))), EXPRESSION),
                    b.optional(ENCRYPT),
                    b.zeroOrMore(INLINE_CONSTRAINT))

            b.rule(OUT_OF_LINE_CONSTRAINT).define(
                b.optional(b.firstOf(CONSTRAINT, CONSTRAINTS), IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                            b.sequence(PRIMARY, KEY, ONE_OR_MORE_IDENTIFIERS, b.optional(b.sequence(USING, INDEX))),
                            b.sequence(FOREIGN, KEY, ONE_OR_MORE_IDENTIFIERS, REFERENCES_CLAUSE)
                        ), b.optional(CONSTRAINT_STATE)
                    ),
                    b.sequence(
                        CHECK, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                        b.optional(CONSTRAINT_STATE),
                        b.optional(PRECHECK_STATE)
                    )
                )
            )

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

            // XMLIndex parameter syntax is carried inside the same quoted parameter string.
            b.rule(INDEX_PARAMETERS_CLAUSE).define(
                PARAMETERS, LPARENTHESIS, CHARACTER_LITERAL, RPARENTHESIS)

            b.rule(INDEX_SIZE_CLAUSE).define(
                INTEGER_LITERAL, b.optional(b.firstOf("K", "M", "G", "T", "P", "E")))

            b.rule(INDEX_STORAGE_CLAUSE).define(
                STORAGE,
                LPARENTHESIS,
                b.oneOrMore(b.firstOf(
                    b.sequence(INITIAL, INDEX_SIZE_CLAUSE),
                    b.sequence(NEXT, INDEX_SIZE_CLAUSE),
                    b.sequence(MINEXTENTS, INTEGER_LITERAL),
                    b.sequence(MAXEXTENTS, b.firstOf(INTEGER_LITERAL, UNLIMITED)),
                    b.sequence(PCTINCREASE, INTEGER_LITERAL),
                    b.sequence(FREELISTS, INTEGER_LITERAL),
                    b.sequence(FREELIST, GROUPS, INTEGER_LITERAL),
                    b.sequence(OPTIMAL, b.optional(b.firstOf(INDEX_SIZE_CLAUSE, NULL))),
                    b.sequence(MAXSIZE, b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE)),
                    b.sequence(BUFFER_POOL, b.firstOf(KEEP, RECYCLE, DEFAULT)),
                    b.sequence(FLASH_CACHE, b.firstOf(KEEP, NONE, DEFAULT)),
                    b.sequence(
                        LPARENTHESIS,
                        CELL_FLASH_CACHE,
                        LPARENTHESIS,
                        b.firstOf(KEEP, NONE, DEFAULT),
                        RPARENTHESIS,
                        RPARENTHESIS))),
                RPARENTHESIS)

            b.rule(INDEX_PHYSICAL_ATTRIBUTES_CLAUSE).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(INITRANS, INTEGER_LITERAL),
                    INDEX_STORAGE_CLAUSE)))

            b.rule(INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(PCTFREE, INTEGER_LITERAL),
                    b.sequence(INITRANS, INTEGER_LITERAL),
                    INDEX_STORAGE_CLAUSE)))

            b.rule(INDEX_PARALLEL_CLAUSE).define(
                b.firstOf(
                    NOPARALLEL,
                    b.sequence(PARALLEL, b.optional(INTEGER_LITERAL))))

            b.rule(INDEX_COMPRESSION_CLAUSE).define(
                b.firstOf(
                    b.sequence(COMPRESS, b.optional(b.firstOf(
                        INTEGER_LITERAL,
                        b.sequence(ADVANCED, b.optional(b.firstOf(LOW, HIGH)))))),
                    NOCOMPRESS))

            b.rule(INDEX_PARTIAL_CLAUSE).define(
                INDEXING, b.firstOf(FULL, PARTIAL))

            b.rule(INDEX_DEALLOCATE_UNUSED_CLAUSE).define(
                DEALLOCATE, UNUSED, b.optional(KEEP, INDEX_SIZE_CLAUSE))

            b.rule(INDEX_ALLOCATE_EXTENT_CLAUSE).define(
                ALLOCATE, EXTENT,
                b.optional(
                    LPARENTHESIS,
                    b.oneOrMore(b.firstOf(
                        b.sequence(SIZE, INDEX_SIZE_CLAUSE),
                        b.sequence(DATAFILE, CHARACTER_LITERAL),
                        b.sequence(INSTANCE, INTEGER_LITERAL))),
                    RPARENTHESIS))

            b.rule(INDEX_SHRINK_CLAUSE).define(
                SHRINK, SPACE, b.optional(COMPACT), b.optional(CASCADE))

            b.rule(INDEX_TRACKING_STATISTICS_CLAUSE).define(
                AFTER, INTEGER_LITERAL,
                b.firstOf(DAY, DAYS, MONTH, MONTHS, YEAR, YEARS),
                OF, b.optional(NO), b.firstOf(ACCESS, MODIFICATION, CREATION))

            b.rule(INDEX_ILM_CONDITION_CLAUSE).define(
                b.firstOf(
                    INDEX_TRACKING_STATISTICS_CLAUSE,
                    b.sequence(LPARENTHESIS, ON, UNIT_NAME, RPARENTHESIS)))

            b.rule(INDEX_ILM_POLICY_CLAUSE).define(
                b.firstOf(
                    b.sequence(OPTIMIZE, INDEX_ILM_CONDITION_CLAUSE),
                    b.sequence(SEGMENT, TIER, TO, LOW_COST_TBS, b.optional(UNIT_NAME))))

            b.rule(INDEX_ILM_ACTION).define(
                b.firstOf(
                    b.sequence(ADD, POLICY, b.optional(INDEX_ILM_POLICY_CLAUSE)),
                    b.sequence(b.firstOf(DELETE, ENABLE, DISABLE), POLICY, IDENTIFIER_NAME),
                    b.firstOf(DELETE_ALL, ENABLE_ALL, DISABLE_ALL)))

            b.rule(INDEX_ILM_CLAUSE).define(
                ILM,
                b.firstOf(
                    b.sequence(LPARENTHESIS, INDEX_ILM_ACTION, RPARENTHESIS),
                    INDEX_ILM_ACTION))

            b.rule(INDEX_ANNOTATION).define(
                b.optional(b.firstOf(
                    b.sequence(ADD, b.optional(b.firstOf(
                        b.sequence(IF, NOT, EXISTS),
                        b.sequence(OR, REPLACE)))),
                    b.sequence(DROP, b.optional(IF, EXISTS)),
                    REPLACE)),
                IDENTIFIER_NAME,
                b.optional(CHARACTER_LITERAL))

            b.rule(INDEX_ANNOTATIONS_CLAUSE).define(
                ANNOTATIONS, LPARENTHESIS,
                INDEX_ANNOTATION, b.zeroOrMore(COMMA, INDEX_ANNOTATION),
                RPARENTHESIS)

            b.rule(CREATE_INDEX_ATTRIBUTE).define(
                b.firstOf(
                    INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
                    LOGGING_CLAUSE,
                    ONLINE,
                    b.sequence(TABLESPACE, b.firstOf(IDENTIFIER_NAME, DEFAULT)),
                    INDEX_COMPRESSION_CLAUSE,
                    b.firstOf(SORT, NOSORT),
                    REVERSE,
                    b.firstOf(VISIBLE, INVISIBLE),
                    INDEX_PARTIAL_CLAUSE,
                    INDEX_PARALLEL_CLAUSE,
                    INDEX_ANNOTATIONS_CLAUSE))

            b.rule(CREATE_INDEX_ATTRIBUTES).define(
                b.oneOrMore(CREATE_INDEX_ATTRIBUTE))

            b.rule(CREATE_INDEX_SCHEMA_OBJECT_NAME).define(
                b.optional(IDENTIFIER_NAME, DOT), IDENTIFIER_NAME)

            b.rule(CREATE_INDEX_EXPR).define(
                b.firstOf(
                    SingleRowSqlFunctionsGrammar.JSON_TABLE_EXPRESSION,
                    EXPRESSION),
                b.optional(b.firstOf(ASC, DESC)))

            b.rule(CREATE_INDEX_PARTITION_STORAGE_CLAUSE).define(
                b.oneOrMore(b.firstOf(
                    INDEX_SEGMENT_ATTRIBUTES_CLAUSE,
                    INDEX_COMPRESSION_CLAUSE,
                    b.sequence(OVERFLOW, STORE, IN, LPARENTHESIS,
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS))))

            b.rule(CREATE_INDEX_PARTITIONING_CLAUSE).define(
                PARTITION, b.optional(IDENTIFIER_NAME),
                VALUES, LESS, THAN, LPARENTHESIS,
                b.firstOf(LITERAL, MAXVALUE),
                b.zeroOrMore(COMMA, b.firstOf(LITERAL, MAXVALUE)),
                RPARENTHESIS,
                b.optional(INDEX_SEGMENT_ATTRIBUTES_CLAUSE))

            b.rule(CREATE_INDEX_HASH_PARTITIONS).define(
                LPARENTHESIS,
                PARTITION, b.optional(IDENTIFIER_NAME),
                b.optional(CREATE_INDEX_PARTITION_STORAGE_CLAUSE),
                b.zeroOrMore(COMMA,
                    PARTITION, b.optional(IDENTIFIER_NAME),
                    b.optional(CREATE_INDEX_PARTITION_STORAGE_CLAUSE)),
                RPARENTHESIS)

            b.rule(CREATE_INDEX_HASH_PARTITIONS_BY_QUANTITY).define(
                PARTITIONS, INTEGER_LITERAL,
                b.optional(b.sequence(STORE, IN, LPARENTHESIS,
                    IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)),
                b.optional(INDEX_COMPRESSION_CLAUSE),
                b.optional(b.sequence(OVERFLOW, STORE, IN, LPARENTHESIS,
                    IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)))

            b.rule(CREATE_INDEX_GLOBAL_PARTITIONED).define(
                GLOBAL, PARTITION, BY,
                b.firstOf(
                    b.sequence(
                        RANGE_KEYWORD, LPARENTHESIS,
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS,
                        LPARENTHESIS,
                        CREATE_INDEX_PARTITIONING_CLAUSE,
                        b.zeroOrMore(COMMA, CREATE_INDEX_PARTITIONING_CLAUSE),
                        RPARENTHESIS),
                    b.sequence(
                        HASH, LPARENTHESIS,
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS,
                        b.firstOf(CREATE_INDEX_HASH_PARTITIONS,
                            CREATE_INDEX_HASH_PARTITIONS_BY_QUANTITY))))

            b.rule(CREATE_INDEX_SUBPARTITION).define(
                SUBPARTITION, b.optional(IDENTIFIER_NAME),
                b.optional(b.sequence(TABLESPACE, IDENTIFIER_NAME)),
                b.optional(INDEX_COMPRESSION_CLAUSE),
                b.optional(b.firstOf(USABLE, UNUSABLE)))

            b.rule(CREATE_INDEX_SUBPARTITION_CLAUSE).define(
                b.firstOf(
                    b.sequence(STORE, IN, LPARENTHESIS,
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                    b.sequence(LPARENTHESIS,
                        CREATE_INDEX_SUBPARTITION,
                        b.zeroOrMore(COMMA, CREATE_INDEX_SUBPARTITION),
                        RPARENTHESIS)))

            b.rule(CREATE_INDEX_LOCAL_RANGE_PARTITIONS).define(
                LPARENTHESIS,
                PARTITION, b.optional(IDENTIFIER_NAME),
                b.zeroOrMore(b.firstOf(INDEX_SEGMENT_ATTRIBUTES_CLAUSE, INDEX_COMPRESSION_CLAUSE)),
                b.optional(b.firstOf(USABLE, UNUSABLE)),
                b.zeroOrMore(COMMA,
                    PARTITION, b.optional(IDENTIFIER_NAME),
                    b.zeroOrMore(b.firstOf(INDEX_SEGMENT_ATTRIBUTES_CLAUSE, INDEX_COMPRESSION_CLAUSE)),
                    b.optional(b.firstOf(USABLE, UNUSABLE))),
                RPARENTHESIS)

            b.rule(CREATE_INDEX_LOCAL_HASH_PARTITIONS).define(
                b.firstOf(
                    b.sequence(STORE, IN, LPARENTHESIS,
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                    b.sequence(LPARENTHESIS,
                        PARTITION, b.optional(IDENTIFIER_NAME),
                        b.optional(b.sequence(TABLESPACE, IDENTIFIER_NAME)),
                        b.optional(b.firstOf(USABLE, UNUSABLE)),
                        b.zeroOrMore(COMMA,
                            PARTITION, b.optional(IDENTIFIER_NAME),
                            b.optional(b.sequence(TABLESPACE, IDENTIFIER_NAME)),
                            b.optional(b.firstOf(USABLE, UNUSABLE))),
                        RPARENTHESIS)))

            b.rule(CREATE_INDEX_LOCAL_COMPOSITE_PARTITIONS).define(
                b.optional(b.sequence(STORE, IN, LPARENTHESIS,
                    IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)),
                LPARENTHESIS,
                PARTITION, b.optional(IDENTIFIER_NAME),
                b.zeroOrMore(b.firstOf(INDEX_SEGMENT_ATTRIBUTES_CLAUSE, INDEX_COMPRESSION_CLAUSE)),
                b.optional(b.firstOf(USABLE, UNUSABLE)),
                b.optional(CREATE_INDEX_SUBPARTITION_CLAUSE),
                b.zeroOrMore(COMMA,
                    PARTITION, b.optional(IDENTIFIER_NAME),
                    b.zeroOrMore(b.firstOf(INDEX_SEGMENT_ATTRIBUTES_CLAUSE, INDEX_COMPRESSION_CLAUSE)),
                    b.optional(b.firstOf(USABLE, UNUSABLE)),
                    b.optional(CREATE_INDEX_SUBPARTITION_CLAUSE)),
                RPARENTHESIS)

            b.rule(CREATE_INDEX_LOCAL_PARTITIONED).define(
                LOCAL,
                b.optional(b.firstOf(
                    CREATE_INDEX_LOCAL_COMPOSITE_PARTITIONS,
                    CREATE_INDEX_LOCAL_RANGE_PARTITIONS,
                    CREATE_INDEX_LOCAL_HASH_PARTITIONS)))

            b.rule(CREATE_INDEX_LOCAL_DOMAIN_CLAUSE).define(
                LOCAL,
                b.optional(LPARENTHESIS,
                    PARTITION, IDENTIFIER_NAME, b.optional(INDEX_PARAMETERS_CLAUSE),
                    b.zeroOrMore(COMMA,
                        PARTITION, IDENTIFIER_NAME, b.optional(INDEX_PARAMETERS_CLAUSE)),
                    RPARENTHESIS))

            b.rule(CREATE_INDEX_DOMAIN_CLAUSE).define(
                UNIT_NAME,
                b.optional(CREATE_INDEX_LOCAL_DOMAIN_CLAUSE),
                b.optional(INDEX_PARALLEL_CLAUSE),
                b.optional(INDEX_PARAMETERS_CLAUSE))

            b.rule(CREATE_INDEX_LOCAL_XMLINDEX_CLAUSE).define(
                LOCAL,
                b.optional(LPARENTHESIS,
                    PARTITION, IDENTIFIER_NAME, b.optional(INDEX_PARAMETERS_CLAUSE),
                    b.zeroOrMore(COMMA,
                        PARTITION, IDENTIFIER_NAME, b.optional(INDEX_PARAMETERS_CLAUSE)),
                    RPARENTHESIS))

            b.rule(CREATE_INDEX_XMLINDEX_CLAUSE).define(
                b.optional(XDB, DOT), XMLINDEX,
                b.optional(CREATE_INDEX_LOCAL_XMLINDEX_CLAUSE),
                b.optional(INDEX_PARALLEL_CLAUSE),
                b.optional(INDEX_PARAMETERS_CLAUSE))

            b.rule(CREATE_INDEX_PROPERTIES).define(
                b.firstOf(
                    b.oneOrMore(b.firstOf(
                        CREATE_INDEX_GLOBAL_PARTITIONED,
                        CREATE_INDEX_LOCAL_PARTITIONED,
                        CREATE_INDEX_ATTRIBUTE)),
                    b.sequence(INDEXTYPE, IS,
                        b.firstOf(CREATE_INDEX_XMLINDEX_CLAUSE, CREATE_INDEX_DOMAIN_CLAUSE))))

            b.rule(CREATE_INDEX_ON_CLAUSE).define(
                ON,
                b.firstOf(
                    CREATE_INDEX_CLUSTER_CLAUSE,
                    CREATE_INDEX_BITMAP_JOIN_CLAUSE,
                    CREATE_INDEX_TABLE_CLAUSE))

            b.rule(CREATE_INDEX_CLUSTER_CLAUSE).define(
                CLUSTER, CREATE_INDEX_SCHEMA_OBJECT_NAME,
                b.optional(CREATE_INDEX_ATTRIBUTES))

            b.rule(CREATE_INDEX_TABLE_CLAUSE).define(
                CREATE_INDEX_SCHEMA_OBJECT_NAME,
                b.optional(IDENTIFIER_NAME),
                LPARENTHESIS,
                CREATE_INDEX_EXPR,
                b.zeroOrMore(COMMA, CREATE_INDEX_EXPR),
                RPARENTHESIS,
                b.optional(CREATE_INDEX_PROPERTIES))

            b.rule(CREATE_INDEX_BITMAP_JOIN_CLAUSE).define(
                CREATE_INDEX_SCHEMA_OBJECT_NAME,
                b.optional(IDENTIFIER_NAME),
                LPARENTHESIS,
                b.optional(b.firstOf(
                    b.sequence(IDENTIFIER_NAME, DOT, IDENTIFIER_NAME, DOT),
                    b.sequence(IDENTIFIER_NAME, DOT))),
                IDENTIFIER_NAME, b.optional(b.firstOf(ASC, DESC)),
                b.zeroOrMore(COMMA,
                    b.optional(b.firstOf(
                        b.sequence(IDENTIFIER_NAME, DOT, IDENTIFIER_NAME, DOT),
                        b.sequence(IDENTIFIER_NAME, DOT))),
                    IDENTIFIER_NAME, b.optional(b.firstOf(ASC, DESC))),
                RPARENTHESIS,
                FROM,
                CREATE_INDEX_SCHEMA_OBJECT_NAME, b.optional(IDENTIFIER_NAME),
                b.zeroOrMore(COMMA,
                    CREATE_INDEX_SCHEMA_OBJECT_NAME, b.optional(IDENTIFIER_NAME)),
                WHERE, BOOLEAN_EXPRESSION,
                b.optional(CREATE_INDEX_LOCAL_PARTITIONED),
                b.optional(CREATE_INDEX_ATTRIBUTES))

            b.rule(CREATE_INDEX).define(
                CREATE,
                b.firstOf(
                    b.sequence(
                        JSON,
                        b.optional(UNIQUE),
                        b.optional(b.firstOf(SPARSE, DENSE)),
                        b.optional(b.firstOf(SINGLEVALUE, MULTIVALUE))),
                    b.sequence(
                        b.optional(JSON),
                        b.optional(b.firstOf(UNIQUE, BITMAP, MULTIVALUE, SPARSE, DENSE))),
                ),
                INDEX,
                b.optional(IF, NOT, EXISTS),
                CREATE_INDEX_SCHEMA_OBJECT_NAME,
                // Oracle documents both placements: before ON in the SQL Reference diagram,
                // and after the indexed object in the VLDB guide example.
                b.firstOf(
                    b.sequence(INDEX_ILM_CLAUSE, CREATE_INDEX_ON_CLAUSE),
                    b.sequence(CREATE_INDEX_ON_CLAUSE, b.optional(INDEX_ILM_CLAUSE))),
                b.optional(b.firstOf(USABLE, UNUSABLE)),
                b.optional(b.firstOf(DEFERRED, IMMEDIATE), INVALIDATION),
                b.optional(SEMICOLON))

            b.rule(INDEX_SEGMENT_ATTRIBUTES_CLAUSE).define(
                b.oneOrMore(b.firstOf(
                    INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
                    b.sequence(TABLESPACE, IDENTIFIER_NAME),
                    LOGGING_CLAUSE)))

            b.rule(INDEX_PARTITION_DESCRIPTION).define(
                PARTITION,
                b.optional(IDENTIFIER_NAME),
                b.optional(b.firstOf(
                    b.oneOrMore(b.firstOf(
                        INDEX_SEGMENT_ATTRIBUTES_CLAUSE,
                        INDEX_COMPRESSION_CLAUSE)),
                    INDEX_PARAMETERS_CLAUSE)))

            b.rule(INDEX_REBUILD_CLAUSE).define(
                REBUILD,
                b.optional(b.firstOf(
                    b.sequence(PARTITION, IDENTIFIER_NAME),
                    b.sequence(SUBPARTITION, IDENTIFIER_NAME),
                    REVERSE,
                    NOREVERSE)),
                b.zeroOrMore(b.firstOf(
                    INDEX_PARALLEL_CLAUSE,
                    b.sequence(TABLESPACE, IDENTIFIER_NAME),
                    INDEX_PARAMETERS_CLAUSE,
                    ONLINE,
                    INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
                    INDEX_COMPRESSION_CLAUSE,
                    LOGGING_CLAUSE,
                    INDEX_PARTIAL_CLAUSE)),
                b.optional(b.firstOf(DEFERRED, IMMEDIATE), INVALIDATION))

            b.rule(MODIFY_INDEX_DEFAULT_ATTRS).define(
                MODIFY, DEFAULT, ATTRIBUTES,
                b.optional(FOR, PARTITION, IDENTIFIER_NAME),
                b.oneOrMore(b.firstOf(
                    INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
                    b.sequence(TABLESPACE, b.firstOf(IDENTIFIER_NAME, DEFAULT)),
                    LOGGING_CLAUSE)))

            b.rule(ADD_HASH_INDEX_PARTITION).define(
                ADD, PARTITION, b.optional(IDENTIFIER_NAME),
                b.optional(b.sequence(TABLESPACE, IDENTIFIER_NAME)),
                b.optional(INDEX_COMPRESSION_CLAUSE),
                b.optional(INDEX_PARALLEL_CLAUSE))

            b.rule(MODIFY_INDEX_PARTITION).define(
                MODIFY, PARTITION, IDENTIFIER_NAME,
                b.firstOf(
                    b.oneOrMore(b.firstOf(
                        INDEX_DEALLOCATE_UNUSED_CLAUSE,
                        INDEX_ALLOCATE_EXTENT_CLAUSE,
                        INDEX_PHYSICAL_ATTRIBUTES_CLAUSE,
                        LOGGING_CLAUSE,
                        INDEX_COMPRESSION_CLAUSE)),
                    INDEX_PARAMETERS_CLAUSE,
                    b.sequence(COALESCE, b.optional(CLEANUP), b.optional(INDEX_PARALLEL_CLAUSE)),
                    b.sequence(UPDATE, BLOCK, REFERENCES),
                    UNUSABLE))

            b.rule(RENAME_INDEX_PARTITION).define(
                RENAME,
                b.firstOf(
                    b.sequence(PARTITION, IDENTIFIER_NAME),
                    b.sequence(SUBPARTITION, IDENTIFIER_NAME)),
                TO, IDENTIFIER_NAME)

            b.rule(DROP_INDEX_PARTITION).define(
                DROP, PARTITION, IDENTIFIER_NAME)

            b.rule(SPLIT_INDEX_PARTITION).define(
                SPLIT, PARTITION, IDENTIFIER_NAME,
                AT, LPARENTHESIS,
                LITERAL, b.zeroOrMore(COMMA, LITERAL),
                RPARENTHESIS,
                INTO, LPARENTHESIS,
                INDEX_PARTITION_DESCRIPTION, COMMA, INDEX_PARTITION_DESCRIPTION,
                RPARENTHESIS,
                b.optional(INDEX_PARALLEL_CLAUSE))

            b.rule(COALESCE_INDEX_PARTITION).define(
                COALESCE, PARTITION, b.optional(INDEX_PARALLEL_CLAUSE))

            b.rule(MODIFY_INDEX_SUBPARTITION).define(
                MODIFY, SUBPARTITION, IDENTIFIER_NAME,
                b.firstOf(
                    UNUSABLE,
                    INDEX_ALLOCATE_EXTENT_CLAUSE,
                    INDEX_DEALLOCATE_UNUSED_CLAUSE))

            b.rule(ALTER_INDEX_PARTITIONING).define(
                b.firstOf(
                    MODIFY_INDEX_DEFAULT_ATTRS,
                    ADD_HASH_INDEX_PARTITION,
                    MODIFY_INDEX_PARTITION,
                    RENAME_INDEX_PARTITION,
                    DROP_INDEX_PARTITION,
                    SPLIT_INDEX_PARTITION,
                    COALESCE_INDEX_PARTITION,
                    MODIFY_INDEX_SUBPARTITION))

            b.rule(ALTER_INDEX_ACTION).define(
                b.firstOf(
                    b.oneOrMore(b.firstOf(
                        INDEX_DEALLOCATE_UNUSED_CLAUSE,
                        INDEX_ALLOCATE_EXTENT_CLAUSE,
                        INDEX_SHRINK_CLAUSE,
                        INDEX_PARALLEL_CLAUSE,
                        INDEX_PHYSICAL_ATTRIBUTES_CLAUSE,
                        LOGGING_CLAUSE,
                        INDEX_PARTIAL_CLAUSE)),
                    INDEX_REBUILD_CLAUSE,
                    INDEX_PARAMETERS_CLAUSE,
                    COMPILE,
                    b.firstOf(ENABLE, DISABLE),
                    b.sequence(UNUSABLE, b.optional(ONLINE),
                        b.optional(b.firstOf(DEFERRED, IMMEDIATE), INVALIDATION)),
                    b.firstOf(VISIBLE, INVISIBLE),
                    b.sequence(RENAME, TO, IDENTIFIER_NAME),
                    ALTER_INDEX_PARTITIONING,
                    b.sequence(COALESCE, b.optional(CLEANUP), b.optional(ONLY), b.optional(INDEX_PARALLEL_CLAUSE)),
                    b.sequence(b.firstOf(MONITORING, NOMONITORING), USAGE),
                    b.sequence(UPDATE, BLOCK, REFERENCES),
                    INDEX_ANNOTATIONS_CLAUSE))

            b.rule(ALTER_INDEX).define(
                ALTER, INDEX, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    b.sequence(INDEX_ILM_CLAUSE, b.optional(ALTER_INDEX_ACTION)),
                    ALTER_INDEX_ACTION),
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

            val sequenceInteger = b.sequence(
                    b.optional(b.firstOf(PLUS, MINUS)),
                    b.next(INTEGER_LITERAL),
                    NUMERIC_LITERAL)

            b.rule(CREATE_SEQUENCE).define(
                    CREATE, SEQUENCE, UNIT_NAME,
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, DATA, NONE)),
                    b.zeroOrMore(b.firstOf(
                            b.sequence(INCREMENT, BY, sequenceInteger),
                            b.sequence(START, WITH, sequenceInteger),
                            b.sequence(MAXVALUE, sequenceInteger),
                            NOMAXVALUE,
                            b.sequence(MINVALUE, sequenceInteger),
                            NOMINVALUE,
                            CYCLE,
                            NOCYCLE,
                            b.sequence(CACHE, sequenceInteger),
                            NOCACHE,
                            ORDER,
                            NOORDER,
                            KEEP,
                            NOKEEP,
                            b.sequence(SCALE, b.firstOf(EXTEND, NOEXTEND)),
                            NOSCALE,
                            SESSION,
                            GLOBAL)),
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
                CREATE_INDEX,
                ALTER_TABLE,
                ALTER_INDEX,
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
