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
import com.felipebz.flr.grammar.ContextKey
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.api.PlSqlGrammar.*
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*
import com.felipebz.zpa.api.PlSqlTokenType.INTEGER_LITERAL
import com.felipebz.zpa.grammar.JavaSourceTextExpression
import com.felipebz.zpa.grammar.JavaResolverMatchStringExpression
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

/**
 * Set while parsing CREATE TABLE and CREATE DOMAIN. Oracle 26 rejects DROP, REPLACE and ADD OR REPLACE
 * annotation directives in both at parse time (ORA-11555/ORA-11556 at the directive), while
 * ALTER TABLE, including ALTER TABLE ADD column, accepts them.
 */
internal val CREATE_ANNOTATIONS_CONTEXT: ContextKey<Boolean> = ContextKey()

enum class DdlGrammar : GrammarRuleKey {

    DDL_COMMENT,
    DDL_COMMAND,
    ONE_OR_MORE_IDENTIFIERS,
    REFERENCES_CLAUSE,
    INLINE_CONSTRAINT,
    OUT_OF_LINE_CONSTRAINT,
    OUT_OF_LINE_REF_CONSTRAINT,
    USING_INDEX_CLAUSE,
    ANNOTATIONS_CLAUSE,
    ANNOTATION,
    TABLE_COLUMN_DEFINITION,
    TABLE_RELATIONAL_PROPERTIES,
    OBJECT_TABLE_CLAUSE,
    OBJECT_TABLE_SUBSTITUTION,
    OBJECT_TABLE_PROPERTIES,
    OBJECT_IDENTIFIER_CLAUSE,
    NESTED_TABLE_COL_PROPERTIES,
    ALTER_TABLE_COLUMN,
    ALTER_TABLE_MODIFY_CONSTRAINT,
    ALTER_TABLE_CONSTRAINT_STATE,
    ENABLE_DISABLE_CLAUSE,
    DROP_COLUMN_CLAUSE,
    PARTITION_EXTENDED_NAME,
    SUBPARTITION_EXTENDED_NAME,
    RENAME_PARTITION_SUBPART,
    EXCHANGE_PARTITION_SUBPART,
    MOVE_TABLE_PARTITION,
    TRUNCATE_PARTITION_SUBPART,
    ADD_RANGE_TABLE_PARTITIONS,
    SPLIT_TABLE_PARTITION,
    MERGE_TABLE_PARTITIONS,
    MODIFY_PARTITION_LOCAL_INDEXES,
    SPLIT_NESTED_TABLE_PART,
    UPDATE_INDEX_CLAUSES,
    DROP_CONSTRAINT_CLAUSE,
    ALTER_SYSTEM,
    ALTER_LOCKDOWN_PROFILE,
    LOCKDOWN_FEATURES,
    LOCKDOWN_OPTIONS,
    LOCKDOWN_STATEMENTS,
    LOCKDOWN_OPTION_VALUES,
    CREATE_DOMAIN,
    DOMAIN_CONSTRAINT,
    CREATE_AUDIT_POLICY,
    ALTER_AUDIT_POLICY,
    AUDIT_PRIVILEGE_CLAUSE,
    AUDIT_ACTION_CLAUSE,
    AUDIT_ROLE_CLAUSE,
    CREATE_PROPERTY_GRAPH,
    PROPERTY_GRAPH_VERTEX_TABLE,
    PROPERTY_GRAPH_EDGE_TABLE,
    PROPERTY_GRAPH_PROPERTIES,
    CREATE_USER,
    USER_AUTHENTICATION_CLAUSE,
    CREATE_CONTEXT,
    CALL_COMMAND,
    CREATE_TABLE,
    INDEX_ORGANIZED_TABLE_CLAUSE,
    INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE,
    CREATE_INDEX,
    CREATE_INDEX_FOR_CONSTRAINT,
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
    CREATE_JAVA,
    CREATE_JAVA_OBJECT,
    CREATE_JAVA_SOURCE,
    CREATE_JAVA_CLASS,
    CREATE_JAVA_RESOURCE,
    JAVA_NAMED_CLAUSE,
    JAVA_SCHEMA_CLAUSE,
    JAVA_SHARING_CLAUSE,
    JAVA_AUTHID_CLAUSE,
    JAVA_RESOLVER_CLAUSE,
    JAVA_RESOLVER_ENTRY,
    JAVA_RESOLVER_MATCH_STRING,
    JAVA_RESOLVER_SCHEMA_NAME,
    JAVA_USING_CLAUSE,
    JAVA_SOURCE_TEXT,
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
    CONSTRAINT_STATE_WITHOUT_USING_INDEX,
    PRECHECK_STATE,
    EXCEPTIONS_CLAUSE;

    companion object {
        fun buildOn(b: PlSqlGrammarBuilder) {
            createDdlCommands(b)
        }

        private fun createDdlCommands(b: PlSqlGrammarBuilder) {
            fun createIndexHeader() = b.sequence(
                CREATE,
                b.firstOf(
                    b.sequence(
                        JSON,
                        b.optional(UNIQUE),
                        b.optional(b.firstOf(SPARSE, DENSE)),
                        b.optional(b.firstOf(SINGLEVALUE, MULTIVALUE))),
                    b.sequence(
                        b.optional(JSON),
                        b.optional(b.firstOf(UNIQUE, BITMAP, MULTIVALUE, SPARSE, DENSE)))
                ),
                INDEX,
                b.optional(IF, NOT, EXISTS),
                CREATE_INDEX_SCHEMA_OBJECT_NAME
            )

            fun usingIndexProperties() = b.oneOrMore(b.firstOf(
                CREATE_INDEX_GLOBAL_PARTITIONED,
                CREATE_INDEX_LOCAL_PARTITIONED,
                INDEX_PHYSICAL_ATTRIBUTES_WITH_PCTFREE_CLAUSE,
                LOGGING_CLAUSE,
                ONLINE,
                b.sequence(TABLESPACE, b.firstOf(IDENTIFIER_NAME, DEFAULT)),
                INDEX_COMPRESSION_CLAUSE,
                b.firstOf(SORT, NOSORT),
                REVERSE,
                b.firstOf(VISIBLE, INVISIBLE),
                INDEX_PARTIAL_CLAUSE,
                ANNOTATIONS_CLAUSE
            ))

            fun createIndexTableClauseForConstraint() = b.sequence(
                CREATE_INDEX_SCHEMA_OBJECT_NAME,
                b.optional(IDENTIFIER_NAME),
                LPARENTHESIS,
                CREATE_INDEX_EXPR,
                b.zeroOrMore(COMMA, CREATE_INDEX_EXPR),
                RPARENTHESIS,
                b.optional(usingIndexProperties())
            )

            fun objectTableProperty() = b.firstOf(
                OUT_OF_LINE_CONSTRAINT,
                b.sequence(IDENTIFIER_NAME, b.zeroOrMore(INLINE_CONSTRAINT))
            )

            fun nestedTableStorageProperty() = b.firstOf(
                NESTED_TABLE_COL_PROPERTIES,
                SEGMENT_ATTRIBUTES_CLAUSE,
                LOB_STORAGE_CLAUSE
            )

            fun tablePropertyClauses() = b.sequence(
                b.zeroOrMore(NESTED_TABLE_COL_PROPERTIES),
                b.zeroOrMore(
                    b.firstOf(
                        LOB_STORAGE_CLAUSE,
                        VARRAY_COL_PROPERTIES))
            )

            fun encryptionPassword() = b.firstOf(
                IDENTIFIER_NAME,
                CHARACTER_LITERAL,
                NUMERIC_LITERAL,
                NULL_LITERAL,
                BOOLEAN_LITERAL
            )

            // Oracle 26 accepts SALT before or after the integrity literal; keep both forms bounded.
            fun encryptionSpec() = b.sequence(
                b.optional(USING, CHARACTER_LITERAL),
                b.optional(IDENTIFIED, BY, encryptionPassword()),
                b.optional(b.firstOf(
                    b.sequence(
                        CHARACTER_LITERAL,
                        b.optional(b.optional(NO), SALT)
                    ),
                    b.sequence(
                        b.optional(NO), SALT,
                        b.optional(CHARACTER_LITERAL)
                    )
                ))
            )

            fun columnEncryptionClause() = b.sequence(ENCRYPT, encryptionSpec())

            fun indexOrganizedTableAttribute() = b.firstOf(
                KEY_COMPRESSION,
                b.sequence(PCTTHRESHOLD, INTEGER_LITERAL),
                b.firstOf(
                    b.sequence(COMPRESS, ADVANCED, b.optional(LOW)),
                    b.sequence(COMPRESS, b.optional(INTEGER_LITERAL)),
                    NOCOMPRESS
                ),
                SEGMENT_ATTRIBUTES_CLAUSE
            )

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
                            UNIQUE,
                            b.sequence(PRIMARY, KEY)
                        ), b.optional(CONSTRAINT_STATE)
                    ),
                    b.sequence(
                        b.firstOf(
                            b.sequence(b.optional(NOT), NULL),
                            REFERENCES_CLAUSE
                        ), b.optional(CONSTRAINT_STATE_WITHOUT_USING_INDEX)
                    ),
                    b.sequence(
                        CHECK, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                        b.optional(CONSTRAINT_STATE_WITHOUT_USING_INDEX),
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
                b.optional(USING_INDEX_CLAUSE),
                b.optional(b.firstOf(ENABLE, DISABLE)),
                b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                b.optional(EXCEPTIONS_CLAUSE)
            )

            b.rule(CONSTRAINT_STATE_WITHOUT_USING_INDEX).define(
                b.optional(b.firstOf(
                    b.sequence(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE), b.optional(b.optional(NOT), DEFERRABLE)),
                    b.sequence(b.optional(NOT), DEFERRABLE, b.optional(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE))),
                )),
                b.optional(b.firstOf(RELY, NORELY)),
                b.optional(b.firstOf(ENABLE, DISABLE)),
                b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                b.optional(EXCEPTIONS_CLAUSE)
            )

            b.rule(USING_INDEX_CLAUSE).define(
                USING, INDEX,
                b.optional(
                    b.firstOf(
                        b.sequence(LPARENTHESIS, CREATE_INDEX_FOR_CONSTRAINT, RPARENTHESIS),
                        usingIndexProperties(),
                        b.sequence(b.nextNot(b.sequence(EXCEPTIONS, INTO)), UNIT_NAME)
                    )
                )
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
                    b.optional(columnEncryptionClause()),
                    b.zeroOrMore(INLINE_CONSTRAINT),
                    // Oracle 26 accepts annotations only after DEFAULT, encryption and inline constraints.
                    b.optional(ANNOTATIONS_CLAUSE))

            b.rule(OUT_OF_LINE_CONSTRAINT).define(
                b.optional(b.firstOf(CONSTRAINT, CONSTRAINTS), IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                            b.sequence(PRIMARY, KEY, ONE_OR_MORE_IDENTIFIERS),
                        ), b.optional(CONSTRAINT_STATE)
                    ),
                    b.sequence(
                        FOREIGN, KEY, ONE_OR_MORE_IDENTIFIERS, REFERENCES_CLAUSE,
                        b.optional(CONSTRAINT_STATE_WITHOUT_USING_INDEX)
                    ),
                    b.sequence(
                        CHECK, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                        b.optional(CONSTRAINT_STATE_WITHOUT_USING_INDEX),
                        b.optional(PRECHECK_STATE)
                    )
                )
            )

            b.rule(OUT_OF_LINE_REF_CONSTRAINT).define(
                b.firstOf(
                    b.sequence(
                        SCOPE, FOR, LPARENTHESIS, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                        RPARENTHESIS, IS, UNIT_NAME),
                    b.sequence(
                        REF, LPARENTHESIS, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                        RPARENTHESIS, WITH, ROWID)
                )
            )

            b.rule(TABLE_RELATIONAL_PROPERTIES).define(
                    b.oneOrMore(b.firstOf(
                        OUT_OF_LINE_REF_CONSTRAINT,
                        OUT_OF_LINE_CONSTRAINT,
                        b.sequence(
                            b.nextNot(b.firstOf(b.sequence(SCOPE, FOR), b.sequence(REF, LPARENTHESIS))),
                            TABLE_COLUMN_DEFINITION)), b.optional(COMMA)))
            b.rule(OBJECT_TABLE_PROPERTIES).define(
                LPARENTHESIS,
                objectTableProperty(),
                b.zeroOrMore(COMMA, objectTableProperty()),
                RPARENTHESIS
            )

            b.rule(OBJECT_IDENTIFIER_CLAUSE).define(
                OBJECT,
                "IDENTIFIER",
                IS,
                b.firstOf(
                    b.sequence(PRIMARY, KEY),
                    b.sequence("SYSTEM", "GENERATED")
                )
            )

            b.rule(OBJECT_TABLE_SUBSTITUTION).define(
                b.optional(NOT),
                SUBSTITUTABLE,
                AT,
                ALL,
                LEVELS
            )

            b.rule(OBJECT_TABLE_CLAUSE).define(
                OF,
                UNIT_NAME,
                b.optional(OBJECT_TABLE_SUBSTITUTION),
                b.optional(OBJECT_TABLE_PROPERTIES),
                b.optional(
                    ON,
                    COMMIT,
                    b.firstOf(
                        DELETE,
                        PRESERVE),
                    ROWS),
                b.optional(OBJECT_IDENTIFIER_CLAUSE)
            )

            b.rule(NESTED_TABLE_COL_PROPERTIES).define(
                NESTED,
                TABLE,
                IDENTIFIER_NAME,
                b.optional(SUBSTITUTABLE_COLUMN_CLAUSE),
                b.optional(b.firstOf(LOCAL, GLOBAL)),
                STORE,
                AS,
                IDENTIFIER_NAME,
                b.optional(
                    LPARENTHESIS,
                    b.oneOrMore(
                        nestedTableStorageProperty(),
                        b.optional(COMMA)
                    ),
                    RPARENTHESIS
                ),
                b.optional(
                    RETURN,
                    b.optional(AS),
                    b.firstOf("LOCATOR", VALUE)
                )
            )

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

            b.rule(INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE).define(
                OVERFLOW,
                b.optional(SEGMENT_ATTRIBUTES_CLAUSE)
            )

            b.rule(INDEX_ORGANIZED_TABLE_CLAUSE).define(
                ORGANIZATION,
                INDEX,
                b.zeroOrMore(indexOrganizedTableAttribute()),
                b.optional(
                    b.firstOf(
                        b.sequence(
                            INCLUDING,
                            IDENTIFIER_NAME,
                            b.zeroOrMore(indexOrganizedTableAttribute()),
                            INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE
                        ),
                        INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE
                    )
                )
            )

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
                            OBJECT_TABLE_SUBSTITUTION))

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
                                                    VARRAY_COL_PROPERTIES,
                                                    NESTED_TABLE_COL_PROPERTIES))),
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
                            PlSqlKeyword.HASH,
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
                            PlSqlKeyword.HASH,
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

            fun tablePartitioning() = b.optional(b.firstOf(
                    PARTITION_BY_RANGE,
                    PARTITION_BY_HASH,
                    PARTITION_BY_LIST,
                    PARTITION_COMPOSITE))

            // Oracle 26 accepts table annotations after the IOT clause, around partitioning and
            // TABLESPACE, and repeats them (`ANNOTATIONS(A '1') ANNOTATIONS(B '2')` executes). They
            // must not precede ORGANIZATION INDEX (ORA-64303) or ON COMMIT (ORA-00922).
            fun tableAnnotations() = b.zeroOrMore(ANNOTATIONS_CLAUSE)

            fun tableSuffixesWithAnnotations() = b.sequence(
                    tableAnnotations(),
                    tablePartitioning(),
                    tableAnnotations(),
                    b.optional(TABLESPACE, IDENTIFIER_NAME, tableAnnotations()))

            b.rule(CREATE_TABLE).define(
                    CREATE,
                    b.optional(
                            GLOBAL,
                            TEMPORARY),
                    TABLE,
                    UNIT_NAME,
                    b.withContext(CREATE_ANNOTATIONS_CONTEXT, true, b.firstOf(
                            b.sequence(
                                    OBJECT_TABLE_CLAUSE,
                                    tablePropertyClauses(),
                                    b.optional(INDEX_ORGANIZED_TABLE_CLAUSE),
                                    tableSuffixesWithAnnotations()),
                            b.sequence(
                                    b.optional(
                                            LPARENTHESIS,
                                            TABLE_RELATIONAL_PROPERTIES,
                                            RPARENTHESIS),
                                    tablePropertyClauses(),
                                    b.optional(INDEX_ORGANIZED_TABLE_CLAUSE),
                                    b.firstOf(
                                            b.sequence(
                                                    tablePartitioning(),
                                                    b.optional(
                                                            TABLESPACE,
                                                            IDENTIFIER_NAME),
                                                    ON,
                                                    COMMIT,
                                                    b.firstOf(
                                                            DELETE,
                                                            PRESERVE),
                                                    ROWS,
                                                    tableAnnotations()),
                                            tableSuffixesWithAnnotations())))),
                    b.optional(AS, DmlGrammar.SELECT_EXPRESSION),
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

            val notInCreate = b.nextNot(b.requireContext(CREATE_ANNOTATIONS_CONTEXT, true))

            b.rule(ANNOTATION).define(
                b.optional(b.firstOf(
                    b.sequence(ADD, b.optional(b.firstOf(
                        b.sequence(IF, NOT, EXISTS),
                        b.sequence(notInCreate, OR, REPLACE)))),
                    b.sequence(notInCreate, DROP, b.optional(IF, EXISTS)),
                    b.sequence(notInCreate, REPLACE))),
                IDENTIFIER_NAME,
                b.optional(CHARACTER_LITERAL))

            b.rule(ANNOTATIONS_CLAUSE).define(
                ANNOTATIONS, LPARENTHESIS,
                ANNOTATION, b.zeroOrMore(COMMA, ANNOTATION),
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
                    ANNOTATIONS_CLAUSE))

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
                        PlSqlKeyword.HASH, LPARENTHESIS,
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
                createIndexHeader(),
                // Oracle documents both placements: before ON in the SQL Reference diagram,
                // and after the indexed object in the VLDB guide example.
                b.firstOf(
                    b.sequence(INDEX_ILM_CLAUSE, CREATE_INDEX_ON_CLAUSE),
                    b.sequence(CREATE_INDEX_ON_CLAUSE, b.optional(INDEX_ILM_CLAUSE))),
                b.optional(b.firstOf(USABLE, UNUSABLE)),
                b.optional(b.firstOf(DEFERRED, IMMEDIATE), INVALIDATION),
                b.optional(SEMICOLON))

            b.rule(CREATE_INDEX_FOR_CONSTRAINT).define(
                createIndexHeader(),
                b.firstOf(
                    b.sequence(
                        INDEX_ILM_CLAUSE,
                        ON,
                        createIndexTableClauseForConstraint()),
                    b.sequence(
                        ON,
                        createIndexTableClauseForConstraint(),
                        b.optional(INDEX_ILM_CLAUSE))),
                b.optional(b.firstOf(USABLE, UNUSABLE)),
                b.optional(b.firstOf(DEFERRED, IMMEDIATE), INVALIDATION))

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
                    ANNOTATIONS_CLAUSE))

            b.rule(ALTER_INDEX).define(
                ALTER, INDEX, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    b.sequence(INDEX_ILM_CLAUSE, b.optional(ALTER_INDEX_ACTION)),
                    ALTER_INDEX_ACTION),
                b.optional(SEMICOLON))

            b.rule(ALTER_TABLE_COLUMN).define(
                    IDENTIFIER_NAME,
                    b.optional(b.sequence(
                            b.nextNot(b.firstOf(COLLATE, DEFAULT, CONSTRAINT, CONSTRAINTS, NOT, NULL, ANNOTATIONS,
                                    ENCRYPT, DECRYPT)),
                            DATATYPE)),
                    b.optional(COLLATE, IDENTIFIER_NAME),
                    b.optional(DEFAULT, EXPRESSION),
                    b.optional(b.firstOf(columnEncryptionClause(), DECRYPT)),
                    b.zeroOrMore(INLINE_CONSTRAINT),
                    b.optional(ANNOTATIONS_CLAUSE))

            b.rule(DROP_COLUMN_CLAUSE).define(
                    b.firstOf(
                            b.sequence(
                                    DROP,
                                    b.firstOf(
                                            b.sequence(UNUSED, COLUMNS),
                                            b.sequence(
                                                    b.firstOf(
                                                            b.sequence(COLUMN, IDENTIFIER_NAME),
                                                            ONE_OR_MORE_IDENTIFIERS),
                                                    b.optional(CASCADE, CONSTRAINTS)))),
                            b.sequence(
                                    SET, UNUSED,
                                    b.firstOf(
                                            b.sequence(COLUMN, IDENTIFIER_NAME),
                                            ONE_OR_MORE_IDENTIFIERS))))

            b.rule(DROP_CONSTRAINT_CLAUSE).define(
                    DROP,
                    b.firstOf(
                            b.sequence(PRIMARY, KEY),
                            b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                            b.sequence(CONSTRAINT, IDENTIFIER_NAME)),
                    b.optional(CASCADE),
                    b.optional(b.firstOf(KEEP, DROP), INDEX),
                    b.optional(ONLINE))

            b.rule(ALTER_TABLE_CONSTRAINT_STATE).define(
                    b.next(b.firstOf(INITIALLY, NOT, DEFERRABLE, RELY, NORELY, USING,
                            ENABLE, DISABLE, VALIDATE, NOVALIDATE, EXCEPTIONS)),
                    CONSTRAINT_STATE)

            b.rule(ALTER_TABLE_MODIFY_CONSTRAINT).define(
                    MODIFY,
                    b.firstOf(
                            b.sequence(
                                    CONSTRAINT, IDENTIFIER_NAME,
                                    b.firstOf(
                                            b.sequence(ALTER_TABLE_CONSTRAINT_STATE,
                                                    b.optional(CASCADE), b.optional(PRECHECK_STATE)),
                                            PRECHECK_STATE)),
                            b.sequence(
                                    b.firstOf(
                                            b.sequence(PRIMARY, KEY),
                                            b.sequence(UNIQUE, LPARENTHESIS, IDENTIFIER_NAME,
                                                    b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)),
                                    ALTER_TABLE_CONSTRAINT_STATE, b.optional(CASCADE))))

            fun enableDisableTarget() = b.firstOf(
                    b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS),
                    b.sequence(PRIMARY, KEY),
                    b.sequence(CONSTRAINT, IDENTIFIER_NAME))

            b.rule(ENABLE_DISABLE_CLAUSE).define(
                    b.firstOf(
                            b.sequence(
                                    ENABLE, b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                                    enableDisableTarget(),
                                    b.optional(USING_INDEX_CLAUSE),
                                    b.optional(EXCEPTIONS_CLAUSE)),
                            b.sequence(
                                    DISABLE, b.optional(b.firstOf(VALIDATE, NOVALIDATE)),
                                    enableDisableTarget(),
                                    b.optional(CASCADE),
                                    b.optional(b.firstOf(KEEP, DROP), INDEX))))

            fun partitionSpec() = b.sequence(PARTITION, b.optional(IDENTIFIER_NAME),
                b.optional(TABLE_PARTITION_DESCRIPTION))

            fun splitListValues() = b.firstOf(
                b.sequence(b.firstOf(LITERAL, NULL),
                    b.zeroOrMore(COMMA, b.firstOf(LITERAL, NULL))),
                b.sequence(LPARENTHESIS, b.firstOf(LITERAL, NULL),
                    b.zeroOrMore(COMMA, b.firstOf(LITERAL, NULL)), RPARENTHESIS,
                    b.zeroOrMore(COMMA, LPARENTHESIS, b.firstOf(LITERAL, NULL),
                        b.zeroOrMore(COMMA, b.firstOf(LITERAL, NULL)), RPARENTHESIS)))

            fun splitListValuesClause() = b.sequence(VALUES, LPARENTHESIS,
                b.firstOf(DEFAULT, splitListValues()), RPARENTHESIS)

            fun splitRangeValuesClause() = b.sequence(VALUES, LESS, THAN, LPARENTHESIS,
                b.firstOf(MAXVALUE, EXPRESSION),
                b.zeroOrMore(COMMA, b.firstOf(MAXVALUE, EXPRESSION)), RPARENTHESIS)

            fun rangeSplitDescription() = b.sequence(PARTITION, b.optional(IDENTIFIER_NAME),
                splitRangeValuesClause(), TABLE_PARTITION_DESCRIPTION)

            fun listSplitDescription() = b.sequence(PARTITION, b.optional(IDENTIFIER_NAME),
                splitListValuesClause(), TABLE_PARTITION_DESCRIPTION)

            fun directorySplitDescription() = b.sequence(PARTITION, IDENTIFIER_NAME,
                TABLESPACE, IDENTIFIER_NAME)
            b.rule(SPLIT_NESTED_TABLE_PART).define(
                NESTED, TABLE, IDENTIFIER_NAME, INTO, LPARENTHESIS,
                PARTITION, IDENTIFIER_NAME, b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                COMMA, PARTITION, IDENTIFIER_NAME, b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                b.optional(SPLIT_NESTED_TABLE_PART), RPARENTHESIS,
                b.optional(SPLIT_NESTED_TABLE_PART))

            fun indexPartitionUpdates() = b.sequence(
                IDENTIFIER_NAME, LPARENTHESIS,
                INDEX_PARTITION_DESCRIPTION,
                b.zeroOrMore(COMMA, INDEX_PARTITION_DESCRIPTION),
                RPARENTHESIS)

            b.rule(UPDATE_INDEX_CLAUSES).define(
                b.firstOf(
                    b.sequence(b.firstOf(UPDATE, "INVALIDATE"), GLOBAL, INDEXES),
                    b.sequence(UPDATE, INDEXES,
                        b.optional(LPARENTHESIS, indexPartitionUpdates(),
                            b.zeroOrMore(COMMA, indexPartitionUpdates()), RPARENTHESIS))))

            fun parallelClause() = b.firstOf(NOPARALLEL,
                b.sequence(PARALLEL, b.optional(INTEGER_LITERAL)))

            fun partitionOrKeyValue() = b.firstOf(
                b.sequence(FOR, LPARENTHESIS, b.firstOf(LITERAL, METHOD_CALL),
                    b.zeroOrMore(COMMA, b.firstOf(LITERAL, METHOD_CALL)), RPARENTHESIS),
                IDENTIFIER_NAME)

            b.rule(PARTITION_EXTENDED_NAME).define(PARTITION,
                b.firstOf(IDENTIFIER_NAME,
                    b.sequence(FOR, LPARENTHESIS, EXPRESSION,
                        b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS)))

            b.rule(SUBPARTITION_EXTENDED_NAME).define(SUBPARTITION,
                b.firstOf(IDENTIFIER_NAME,
                    b.sequence(FOR, LPARENTHESIS, EXPRESSION,
                        b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS)))

            b.rule(RENAME_PARTITION_SUBPART).define(RENAME,
                b.firstOf(PARTITION_EXTENDED_NAME, SUBPARTITION_EXTENDED_NAME),
                TO, IDENTIFIER_NAME)

            // EXCHANGE and TRUNCATE cannot specify the index partition descriptions
            // accepted by SPLIT and MERGE.
            fun restrictedIndexUpdates() = b.firstOf(
                b.sequence(b.firstOf(UPDATE, "INVALIDATE"), GLOBAL, INDEXES),
                b.sequence(UPDATE, INDEXES))

            // Oracle 26 runtime accepts CASCADE before index updates, opposite the SQLRF syntax diagram.
            b.rule(EXCHANGE_PARTITION_SUBPART).define(
                "EXCHANGE", b.firstOf(PARTITION_EXTENDED_NAME, SUBPARTITION_EXTENDED_NAME),
                WITH, TABLE, UNIT_NAME,
                b.optional(b.firstOf(INCLUDING, EXCLUDING), INDEXES),
                b.optional(b.firstOf(WITH, WITHOUT), "VALIDATION"),
                b.optional(EXCEPTIONS_CLAUSE),
                b.optional(CASCADE),
                b.optional(restrictedIndexUpdates(), b.optional(parallelClause())))

            val truncateForKeyValues = b.sequence(FOR, LPARENTHESIS, EXPRESSION,
                b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS)
            fun truncateExtendedNames(singular: PlSqlKeyword, plural: PlSqlKeyword) = b.sequence(
                b.firstOf(singular, plural),
                b.firstOf(
                    b.sequence(IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME)),
                    b.sequence(truncateForKeyValues,
                        b.zeroOrMore(COMMA, truncateForKeyValues))))

            // Oracle 26 requires CASCADE before index updates, despite the SQLRF diagram.
            b.rule(TRUNCATE_PARTITION_SUBPART).define(
                TRUNCATE,
                b.firstOf(truncateExtendedNames(PARTITION, PARTITIONS),
                    truncateExtendedNames(SUBPARTITION, SUBPARTITIONS)),
                b.optional(b.firstOf(b.sequence(DROP, b.optional(ALL)), REUSE), STORAGE),
                b.optional(CASCADE),
                b.optional(restrictedIndexUpdates(), b.optional(parallelClause())))

            b.rule(SPLIT_TABLE_PARTITION).define(
                SPLIT, PARTITION_EXTENDED_NAME,
                b.firstOf(
                    b.sequence(AT, LPARENTHESIS, EXPRESSION,
                        b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS,
                        b.optional(INTO, LPARENTHESIS, partitionSpec(), COMMA,
                            partitionSpec(), RPARENTHESIS)),
                    b.sequence(VALUES, LPARENTHESIS, splitListValues(), RPARENTHESIS,
                        b.optional(INTO, LPARENTHESIS, partitionSpec(), COMMA,
                            partitionSpec(), RPARENTHESIS)),
                    b.sequence(INTO, LPARENTHESIS,
                        b.firstOf(
                            b.sequence(rangeSplitDescription(),
                                b.zeroOrMore(COMMA, rangeSplitDescription()),
                                COMMA, partitionSpec()),
                            b.sequence(listSplitDescription(),
                                b.zeroOrMore(COMMA, listSplitDescription()),
                                COMMA, partitionSpec()),
                            b.sequence(directorySplitDescription(), COMMA,
                                directorySplitDescription())),
                        RPARENTHESIS)),
                b.optional(SPLIT_NESTED_TABLE_PART),
                b.optional(UPDATE_INDEX_CLAUSES),
                b.optional(parallelClause()),
                b.optional(ONLINE))

            fun addRangePartition() = b.sequence(PARTITION, b.optional(IDENTIFIER_NAME),
                VALUES, LESS, THAN, LPARENTHESIS,
                b.firstOf(MAXVALUE, METHOD_CALL, LITERAL),
                b.zeroOrMore(COMMA, b.firstOf(MAXVALUE, METHOD_CALL, LITERAL)),
                RPARENTHESIS, TABLE_PARTITION_DESCRIPTION, b.optional(UPDATE_INDEX_CLAUSES))

            b.rule(ADD_RANGE_TABLE_PARTITIONS).define(
                ADD, addRangePartition(), b.zeroOrMore(COMMA, addRangePartition()))

            b.rule(MERGE_TABLE_PARTITIONS).define(
                MERGE, PARTITIONS, partitionOrKeyValue(),
                b.firstOf(
                    b.sequence(COMMA, partitionOrKeyValue(),
                        b.zeroOrMore(COMMA, partitionOrKeyValue())),
                    b.sequence(TO, partitionOrKeyValue())),
                b.optional(INTO, partitionSpec()),
                b.optional(UPDATE_INDEX_CLAUSES),
                b.optional(parallelClause()),
                // Oracle 26 documents ONLINE in the merge prose although its syntax image omits it.
                b.optional(ONLINE))

            fun unusableLocalIndexesClause() = b.sequence(
                b.optional(REBUILD), UNUSABLE, LOCAL, INDEXES)

            b.rule(MODIFY_PARTITION_LOCAL_INDEXES).define(
                MODIFY, PARTITION_EXTENDED_NAME, unusableLocalIndexesClause())

            // The shared description permits repeated segment attributes; a partition MOVE
            // must not specify TABLESPACE twice, including around physical/logging attributes.
            val otherSegmentAttribute = b.firstOf(PHISICAL_ATRIBUTES_CLAUSE, LOGGING_CLAUSE)
            fun movePartitionDescription() = b.sequence(
                b.nextNot(b.sequence(b.zeroOrMore(otherSegmentAttribute),
                    TABLESPACE, IDENTIFIER_NAME, b.zeroOrMore(otherSegmentAttribute), TABLESPACE)),
                b.next(b.firstOf(SEGMENT_ATTRIBUTES_CLAUSE, TABLE_COMPRESSION,
                    KEY_COMPRESSION, OVERFLOW, LOB_STORAGE_CLAUSE,
                    VARRAY_COL_PROPERTIES, NESTED_TABLE_COL_PROPERTIES,
                    PARTITION_LEVEL_SUBPARTITION)),
                TABLE_PARTITION_DESCRIPTION)

            // Oracle 26 accepts these MOVE PARTITION suffixes in orders absent from its SQLRF diagram.
            // Track remaining families so each can occur at most once.
            val moveSuffixes = arrayOfNulls<Any>(16)
            fun movePartitionSuffixes(remaining: Int): Any {
                moveSuffixes[remaining]?.let { return it }
                fun thenRemaining(clause: Any, next: Int) =
                    if (next == 0) clause else b.sequence(clause, movePartitionSuffixes(next))

                val choices = ArrayList<Any>(4)
                if (remaining and 1 != 0) {
                    choices.add(thenRemaining(movePartitionDescription(), remaining xor 1))
                }
                if (remaining and 2 != 0) {
                    choices.add(thenRemaining(UPDATE_INDEX_CLAUSES, remaining xor 2))
                }
                if (remaining and 4 != 0) {
                    choices.add(thenRemaining(parallelClause(), remaining xor 4))
                }
                if (remaining and 8 != 0) {
                    choices.add(thenRemaining(ONLINE, remaining xor 8))
                }
                val result = b.optional(if (choices.size == 1) choices[0]
                    else b.firstOf(choices[0], choices[1], *choices.drop(2).toTypedArray()))
                moveSuffixes[remaining] = result
                return result
            }

            b.rule(MOVE_TABLE_PARTITION).define(
                MOVE, PARTITION_EXTENDED_NAME,
                b.optional(MAPPING, TABLE),
                movePartitionSuffixes(15))

            // Oracle rejects combining RENAME COLUMN with another ALTER TABLE operation (ORA-23290).
            fun renameColumnClause() = b.sequence(RENAME, COLUMN, IDENTIFIER_NAME, TO, IDENTIFIER_NAME)

            fun alterTableAction() = b.firstOf(
                            b.sequence(
                                    ADD,
                                    b.firstOf(
                                            b.sequence(LPARENTHESIS, TABLE_RELATIONAL_PROPERTIES, RPARENTHESIS),
                                            TABLE_RELATIONAL_PROPERTIES)),
                            ALTER_TABLE_MODIFY_CONSTRAINT,
                            b.sequence(
                                    MODIFY,
                                    b.firstOf(
                                            b.sequence(LPARENTHESIS, ALTER_TABLE_COLUMN,
                                                    b.zeroOrMore(COMMA, ALTER_TABLE_COLUMN), RPARENTHESIS),
                                            b.sequence(b.nextNot(b.firstOf(CONSTRAINT, PARTITION, b.sequence(PRIMARY, KEY),
                                                            b.sequence(UNIQUE, LPARENTHESIS))),
                                                    ALTER_TABLE_COLUMN,
                                                    b.next(b.firstOf(SEMICOLON, DIVISION, EOF, ENABLE_DISABLE_CLAUSE))))),
                            DROP_COLUMN_CLAUSE,
                            b.sequence(DROP, b.next(PARTITION), TABLE_RELATIONAL_PROPERTIES),
                            b.sequence(
                                    MOVE,
                                    b.optional(
                                            b.firstOf(
                                                    b.sequence(ONLINE, b.optional(TABLESPACE, IDENTIFIER_NAME)),
                                                    b.sequence(TABLESPACE, IDENTIFIER_NAME, b.optional(ONLINE))))),
                            b.sequence(b.firstOf(ENABLE, DISABLE), ROW, MOVEMENT))

            b.rule(ALTER_TABLE).define(
                    ALTER, TABLE, UNIT_NAME,
                    b.firstOf(
                            renameColumnClause(),
                            RENAME_PARTITION_SUBPART,
                            MOVE_TABLE_PARTITION,
                            TRUNCATE_PARTITION_SUBPART,
                            EXCHANGE_PARTITION_SUBPART,
                            ADD_RANGE_TABLE_PARTITIONS,
                            SPLIT_TABLE_PARTITION,
                            MERGE_TABLE_PARTITIONS,
                            MODIFY_PARTITION_LOCAL_INDEXES,
                            // alter_table_properties annotations_clause. Kept standalone: combining it with
                            // ENABLE CONSTRAINT raises ORA-00600 in Oracle 26ai, so composition is unverified.
                            ANNOTATIONS_CLAUSE,
                            b.sequence(
                                    b.oneOrMore(DROP_CONSTRAINT_CLAUSE),
                                    b.zeroOrMore(ENABLE_DISABLE_CLAUSE)),
                            b.sequence(alterTableAction(), b.zeroOrMore(ENABLE_DISABLE_CLAUSE)),
                            b.oneOrMore(ENABLE_DISABLE_CLAUSE)),
                    b.optional(SEMICOLON))

            b.rule(ALTER_SYSTEM).define(
                    ALTER, SYSTEM,
                    b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.optional(SEMICOLON))

            createLockdownProfile(b)
            createDomain(b)
            createAuditPolicy(b)
            createPropertyGraph(b)
            createUser(b)

            // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-CONTEXT.html
            b.rule(CREATE_CONTEXT).define(
                    CREATE, b.optional(OR, REPLACE), CONTEXT, UNIT_NAME,
                    USING, UNIT_NAME,
                    b.optional(b.firstOf(
                            b.sequence(INITIALIZED, b.firstOf(EXTERNALLY, GLOBALLY)),
                            b.sequence(ACCESSED, GLOBALLY))),
                    b.optional(SEMICOLON))

            b.rule(CALL_COMMAND).define(
                CALL,
                b.sequence(
                    METHOD_CALL,
                    b.zeroOrMore(DOT, METHOD_CALL)),
                b.optional(INTO, HOST_AND_INDICATOR_VARIABLE),
                b.optional(SEMICOLON))

            b.rule(COMPILE_CLAUSE).define(
                COMPILE, b.optional(DEBUG),
                b.zeroOrMore(COMPILER_PARAMETERS_CLAUSE),
                b.optional(REUSE, SETTINGS))

            // The value is not always quoted: `plsql_optimize_level=3`, `plsql_code_type = native`.
            b.rule(COMPILER_PARAMETERS_CLAUSE).define(
                    IDENTIFIER_NAME, EQUALS_OPERATOR, b.firstOf(LITERAL, IDENTIFIER_NAME))

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

            b.rule(CREATE_JAVA).define(
                CREATE,
                b.optional(OR, REPLACE),
                b.optional(AND, b.firstOf(RESOLVE, COMPILE)),
                b.optional(NOFORCE),
                JAVA,
                b.optional(IF, NOT, EXISTS),
                CREATE_JAVA_OBJECT,
                b.optional(SEMICOLON))

            b.rule(CREATE_JAVA_OBJECT).define(
                b.firstOf(CREATE_JAVA_SOURCE, CREATE_JAVA_CLASS, CREATE_JAVA_RESOURCE))

            b.rule(CREATE_JAVA_SOURCE).define(
                SOURCE,
                JAVA_NAMED_CLAUSE,
                b.optional(JAVA_SHARING_CLAUSE),
                b.optional(JAVA_AUTHID_CLAUSE),
                b.optional(JAVA_RESOLVER_CLAUSE),
                b.firstOf(
                    b.sequence(AS, JAVA_SOURCE_TEXT),
                    JAVA_USING_CLAUSE))

            b.rule(CREATE_JAVA_CLASS).define(
                CLASS,
                b.optional(JAVA_SCHEMA_CLAUSE),
                b.optional(JAVA_SHARING_CLAUSE),
                b.optional(JAVA_AUTHID_CLAUSE),
                b.optional(JAVA_RESOLVER_CLAUSE),
                JAVA_USING_CLAUSE)

            b.rule(CREATE_JAVA_RESOURCE).define(
                RESOURCE,
                JAVA_NAMED_CLAUSE,
                b.optional(JAVA_SHARING_CLAUSE),
                b.optional(JAVA_AUTHID_CLAUSE),
                b.optional(JAVA_RESOLVER_CLAUSE),
                JAVA_USING_CLAUSE)

            b.rule(JAVA_NAMED_CLAUSE).define(NAMED, UNIT_NAME)

            b.rule(JAVA_SCHEMA_CLAUSE).define(SCHEMA, IDENTIFIER_NAME)

            b.rule(JAVA_SHARING_CLAUSE).define(
                SHARING, EQUALS, b.firstOf(METADATA, NONE))

            b.rule(JAVA_AUTHID_CLAUSE).define(
                AUTHID, b.firstOf(CURRENT_USER, DEFINER))

            b.rule(JAVA_RESOLVER_CLAUSE).define(
                RESOLVER,
                LPARENTHESIS,
                b.oneOrMore(JAVA_RESOLVER_ENTRY),
                RPARENTHESIS)

            b.rule(JAVA_RESOLVER_ENTRY).define(
                LPARENTHESIS,
                JAVA_RESOLVER_MATCH_STRING,
                b.optional(COMMA),
                JAVA_RESOLVER_SCHEMA_NAME,
                RPARENTHESIS)

            b.rule(JAVA_RESOLVER_MATCH_STRING).define(JavaResolverMatchStringExpression)

            b.rule(JAVA_RESOLVER_SCHEMA_NAME).define(
                b.firstOf(IDENTIFIER_NAME, MINUS, PUBLIC))

            b.rule(JAVA_USING_CLAUSE).define(
                USING,
                b.firstOf(
                    b.sequence(BFILE, LPARENTHESIS, IDENTIFIER_NAME, COMMA, CHARACTER_LITERAL, RPARENTHESIS),
                    b.sequence(
                        b.firstOf(CLOB, BLOB, BFILE),
                        LPARENTHESIS,
                        DmlGrammar.SELECT_EXPRESSION,
                        RPARENTHESIS),
                    CHARACTER_LITERAL))

            b.rule(JAVA_SOURCE_TEXT).define(JavaSourceTextExpression)

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
                CREATE_JAVA,
                CREATE_CONTEXT,
                CREATE_DOMAIN,
                CREATE_AUDIT_POLICY,
                ALTER_AUDIT_POLICY,
                CREATE_PROPERTY_GRAPH,
                CREATE_USER,
                CALL_COMMAND,
                ALTER_SYSTEM,
                ALTER_LOCKDOWN_PROFILE,
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

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html
        // Only the single-column `AS datatype` branch; ENUM, multi-column and flexible domains are not modeled.
        private fun createDomain(b: PlSqlGrammarBuilder) {
            // Oracle 26 accepts only these states after a domain CHECK: USING INDEX, PRECHECK and
            // EXCEPTIONS INTO fail with ORA-03049.
            val domainConstraintState = b.sequence(
                b.optional(b.firstOf(
                    b.sequence(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE), b.optional(b.optional(NOT), DEFERRABLE)),
                    b.sequence(b.optional(NOT), DEFERRABLE, b.optional(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE))))),
                b.optional(b.firstOf(RELY, NORELY)),
                b.optional(b.firstOf(ENABLE, DISABLE)),
                b.optional(b.firstOf(VALIDATE, NOVALIDATE)))

            // A name is allowed only on CHECK: `CONSTRAINT c NOT NULL` fails with ORA-02253.
            b.rule(DOMAIN_CONSTRAINT).define(
                b.optional(CONSTRAINT, b.optional(IDENTIFIER_NAME)),
                CHECK, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                domainConstraintState)

            // Oracle 26 accepts these properties in any order after the datatype and STRICT, with CHECK
            // constraints repeatable. Oracle rejects a repeated singleton property (ORA-00139/ORA-02258),
            // but tracking that per property makes the compiled grammar grow factorially, so the parser
            // accepts repeats.
            val domainProperty = b.firstOf(
                DOMAIN_CONSTRAINT,
                b.sequence(
                    DEFAULT,
                    b.optional(ON, NULL, b.optional(FOR, INSERT, b.firstOf(ONLY, b.sequence(AND, UPDATE)))),
                    EXPRESSION),
                b.sequence(b.optional(NOT), NULL),
                b.sequence(VALIDATE, b.optional(CAST), b.optional(USING), CHARACTER_LITERAL),
                b.sequence(COLLATE, IDENTIFIER_NAME),
                b.sequence(DISPLAY, EXPRESSION),
                b.sequence(ORDER, EXPRESSION),
                b.withContext(CREATE_ANNOTATIONS_CONTEXT, true, ANNOTATIONS_CLAUSE))

            b.rule(CREATE_DOMAIN).define(
                CREATE, b.optional(USECASE), DOMAIN, b.optional(IF, NOT, EXISTS),
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                // Unquoted ENUM always starts the (not yet modeled) enum branch: Oracle 26 reports
                // ORA-00904 right after a bare ENUM, while "ENUM" or other names reach ORA-11531.
                AS, b.nextNot(ENUM), DATATYPE,
                // STRICT must follow the datatype immediately (ORA-03049 elsewhere).
                b.optional(STRICT),
                b.zeroOrMore(domainProperty),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
        private fun createAuditPolicy(b: PlSqlGrammarBuilder) {
            // In ALTER AUDIT POLICY, DROP is also a privilege/action word (DROP ANY TABLE), so it only
            // ends a list when it starts the DROP clause.
            val clauseStart = b.firstOf(
                PRIVILEGES, ACTIONS, ROLES, WHEN, ONLY, CONTAINER, CONDITION,
                b.sequence(DROP, b.firstOf(PRIVILEGES, ACTIONS, ROLES, ONLY)))
            val privilegeWords = b.oneOrMore(b.nextNot(clauseStart), DclGrammar.IDENTIFIER_OR_KEYWORD)
            val actionWords = b.oneOrMore(b.nextNot(b.firstOf(clauseStart, ON)), DclGrammar.IDENTIFIER_OR_KEYWORD)
            val schemaObjectName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))

            b.rule(AUDIT_PRIVILEGE_CLAUSE).define(PRIVILEGES, privilegeWords, b.zeroOrMore(COMMA, privilegeWords))

            val objectAction = b.sequence(
                actionWords,
                b.optional(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                ON,
                b.firstOf(
                    b.sequence(DIRECTORY, schemaObjectName),
                    b.sequence(MINING, MODEL, schemaObjectName),
                    schemaObjectName))
            val standardAction = b.firstOf(objectAction, actionWords)

            val firewallAction = b.sequence(
                b.firstOf(b.sequence(SQL, VIOLATION), b.sequence(CONTEXT, VIOLATION), ALL), ON, IDENTIFIER_NAME)
            val componentActions = b.sequence(
                COMPONENT, EQUALS,
                b.firstOf(
                    b.sequence(
                        b.firstOf(DATAPUMP, DIRECT_LOAD, OLS, XS),
                        actionWords, b.zeroOrMore(COMMA, actionWords)),
                    b.sequence(
                        DV,
                        actionWords, ON, IDENTIFIER_NAME,
                        b.zeroOrMore(COMMA, actionWords, ON, IDENTIFIER_NAME)),
                    b.sequence(SQL_FIREWALL, firewallAction, b.zeroOrMore(COMMA, firewallAction)),
                    b.sequence(PROTOCOL, b.firstOf(FTP, HTTP, AUTHENTICATION))))

            b.rule(AUDIT_ACTION_CLAUSE).define(
                ACTIONS,
                b.firstOf(componentActions, b.sequence(standardAction, b.zeroOrMore(COMMA, standardAction))))

            b.rule(AUDIT_ROLE_CLAUSE).define(
                ROLES, DclGrammar.IDENTIFIER_OR_KEYWORD, b.zeroOrMore(COMMA, DclGrammar.IDENTIFIER_OR_KEYWORD))

            // Oracle 26 requires at least one option and this order (ORA-46373/ORA-46383); only ACTIONS repeats.
            b.rule(CREATE_AUDIT_POLICY).define(
                CREATE, AUDIT, POLICY, IDENTIFIER_NAME,
                b.firstOf(
                    b.sequence(AUDIT_PRIVILEGE_CLAUSE, b.zeroOrMore(AUDIT_ACTION_CLAUSE), b.optional(AUDIT_ROLE_CLAUSE)),
                    b.sequence(b.oneOrMore(AUDIT_ACTION_CLAUSE), b.optional(AUDIT_ROLE_CLAUSE)),
                    AUDIT_ROLE_CLAUSE),
                b.optional(
                    WHEN, CHARACTER_LITERAL,
                    EVALUATE, PER, b.firstOf(STATEMENT_KEYWORD, SESSION, INSTANCE)),
                b.optional(ONLY, TOPLEVEL),
                b.optional(CONTAINER, EQUALS, b.firstOf(ALL, CURRENT)),
                b.optional(SEMICOLON))

            val policyChanges = b.sequence(
                b.optional(AUDIT_PRIVILEGE_CLAUSE),
                b.zeroOrMore(AUDIT_ACTION_CLAUSE),
                b.optional(AUDIT_ROLE_CLAUSE),
                b.optional(ONLY, TOPLEVEL))

            // Oracle 26 requires ADD, DROP and CONDITION in this order (ORA-46384) and rejects the
            // documented `ACTIONS ADD ...` example at ACTIONS (ORA-03049).
            b.rule(ALTER_AUDIT_POLICY).define(
                ALTER, AUDIT, POLICY, IDENTIFIER_NAME,
                b.optional(ADD, policyChanges),
                b.optional(DROP, policyChanges),
                b.optional(
                    CONDITION,
                    b.firstOf(
                        DROP,
                        b.sequence(CHARACTER_LITERAL, EVALUATE, PER, b.firstOf(STATEMENT_KEYWORD, SESSION, INSTANCE)))),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-USER.html
        private fun createUser(b: PlSqlGrammarBuilder) {
            // Oracle 26 rejects a literal password (ORA-00988) and a quoted-identifier external name
            // (ORA-28025); only the documented forms are modeled.
            b.rule(USER_AUTHENTICATION_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        IDENTIFIED,
                        b.firstOf(
                            b.sequence(
                                BY, IDENTIFIER_NAME,
                                b.optional(b.optional(HTTP), DIGEST, b.firstOf(ENABLE, DISABLE)),
                                b.optional(AND, FACTOR, CHARACTER_LITERAL, AS, CHARACTER_LITERAL)),
                            b.sequence(
                                EXTERNALLY,
                                b.optional(AS, CHARACTER_LITERAL, b.optional(WITH, THUMBPRINT, CHARACTER_LITERAL))),
                            b.sequence(GLOBALLY, b.optional(AS, CHARACTER_LITERAL)))),
                    b.sequence(NO, AUTHENTICATION)))

            // Oracle 26 accepts the options in any order. Repeats of most of them are rejected, but tracking
            // that per option makes the compiled grammar grow factorially, so the parser accepts them.
            val userOption = b.firstOf(
                USER_AUTHENTICATION_CLAUSE,
                b.sequence(DEFAULT, COLLATION, IDENTIFIER_NAME),
                b.sequence(DEFAULT, TABLESPACE, IDENTIFIER_NAME),
                b.sequence(b.optional(LOCAL), TEMPORARY, TABLESPACE, IDENTIFIER_NAME),
                b.sequence(QUOTA, b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE), ON, IDENTIFIER_NAME),
                b.sequence(PROFILE, b.firstOf(DEFAULT, IDENTIFIER_NAME)),
                b.sequence(PASSWORD, EXPIRE),
                b.sequence(ACCOUNT, b.firstOf(LOCK, UNLOCK)),
                b.sequence(ENABLE, EDITIONS),
                b.sequence(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)),
                b.sequence(READ, b.firstOf(ONLY, WRITE)))

            b.rule(CREATE_USER).define(
                CREATE, USER, b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME,
                b.zeroOrMore(userOption),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-property-graph.html
        private fun createPropertyGraph(b: PlSqlGrammarBuilder) {
            val schemaObjectName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val columnList = b.sequence(
                LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)
            val elementNameAndKey = b.sequence(
                schemaObjectName, b.optional(AS, IDENTIFIER_NAME), b.optional(KEY, columnList))

            // An expression needs AS (ORA-42424 before trailing tokens are read).
            val property = b.firstOf(b.sequence(EXPRESSION, AS, IDENTIFIER_NAME), IDENTIFIER_NAME)
            b.rule(PROPERTY_GRAPH_PROPERTIES).define(
                b.firstOf(
                    b.sequence(NO, PROPERTIES),
                    b.sequence(
                        PROPERTIES,
                        b.firstOf(
                            b.sequence(b.optional(ARE), ALL, COLUMNS, b.optional(EXCEPT, columnList)),
                            b.sequence(LPARENTHESIS, property, b.zeroOrMore(COMMA, property), RPARENTHESIS)))))

            // Only one properties clause may belong to the default label (ORA-42408 for a second one).
            val label = b.sequence(
                b.firstOf(b.sequence(PlSqlKeyword.LABEL, IDENTIFIER_NAME), b.sequence(DEFAULT, PlSqlKeyword.LABEL)),
                b.optional(PROPERTY_GRAPH_PROPERTIES))
            val labelsAndProperties = b.sequence(
                b.zeroOrMore(label), b.optional(PROPERTY_GRAPH_PROPERTIES), b.zeroOrMore(label))

            b.rule(PROPERTY_GRAPH_VERTEX_TABLE).define(elementNameAndKey, labelsAndProperties)

            val vertexReference = b.firstOf(
                b.sequence(KEY, columnList, REFERENCES, IDENTIFIER_NAME, columnList),
                IDENTIFIER_NAME)
            b.rule(PROPERTY_GRAPH_EDGE_TABLE).define(
                elementNameAndKey,
                SOURCE, vertexReference,
                DESTINATION, vertexReference,
                labelsAndProperties)

            val graphOption = b.firstOf(
                b.sequence(b.firstOf(ENFORCED, TRUSTED), MODE),
                b.sequence(b.firstOf(ALLOW, DISALLOW), MIXED, PROPERTY, TYPES))

            b.rule(CREATE_PROPERTY_GRAPH).define(
                CREATE, b.optional(OR, REPLACE), PROPERTY, GRAPH, b.optional(IF, NOT, EXISTS),
                schemaObjectName,
                VERTEX, TABLES, LPARENTHESIS,
                PROPERTY_GRAPH_VERTEX_TABLE, b.zeroOrMore(COMMA, PROPERTY_GRAPH_VERTEX_TABLE),
                RPARENTHESIS,
                b.optional(
                    EDGE, TABLES, LPARENTHESIS,
                    PROPERTY_GRAPH_EDGE_TABLE, b.zeroOrMore(COMMA, PROPERTY_GRAPH_EDGE_TABLE),
                    RPARENTHESIS),
                b.optional(OPTIONS, LPARENTHESIS, graphOption, b.zeroOrMore(COMMA, graphOption), RPARENTHESIS),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-LOCKDOWN-PROFILE.html
        private fun createLockdownProfile(b: PlSqlGrammarBuilder) {
            // Values are opaque string literals: Oracle rejects `= (NAME)` and `= ()` (ORA-01780).
            fun quotedList() = b.sequence(
                EQUALS, LPARENTHESIS, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL), RPARENTHESIS)

            // Oracle 26 rejects the root USERS suffix after any `ALL EXCEPT = (...)` (ORA-00922 at USERS),
            // although it accepts it after a bare ALL or a plain list.
            fun allExcept() = b.sequence(ALL, b.optional(EXCEPT, quotedList(), b.nextNot(USERS)))

            // Only a single selected value may be refined further; Oracle 26 rejects a refinement
            // after a list (ORA-00922) and after ALL. Without a refinement this is `= (...) | ALL ...`.
            fun singleOrList(refinement: Any? = null): Any =
                if (refinement == null) b.firstOf(quotedList(), allExcept())
                else b.firstOf(
                    allExcept(),
                    b.sequence(
                        EQUALS, LPARENTHESIS, CHARACTER_LITERAL,
                        b.firstOf(
                            b.sequence(b.oneOrMore(COMMA, CHARACTER_LITERAL), RPARENTHESIS),
                            b.sequence(RPARENTHESIS, b.optional(refinement)))))

            fun clauseOptions(optionValues: Any?) = b.sequence(OPTION, singleOrList(optionValues))

            fun statementClauses(optionValues: Any?) = b.sequence(CLAUSE, singleOrList(clauseOptions(optionValues)))

            b.rule(ALTER_LOCKDOWN_PROFILE).define(
                ALTER, LOCKDOWN, PROFILE, IDENTIFIER_NAME,
                b.firstOf(LOCKDOWN_FEATURES, LOCKDOWN_OPTIONS, LOCKDOWN_STATEMENTS),
                b.optional(USERS, EQUALS, b.firstOf(ALL, COMMON, LOCAL)),
                b.optional(SEMICOLON))

            b.rule(LOCKDOWN_FEATURES).define(b.firstOf(DISABLE, ENABLE), FEATURE, singleOrList())

            b.rule(LOCKDOWN_OPTIONS).define(b.firstOf(DISABLE, ENABLE), OPTION, singleOrList())

            // Option values exist only under DISABLE: Oracle 26 rejects them after ENABLE at MINVALUE,
            // before trailing tokens (ORA-00922).
            b.rule(LOCKDOWN_STATEMENTS).define(
                b.firstOf(
                    b.sequence(DISABLE, STATEMENT_KEYWORD, singleOrList(statementClauses(LOCKDOWN_OPTION_VALUES))),
                    b.sequence(ENABLE, STATEMENT_KEYWORD, singleOrList(statementClauses(null)))))

            // VALUE, MINVALUE and MAXVALUE may appear in any order, each at most once (a repeated
            // one fails with ORA-00922 in Oracle 26).
            val valueClauses = arrayOf<Any>(
                b.sequence(VALUE, quotedList()),
                b.sequence(MINVALUE, EQUALS, CHARACTER_LITERAL),
                b.sequence(MAXVALUE, EQUALS, CHARACTER_LITERAL))
            // Oracle 26 also rejects USERS once MINVALUE or MAXVALUE was given (VALUE alone allows it).
            fun valuesEnd(remaining: Int): Any? = if ((7 xor remaining) and 6 != 0) b.nextNot(USERS) else null
            val optionValues = arrayOfNulls<Any>(8)
            fun optionValues(remaining: Int): Any {
                optionValues[remaining]?.let { return it }
                val choices = valueClauses.indices.filter { remaining and (1 shl it) != 0 }.map { index ->
                    val next = remaining xor (1 shl index)
                    val end = valuesEnd(next)
                    val rest = when {
                        next == 0 -> end
                        end == null -> b.optional(optionValues(next))
                        else -> b.firstOf(optionValues(next), end)
                    }
                    if (rest == null) valueClauses[index] else b.sequence(valueClauses[index], rest)
                }
                val result = if (choices.size == 1) choices[0]
                    else b.firstOf(choices[0], choices[1], *choices.drop(2).toTypedArray())
                optionValues[remaining] = result
                return result
            }

            b.rule(LOCKDOWN_OPTION_VALUES).define(optionValues(7))
        }
    }

}
