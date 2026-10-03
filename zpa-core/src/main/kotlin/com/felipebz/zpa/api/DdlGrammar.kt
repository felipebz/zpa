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
import com.felipebz.flr.api.GenericTokenType.IDENTIFIER
import com.felipebz.flr.grammar.ContextKey
import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.zpa.api.PlSqlGrammar.*
import com.felipebz.zpa.api.PlSqlKeyword.*
import com.felipebz.zpa.api.PlSqlPunctuator.*
import com.felipebz.zpa.api.PlSqlTokenType.INTEGER_LITERAL
import com.felipebz.zpa.api.PlSqlTokenType.NUMBER_LITERAL
import com.felipebz.zpa.grammar.JavaSourceTextExpression
import com.felipebz.zpa.grammar.JavaResolverMatchStringExpression
import com.felipebz.zpa.sslr.PlSqlGrammarBuilder

/**
 * Set while parsing CREATE TABLE and CREATE DOMAIN. Oracle 26 rejects DROP, REPLACE and ADD OR REPLACE
 * annotation directives in both at parse time (ORA-11555/ORA-11556 at the directive), while
 * ALTER TABLE, including ALTER TABLE ADD column, accepts them.
 */
internal val CREATE_ANNOTATIONS_CONTEXT: ContextKey<Boolean> = ContextKey()
internal val OUTLINE_CREATE_TABLE_CONTEXT: ContextKey<Boolean> = ContextKey()
internal val PRIVATE_TEMPORARY_TABLE_CONTEXT: ContextKey<Boolean> = ContextKey()
/**
 * Enables view-specific restrictions in shared constraint productions.
 * Callers remain responsible for restricting unsupported constraint kinds.
 */
internal val VIEW_CONSTRAINT_CONTEXT: ContextKey<Boolean> = ContextKey()

enum class DdlGrammar : GrammarRuleKey {

    DDL_COMMENT,
    DDL_COMMAND,
    ONE_OR_MORE_IDENTIFIERS,
    REFERENCES_CLAUSE,
    INLINE_CONSTRAINT,
    INLINE_REF_CONSTRAINT,
    OUT_OF_LINE_CONSTRAINT,
    OUT_OF_LINE_REF_CONSTRAINT,
    USING_INDEX_CLAUSE,
    ANNOTATIONS_CLAUSE,
    ANNOTATION,
    TABLE_COLUMN_DEFINITION,
    VIRTUAL_COLUMN_DEFINITION,
    JSON_COLLECTION_COLUMNS,
    CTAS_COLUMN_DEFINITION,
    CTAS_RELATIONAL_PROPERTIES,
    CREATE_TABLE_RELATIONAL_TAIL,
    SUPPLEMENTAL_LOGGING_PROPS,
    SUPPLEMENTAL_ID_KEY_CLAUSE,
    TABLE_RELATIONAL_PROPERTIES,
    OBJECT_TABLE_CLAUSE,
    XMLTYPE_TABLE,
    XMLTYPE_COLUMN_PROPERTIES,
    XMLTYPE_STORAGE,
    XMLSCHEMA_SPEC,
    XMLTYPE_VIRTUAL_COLUMNS,
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
    CREATE_LOCKDOWN_PROFILE,
    LOCKDOWN_FEATURES,
    LOCKDOWN_OPTIONS,
    LOCKDOWN_STATEMENTS,
    LOCKDOWN_OPTION_VALUES,
    CREATE_DOMAIN,
    DOMAIN_CONSTRAINT,
    DOMAIN_COLUMN,
    DOMAIN_ENUM,
    CREATE_FLEXIBLE_DOMAIN,
    CREATE_MATERIALIZED_ZONEMAP,
    ALTER_MATERIALIZED_ZONEMAP,
    ZONEMAP_REFRESH_CLAUSE,
    CREATE_ATTRIBUTE_DIMENSION,
    ATTRIBUTE_DIMENSION_LEVEL_CLAUSE,
    CREATE_HIERARCHY,
    AV_CLASSIFICATION_CLAUSE,
    CREATE_DIMENSION,
    DIMENSION_LEVEL_CLAUSE,
    DIMENSION_HIERARCHY_CLAUSE,
    DIMENSION_ATTRIBUTE_CLAUSE,
    ALTER_DIMENSION,
    ALTER_ATTRIBUTE_DIMENSION,
    ALTER_HIERARCHY,
    ALTER_ANALYTIC_VIEW,
    CREATE_DATABASE_LINK,
    ALTER_DATABASE_LINK,
    DATABASE_LINK_NAME,
    CREATE_OUTLINE,
    ALTER_OUTLINE,
    CREATE_INMEMORY_JOIN_GROUP,
    ALTER_INMEMORY_JOIN_GROUP,
    CREATE_MLE_ENV,
    ALTER_MLE_ENV,
    CREATE_MLE_MODULE,
    ALTER_MLE_MODULE,
    MLE_MODULE_USING_CLAUSE,
    ALTER_VIEW,
    CREATE_FLASHBACK_ARCHIVE,
    ALTER_FLASHBACK_ARCHIVE,
    PURGE_STATEMENT,
    CREATE_PFILE,
    CREATE_RESTORE_POINT,
    FLASHBACK_TABLE,
    CREATE_EDITION,
    CREATE_OPERATOR,
    ALTER_OPERATOR,
    CREATE_INDEXTYPE,
    ALTER_INDEXTYPE,
    CREATE_SPFILE,
    ALTER_DOMAIN,
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
    ALTER_USER,
    CREATE_PROFILE,
    ALTER_PROFILE,
    PROFILE_LIMIT_CLAUSE,
    CREATE_TABLESPACE,
    ALTER_TABLESPACE,
    CREATE_TABLESPACE_SET,
    ALTER_TABLESPACE_SET,
    DROP_TABLESPACE,
    DROP_TABLESPACE_SET,
    TABLESPACE_DROP_OPTIONS,
    TABLESPACE_SET_TEMPLATE,
    TABLESPACE_SET_FILE_SPEC,
    TABLESPACE_PERMANENT_ATTRS_COMMON,
    ALTER_TABLESPACE_ATTRS_COMMON,
    ALTER_TABLESPACE_ENCRYPTION,
    TABLESPACE_STORAGE_CLAUSE,
    TABLESPACE_MEMCOMPRESS,
    TABLESPACE_INMEMORY_ATTRIBUTES,
    TABLESPACE_INMEMORY_CLAUSE,
    TABLESPACE_INMEMORY_TEXT_COLUMN,
    TABLESPACE_ILM_CONDITION,
    TABLESPACE_ILM_POLICY,
    TABLESPACE_ILM_CLAUSE,
    CREATE_ROLE,
    ALTER_ROLE,
    ROLE_IDENTIFICATION_CLAUSE,
    CREATE_ROLLBACK_SEGMENT,
    ALTER_ROLLBACK_SEGMENT,
    CREATE_CLUSTER,
    ALTER_CLUSTER,
    TABLE_CLUSTER_CLAUSE,
    ANALYZE_STATEMENT,
    AUDIT_STATEMENT,
    NOAUDIT_STATEMENT,
    AUDIT_POLICY_CLAUSE,
    AUDIT_CONTEXT_CLAUSE,
    STATISTICS_ASSOCIATION_TARGET,
    ASSOCIATE_STATISTICS,
    DISASSOCIATE_STATISTICS,
    RENAME_STATEMENT,
    ALTER_RESOURCE_COST,
    ADMINISTER_KEY_MANAGEMENT,
    OPEN_KEYSTORE,
    CLOSE_KEYSTORE,
    KEYSTORE_IDENTIFIED_BY,
    KEYSTORE_CONTAINER_CLAUSE,
    FORCE_KEYSTORE,
    KEYSTORE_WITH_BACKUP,
    KEYSTORE_BACKUP_IDENTIFIER,
    KEYSTORE_PASSWORD_IDENTIFIED_BY,
    CREATE_KEYSTORE,
    BACKUP_KEYSTORE,
    ALTER_KEYSTORE_PASSWORD,
    MERGE_KEYSTORE_SOURCE,
    MERGE_INTO_NEW_KEYSTORE,
    MERGE_INTO_EXISTING_KEYSTORE,
    SECRET_KEYSTORE_TARGET,
    SECRET_MANAGEMENT_CLAUSES,
    ADD_UPDATE_SECRET,
    DELETE_SECRET,
    KEY_MANAGEMENT_CLAUSES,
    SET_KEY,
    CREATE_KEY,
    USE_KEY,
    SET_KEY_TAG,
    EXPORT_KEYS,
    IMPORT_KEYS,
    MIGRATE_KEY,
    REVERSE_MIGRATE_KEY,
    ALTER_PLUGGABLE_DATABASE,
    PREPARE_CLAUSE,
    DROP_MIRROR_COPY,
    CREATE_PLUGGABLE_DATABASE,
    PDB_FROM_SEED,
    PDB_CLONE,
    PDB_FROM_XML,
    PDB_FILE_NAME_CONVERT,
    PDB_SERVICE_NAME_CONVERT,
    PDB_SOURCE_FILE_NAME_CONVERT,
    PDB_SOURCE_FILE_DIRECTORY,
    PDB_PATH_PREFIX,
    PDB_TEMPFILE_REUSE,
    PDB_STANDBYS,
    PDB_USER_TABLESPACES,
    PDB_LOGGING,
    PDB_CREATE_FILE_DEST,
    PDB_STORAGE_CLAUSE,
    PDB_DEFAULT_TABLESPACE,
    PDB_DEFAULT_TABLESPACE_FILES,
    PDB_ROLES_CLAUSE,
    PDB_KEYSTORE_CLAUSE,
    PDB_REFRESH_MODE_CLAUSE,
    PDB_RELOCATE_CLAUSE,
    PDB_USING_SNAPSHOT,
    PDB_DECRYPT_CLAUSE,
    PDB_APPLICATION_SYNC_CLAUSE,
    PDB_CONTAINERS_CLAUSE,
    PDB_UNPLUG_CLAUSE,
    PDB_CHANGE_STATE,
    PDB_SAVE_OR_DISCARD_STATE,
    PDB_OPEN,
    PDB_CLOSE,
    PDB_INSTANCES_CLAUSE,
    ALTER_DATABASE,
    ALTER_DISKGROUP,
    QUALIFIED_DISK_CLAUSE,
    ADD_DISK_CLAUSE,
    DROP_DISK_CLAUSE,
    UNDROP_DISK_CLAUSE,
    RESIZE_DISK_CLAUSE,
    REBALANCE_DISKGROUP_CLAUSE,
    DISKGROUP_AVAILABILITY,
    CHECK_DISKGROUP_CLAUSE,
    DISKGROUP_TEMPLATE_CLAUSES,
    DISKGROUP_DIRECTORY_CLAUSES,
    DISKGROUP_ALIAS_CLAUSES,
    DISKGROUP_FILEGROUP_CLAUSE,
    ADD_FILEGROUP_CLAUSE,
    MODIFY_FILEGROUP_CLAUSE,
    MOVE_TO_FILEGROUP_CLAUSE,
    DROP_FILEGROUP_CLAUSE,
    FILEGROUP_PROPERTY,
    SCRUB_CLAUSE,
    RENAME_GLOBAL_NAME_CLAUSE,
    BLOCK_CHANGE_TRACKING_CLAUSE,
    GENERAL_RECOVERY,
    MANAGED_STANDBY_RECOVERY,
    RECOVER_TO_LOGICAL_STANDBY,
    STARTUP_CLAUSES,
    DEFAULT_TABLESPACE_SETTINGS,
    DATABASE_FILE_CLAUSES,
    CREATE_DATAFILE_CLAUSE,
    ALTER_DATAFILE_CLAUSE,
    ALTER_TEMPFILE_CLAUSE,
    MOVE_DATAFILE_CLAUSE,
    LOST_WRITE_PROTECTION,
    LOGFILE_CLAUSES,
    ADD_LOGFILE_CLAUSES,
    DROP_LOGFILE_CLAUSES,
    REDO_LOG_FILE_SPEC,
    LOGFILE_DESCRIPTOR,
    SUPPLEMENTAL_DB_LOGGING,
    CREATE_ASSERTION,
    ASSERTION_CONDITION,
    ASSERTION_UNIVERSAL_EXPRESSION,
    DATAFILE_TEMPFILE_SPEC,
    AUTOEXTEND_CLAUSE,
    EXTENT_MANAGEMENT_CLAUSE,
    TABLESPACE_ENCRYPTION_CLAUSE,
    DEFAULT_TABLESPACE_PARAMS,
    USER_PROXY_CLAUSE,
    CREATE_CONTEXT,
    CALL_COMMAND,
    CREATE_TABLE,
    INDEX_ORGANIZED_TABLE_CLAUSE,
    INDEX_ORGANIZED_TABLE_OVERFLOW_CLAUSE,
    CREATE_INDEX,
    CREATE_SEARCH_INDEX,
    CREATE_VECTOR_INDEX,
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
    TYPE_COMPILE_CLAUSE,
    DROP_COMMAND,
    CREATE_SYNONYM,
    CREATE_JAVA,
    ALTER_JAVA,
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
    ALTER_SEQUENCE,
    ALTER_SYNONYM,
    TRUNCATE_CLUSTER,
    CREATE_LIBRARY,
    ALTER_LIBRARY,
    PARTITION_BY_RANGE,
    PARTITION_BY_HASH,
    RANGE_VALUES_CLAUSE,
    TABLE_PARTITION_DESCRIPTION,
    SEGMENT_ATTRIBUTES_CLAUSE,
    PHYSICAL_ATRIBUTES_CLAUSE,
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
    LOGGING_CLAUSE,
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
            // Oracle 26 still accepts this among CREATE INDEX attributes and unnamed USING INDEX properties.
            val computeStatistics = b.sequence(COMPUTE, STATISTICS)

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
                ANNOTATIONS_CLAUSE,
                computeStatistics
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
                b.sequence(IDENTIFIER_NAME, b.zeroOrMore(b.firstOf(INLINE_REF_CONSTRAINT, INLINE_CONSTRAINT)))
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
                        VARRAY_COL_PROPERTIES,
                        XMLTYPE_COLUMN_PROPERTIES))
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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html
            // Table constraint column lists name columns or object attributes. The diagram shows plain columns,
            // but Oracle 26 parses dotted attribute paths of any depth in UNIQUE, PRIMARY KEY, FOREIGN KEY and
            // REFERENCES lists, resolving the components later (ORA-22809/ORA-00904/ORA-02337); empty or trailing
            // components are rejected (ORA-03050/ORA-00936). Each column or path may also carry a database link,
            // which Oracle 26 accepts and ignores when it records the constraint column. The link follows the
            // dblink syntax `database[.domain…][@connection_qualifier]`, and the database may be omitted to give
            // `column@@qualifier`. The qualifier is one undotted name, and a further `@` fails (ORA-02083); an
            // empty link or qualifier fails too (ORA-01729/ORA-02084). View constraints keep plain column names.
            val constraintColumn = b.sequence(
                IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME),
                b.optional(REMOTE, b.firstOf(
                    b.sequence(IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME), b.optional(REMOTE, IDENTIFIER_NAME)),
                    b.sequence(REMOTE, IDENTIFIER_NAME))))
            val constraintColumns = b.firstOf(
                b.sequence(b.requireContext(VIEW_CONSTRAINT_CONTEXT, true), ONE_OR_MORE_IDENTIFIERS),
                b.sequence(
                    b.nextNot(b.requireContext(VIEW_CONSTRAINT_CONTEXT, true)),
                    LPARENTHESIS, constraintColumn, b.zeroOrMore(COMMA, constraintColumn), RPARENTHESIS))

            b.rule(REFERENCES_CLAUSE).define(
                    REFERENCES, MEMBER_EXPRESSION,
                    b.optional(constraintColumns),
                    b.optional(b.nextNot(b.requireContext(VIEW_CONSTRAINT_CONTEXT, true)),
                        ON, DELETE, b.firstOf(CASCADE, b.sequence(SET, NULL)))
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
                        b.sequence(b.nextNot(b.firstOf(COMPUTE, STATISTICS)), b.nextNot(b.sequence(EXCEPTIONS, INTO)), UNIT_NAME)
                    )
                )
            )

            b.rule(PRECHECK_STATE).define(b.firstOf(PRECHECK, NOPRECHECK))

            b.rule(EXCEPTIONS_CLAUSE).define(EXCEPTIONS, INTO, UNIT_NAME)

            // Unlike CREATE SEQUENCE: no SESSION/GLOBAL, bare RESTART, and LIMIT VALUE only in MODIFY.
            // The lexer reads 1d/1f as numbers, which Oracle rejects; not distinguished here.
            val identityNumber = b.sequence(b.optional(b.firstOf(PLUS, MINUS)), NUMERIC_LITERAL)
            fun identityClause(limitValue: Boolean): Any {
                val startValue = if (limitValue) b.firstOf(identityNumber, b.sequence(LIMIT, VALUE)) else identityNumber
                val identityOption = b.firstOf(
                    b.sequence(START, WITH, startValue),
                    b.sequence(INCREMENT, BY, identityNumber),
                    b.sequence(MAXVALUE, identityNumber),
                    NOMAXVALUE,
                    b.sequence(MINVALUE, identityNumber),
                    NOMINVALUE,
                    CYCLE,
                    NOCYCLE,
                    b.sequence(CACHE, identityNumber),
                    NOCACHE,
                    ORDER,
                    NOORDER,
                    KEEP,
                    NOKEEP,
                    b.sequence(SCALE, b.optional(b.firstOf(EXTEND, NOEXTEND))),
                    NOSCALE,
                    RESTART)
                return b.sequence(
                    GENERATED,
                    b.optional(b.firstOf(
                        ALWAYS,
                        b.sequence(BY, DEFAULT, b.optional(ON, NULL,
                            b.optional(FOR, INSERT, b.firstOf(ONLY, b.sequence(AND, UPDATE))))))),
                    AS, IDENTITY,
                    b.optional(b.firstOf(
                        b.sequence(LPARENTHESIS, b.zeroOrMore(identityOption), RPARENTHESIS),
                        b.oneOrMore(identityOption))))
            }
            val identityStart = b.sequence(GENERATED, b.firstOf(ALWAYS, BY, AS))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/constraint.html
            // inline_ref_constraint without its references_clause branch, which INLINE_CONSTRAINT already
            // covers. Oracle 26 parses these on any column and mixed with other inline constraints in any
            // order or repetition; non-REF columns, duplicate SCOPE and SCOPE with REFERENCES fail later
            // (ORA-22893/ORA-22888/ORA-22896). A constraint name (ORA-22890) or constraint state (ORA-03076)
            // is rejected. The scope table also parses a database link, rejected later (ORA-25124).
            b.rule(INLINE_REF_CONSTRAINT).define(
                b.firstOf(
                    b.sequence(SCOPE, IS, DmlGrammar.TABLE_REFERENCE),
                    b.sequence(WITH, ROWID)))

            fun columnValueAndConstraints() = b.sequence(
                    b.optional(b.firstOf(
                        b.sequence(DEFAULT, b.optional(
                            b.sequence(ON, NULL,
                                b.optional(FOR, INSERT,
                                    b.firstOf(ONLY, b.sequence(AND, UPDATE))))), EXPRESSION),
                        identityClause(false))),
                    b.optional(columnEncryptionClause()),
                    b.zeroOrMore(b.firstOf(INLINE_REF_CONSTRAINT, INLINE_CONSTRAINT)),
                    // Oracle 26 accepts annotations only after DEFAULT, encryption and inline constraints.
                    b.optional(ANNOTATIONS_CLAUSE))

            b.rule(TABLE_COLUMN_DEFINITION).define(
                    IDENTIFIER_NAME,
                    // Oracle rejects a missing identity datatype only after parsing (ORA-02263).
                    b.firstOf(
                        b.sequence(b.nextNot(identityStart), DATATYPE),
                        b.next(identityStart)),
                    b.optional(SORT),
                    columnValueAndConstraints(),
                    b.nextNot(b.firstOf(
                        AS, VISIBLE, INVISIBLE, b.sequence(GENERATED, ALWAYS, AS, LPARENTHESIS))))

            // Oracle also accepts STORED for MATERIALIZED.
            val editionName = b.sequence(EDITION, IDENTIFIER_NAME)
            b.rule(VIRTUAL_COLUMN_DEFINITION).define(
                    IDENTIFIER_NAME,
                    b.optional(b.nextNot(b.firstOf(VISIBLE, INVISIBLE, GENERATED)), DATATYPE, b.optional(COLLATE, IDENTIFIER_NAME)),
                    b.optional(b.firstOf(VISIBLE, INVISIBLE)),
                    b.optional(GENERATED, ALWAYS),
                    AS, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                    b.optional(b.firstOf(VIRTUAL, MATERIALIZED, STORED)),
                    b.optional(EVALUATE, USING, b.firstOf(b.sequence(CURRENT, EDITION), editionName, b.sequence(NULL, EDITION))),
                    b.optional(UNUSABLE, BEFORE, b.firstOf(b.sequence(CURRENT, EDITION), editionName)),
                    b.optional(UNUSABLE, BEGINNING, WITH, b.firstOf(b.sequence(CURRENT, EDITION), editionName, b.sequence(NULL, EDITION))),
                    b.zeroOrMore(INLINE_CONSTRAINT))

            b.rule(JSON_COLLECTION_COLUMNS).define(
                    LPARENTHESIS,
                    b.firstOf(OUT_OF_LINE_CONSTRAINT, VIRTUAL_COLUMN_DEFINITION),
                    b.zeroOrMore(COMMA, b.firstOf(OUT_OF_LINE_CONSTRAINT, VIRTUAL_COLUMN_DEFINITION)),
                    RPARENTHESIS)

            // CREATE TABLE ... AS subquery infers datatypes; keep this out of ordinary columns and ALTER.
            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html
            // Oracle 26 also accepts repeated SORT and SORT after visibility. Column-count, identity,
            // annotation, foreign-key, REF and reservable-column restrictions are database validation,
            // not additional syntax restrictions here (ORA-01730/01773/11559/02440/22893/55773).
            val visibility = b.firstOf(VISIBLE, INVISIBLE)
            b.rule(CTAS_COLUMN_DEFINITION).define(
                    IDENTIFIER_NAME,
                    b.zeroOrMore(SORT),
                    b.optional(b.firstOf(
                        b.sequence(visibility, b.zeroOrMore(SORT), b.optional(RESERVABLE)),
                        b.sequence(RESERVABLE, b.zeroOrMore(SORT), b.optional(visibility)))),
                    b.zeroOrMore(SORT),
                    columnValueAndConstraints())

            // View constraints in this context only permit
            // [RELY | NORELY] DISABLE [NOVALIDATE] after the column list.
            val viewConstraintState = b.sequence(b.optional(b.firstOf(RELY, NORELY)), DISABLE, b.optional(NOVALIDATE))
            fun outOfLineConstraintState(normalState: Any) = b.firstOf(
                b.sequence(b.requireContext(VIEW_CONSTRAINT_CONTEXT, true), viewConstraintState),
                b.sequence(b.nextNot(b.requireContext(VIEW_CONSTRAINT_CONTEXT, true)), normalState))

            b.rule(OUT_OF_LINE_CONSTRAINT).define(
                b.optional(b.firstOf(CONSTRAINT, CONSTRAINTS), IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(
                        b.firstOf(
                            b.sequence(UNIQUE, constraintColumns),
                            b.sequence(PRIMARY, KEY, constraintColumns),
                        ), outOfLineConstraintState(b.optional(CONSTRAINT_STATE))
                    ),
                    b.sequence(
                        FOREIGN, KEY, constraintColumns, REFERENCES_CLAUSE,
                        outOfLineConstraintState(b.optional(CONSTRAINT_STATE_WITHOUT_USING_INDEX))
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
            val supplementalLogKey = b.firstOf(
                ALL, b.sequence(PRIMARY, KEY), UNIQUE, b.sequence(FOREIGN, KEY))
            val supplementalLogColumn = b.sequence(IDENTIFIER_NAME, b.optional(NO, LOG))
            val supplementalLogGroup = b.sequence(
                GROUP, IDENTIFIER_NAME, LPARENTHESIS, supplementalLogColumn,
                b.zeroOrMore(COMMA, supplementalLogColumn), RPARENTHESIS, b.optional(ALWAYS))
            b.rule(SUPPLEMENTAL_ID_KEY_CLAUSE).define(
                DATA, LPARENTHESIS, supplementalLogKey,
                b.zeroOrMore(COMMA, supplementalLogKey), RPARENTHESIS, COLUMNS).skip()
            b.rule(SUPPLEMENTAL_LOGGING_PROPS).define(
                SUPPLEMENTAL, LOG, b.firstOf(supplementalLogGroup, SUPPLEMENTAL_ID_KEY_CLAUSE))

            val relationalProperty = b.firstOf(
                OUT_OF_LINE_REF_CONSTRAINT,
                OUT_OF_LINE_CONSTRAINT,
                b.sequence(
                    b.nextNot(b.firstOf(
                        b.sequence(SCOPE, FOR), b.sequence(REF, LPARENTHESIS), b.sequence(SUPPLEMENTAL, LOG))),
                    TABLE_COLUMN_DEFINITION),
                VIRTUAL_COLUMN_DEFINITION)
            b.rule(TABLE_RELATIONAL_PROPERTIES).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(
                        relationalProperty,
                        b.firstOf(COMMA, b.nextNot(b.sequence(SUPPLEMENTAL, LOG)))),
                    b.sequence(
                        SUPPLEMENTAL_LOGGING_PROPS,
                        b.firstOf(
                            b.sequence(COMMA, b.nextNot(RPARENTHESIS)),
                            b.next(RPARENTHESIS))))))

            val ctasNonColumnProperty = b.firstOf(
                    OUT_OF_LINE_REF_CONSTRAINT, OUT_OF_LINE_CONSTRAINT, SUPPLEMENTAL_LOGGING_PROPS)
            val ctasColumn = b.sequence(
                    b.nextNot(b.firstOf(
                        b.sequence(SCOPE, FOR), b.sequence(REF, LPARENTHESIS), b.sequence(SUPPLEMENTAL, LOG))),
                    CTAS_COLUMN_DEFINITION)
            b.rule(CTAS_RELATIONAL_PROPERTIES).define(
                    b.zeroOrMore(ctasNonColumnProperty, COMMA),
                    ctasColumn,
                    b.zeroOrMore(COMMA, b.firstOf(ctasNonColumnProperty, ctasColumn)))

            // Discriminate at the first property, not by parsing a column list/tail to look for a distant AS.
            // Constraint-first lists enter CTAS, but a typed first column fails locally, before the table tail.
            val ctasPropertiesStart = b.sequence(
                    LPARENTHESIS,
                    b.firstOf(
                        b.firstOf(CONSTRAINT, CONSTRAINTS, PRIMARY, UNIQUE, FOREIGN, CHECK),
                        b.sequence(SCOPE, FOR), b.sequence(REF, LPARENTHESIS), b.sequence(SUPPLEMENTAL, LOG),
                        b.sequence(IDENTIFIER_NAME, b.firstOf(
                            COMMA, RPARENTHESIS, SORT, visibility, RESERVABLE, DEFAULT, identityStart, ENCRYPT,
                            CONSTRAINT, CONSTRAINTS, NOT, NULL, UNIQUE, PRIMARY, CHECK, REFERENCES,
                            b.sequence(SCOPE, IS), b.sequence(WITH, ROWID), b.sequence(ANNOTATIONS, LPARENTHESIS)))))
            b.rule(OBJECT_TABLE_PROPERTIES).define(
                LPARENTHESIS,
                objectTableProperty(),
                b.zeroOrMore(COMMA, objectTableProperty()),
                RPARENTHESIS
            )

            b.rule(OBJECT_IDENTIFIER_CLAUSE).define(
                OBJECT,
                IDENTIFIER_KEYWORD,
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

            // MAXTRANS is deprecated, but Oracle 26 still parses it for tables and indexes.
            b.rule(PHYSICAL_ATRIBUTES_CLAUSE).define(
                    b.oneOrMore(b.firstOf(
                            b.sequence(PCTFREE, INTEGER_LITERAL),
                            b.sequence(PCTUSED, INTEGER_LITERAL),
                            b.sequence(INITRANS, INTEGER_LITERAL),
                            b.sequence(MAXTRANS, INTEGER_LITERAL),
                            INDEX_STORAGE_CLAUSE)))

            b.rule(SEGMENT_ATTRIBUTES_CLAUSE).define(
                    b.oneOrMore(b.firstOf(
                            PHYSICAL_ATRIBUTES_CLAUSE,
                            b.sequence(TABLESPACE, IDENTIFIER_NAME),
                            LOGGING_CLAUSE)))

            // Oracle 26 also parses the legacy COMPRESS FOR forms, COMPRESS BASIC (but not ADVANCED), a bare
            // ROW STORE and a bare COLUMN STORE; FOR must be followed by a compression type (ORA-14463/ORA-14464).
            // A store clause never splits into two properties: COMPRESS that does not fit its form, NOCOMPRESS or
            // ROW STORE after COLUMN STORE fail with ORA-00922, while whole repeated clauses fail with ORA-14460.
            val columnStoreLocking = b.sequence(b.optional(NO), ROW, LEVEL, LOCKING)
            b.rule(TABLE_COMPRESSION).define(
                    b.firstOf(
                            b.sequence(COMPRESS, b.firstOf(
                                    BASIC,
                                    b.sequence(FOR, b.firstOf(
                                            OLTP,
                                            b.sequence(b.firstOf(QUERY, ARCHIVE), b.optional(b.firstOf(LOW, HIGH))),
                                            b.sequence(b.firstOf(ALL, DIRECT_LOAD), OPERATIONS))),
                                    b.nextNot(FOR))),
                            NOCOMPRESS,
                            b.sequence(ROW, STORE, b.firstOf(
                                    b.sequence(COMPRESS, b.firstOf(
                                            BASIC,
                                            ADVANCED,
                                            b.sequence(FOR, b.firstOf(OLTP, b.sequence(b.firstOf(ALL, DIRECT_LOAD), OPERATIONS))),
                                            b.nextNot(FOR))),
                                    NOCOMPRESS,
                                    b.nextNot(COMPRESS))),
                            b.sequence(COLUMN, STORE,
                                    b.optional(columnStoreLocking),
                                    b.firstOf(
                                            b.sequence(COMPRESS, b.firstOf(
                                                    b.sequence(FOR, b.optional(MEMSPEED), b.firstOf(QUERY, ARCHIVE),
                                                            b.optional(b.firstOf(LOW, HIGH))),
                                                    b.nextNot(FOR))),
                                            b.nextNot(COMPRESS)),
                                    b.optional(columnStoreLocking),
                                    b.nextNot(b.firstOf(NOCOMPRESS, b.sequence(ROW, STORE))))))

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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html (LOB_storage_clause)
            // The diagram repeats {SECUREFILE | BASICFILE | LOB_segname | (params)} freely; Oracle 26 instead
            // parses them once each in that order (ORA-00922 otherwise), rejects SECUREFILE with BASICFILE
            // (ORA-43852) or a repeated type (ORA-22850) and a segment name for several columns (ORA-22855),
            // all during parsing. After a storage type, the segment name is optional and Oracle reads the
            // table properties below as the next clause instead; other words (PCTFREE, COMPRESS) become the
            // segment name and fail on the following token.
            // A bare COMPRESS or NOCOMPRESS directly after the storage type is read as the segment name (ORA-00906).
            val lobStorageType = b.sequence(b.firstOf(SECUREFILE, BASICFILE), b.nextNot(b.firstOf(COMPRESS, NOCOMPRESS)))
            val lobParameters = b.sequence(LPARENTHESIS, LOB_PARAMETERS, RPARENTHESIS)
            val optionalLobSegname = b.optional(
                b.nextNot(b.firstOf(
                    TABLESPACE, LOGGING, NOLOGGING, PCTUSED, INITRANS, MAXTRANS, STORAGE,
                    PARALLEL, NOPARALLEL, ENABLE, DISABLE, CACHE, NOCACHE,
                    PARTITION, LOB, NESTED, VARRAY, ANNOTATIONS, SECUREFILE, BASICFILE)),
                IDENTIFIER_NAME)
            b.rule(LOB_STORAGE_CLAUSE).define(
                    LOB,
                    b.firstOf(
                            b.sequence(
                                    LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS, STORE, AS,
                                    b.firstOf(
                                            b.sequence(lobStorageType, optionalLobSegname, b.optional(lobParameters)),
                                            b.sequence(b.nextNot(b.firstOf(SECUREFILE, BASICFILE)), IDENTIFIER_NAME, b.optional(lobParameters)),
                                            lobParameters)),
                            b.sequence(
                                    LPARENTHESIS,
                                    b.oneOrMore(
                                            IDENTIFIER_NAME,
                                            b.optional(COMMA)),
                                    RPARENTHESIS, STORE, AS,
                                    b.firstOf(
                                            b.sequence(lobStorageType, b.optional(lobParameters)),
                                            lobParameters))))

            // Oracle 26 rejects `STORE ALL VARRAYS` after `OF XMLTYPE XMLTYPE` (ORA-00905) and a string-literal
            // schema URL (ORA-19002), so neither is modeled. Any other word after the storage type is read as the
            // LOB segment name, even TABLESPACE, PCTFREE, LOB or PARTITION (errors land on the following token),
            // except VIRTUAL after BINARY XML, which starts VIRTUAL COLUMNS.
            fun xmlLobSegment(vararg extraStops: Any) = b.optional(b.firstOf(
                b.sequence(LPARENTHESIS, LOB_PARAMETERS, RPARENTHESIS),
                b.sequence(
                    b.nextNot(b.firstOf(XMLSCHEMA, ELEMENT, XMLTYPE, *extraStops)),
                    IDENTIFIER_NAME,
                    b.optional(LPARENTHESIS, LOB_PARAMETERS, RPARENTHESIS))))
            b.rule(XMLTYPE_STORAGE).define(
                STORE, AS,
                b.firstOf(
                    b.sequence(OBJECT, RELATIONAL),
                    b.sequence(
                        b.optional(b.firstOf(SECUREFILE, BASICFILE)),
                        b.firstOf(
                            b.sequence(CLOB, xmlLobSegment()),
                            b.sequence(
                                b.optional(b.optional(NOT), TRANSPORTABLE), BINARY, XML,
                                xmlLobSegment(VIRTUAL))))))

            b.rule(XMLSCHEMA_SPEC).define(
                b.optional(XMLSCHEMA, IDENTIFIER_NAME), ELEMENT, IDENTIFIER_NAME,
                b.optional(STORE, ALL, VARRAYS, AS, b.firstOf(LOBS, TABLES)),
                b.optional(b.firstOf(ALLOW, DISALLOW), NONSCHEMA),
                b.optional(b.firstOf(ALLOW, DISALLOW), ANYSCHEMA))

            b.rule(XMLTYPE_COLUMN_PROPERTIES).define(
                XMLTYPE, b.optional(COLUMN), IDENTIFIER_NAME, b.optional(XMLTYPE_STORAGE), b.optional(XMLSCHEMA_SPEC))

            // Unlike the diagram, Oracle 26 takes VIRTUAL COLUMNS after ON COMMIT and the OID clause, repeated,
            // and interleaved with XMLSchema clauses. Datatypes, constraints and other column properties fail.
            val xmlVirtualColumn = b.sequence(
                IDENTIFIER_NAME,
                b.optional(GENERATED, ALWAYS),
                AS, LPARENTHESIS, EXPRESSION, RPARENTHESIS,
                b.optional(VIRTUAL), b.optional(VISIBLE))
            b.rule(XMLTYPE_VIRTUAL_COLUMNS).define(
                VIRTUAL, COLUMNS, LPARENTHESIS, xmlVirtualColumn, b.zeroOrMore(COMMA, xmlVirtualColumn), RPARENTHESIS)

            b.rule(XMLTYPE_TABLE).define(
                OF, XMLTYPE, b.nextNot(DOT),
                b.optional(OBJECT_TABLE_PROPERTIES),
                b.optional(XMLTYPE, XMLTYPE_STORAGE),
                b.optional(XMLSCHEMA_SPEC),
                b.optional(ON, COMMIT, b.firstOf(DELETE, PRESERVE), ROWS),
                b.optional(OBJECT_IDENTIFIER_CLAUSE),
                b.zeroOrMore(b.firstOf(XMLTYPE_VIRTUAL_COLUMNS, XMLSCHEMA_SPEC)))

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
                                    ROW),
                            INDEX_STORAGE_CLAUSE,
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

            val deferredSegmentCreation = b.sequence(SEGMENT, CREATION, b.firstOf(IMMEDIATE, DEFERRED))

            // Oracle 26 accepts this option in subpartition storage descriptions and templates,
            // even though the subpartition diagrams omit it.
            b.rule(PARTITIONING_STORAGE_CLAUSE).define(
                    b.optional(
                            b.oneOrMore(
                                    b.firstOf(
                                            deferredSegmentCreation,
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
                                                    LITERAL,
                                                    IDENTIFIER_NAME,
                                                    MAXVALUE),
                                            b.optional(COMMA)),
                                    RPARENTHESIS)))

            // Keep compression, overflow, and column-storage stages in their prior order and cardinality.
            // Oracle 26 accepts SEGMENT CREATION at every boundary. It also accepts physical attributes
            // after compression (including NOCOMPRESS LOGGING STORAGE in the ECLAIMPROCESS fixture).
            val segmentCreationWithAttributes = b.sequence(
                    deferredSegmentCreation, b.optional(SEGMENT_ATTRIBUTES_CLAUSE))
            b.rule(TABLE_PARTITION_DESCRIPTION).define(
                    b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                    b.zeroOrMore(segmentCreationWithAttributes),
                    b.optional(b.firstOf(TABLE_COMPRESSION, KEY_COMPRESSION)),
                    b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                    b.zeroOrMore(segmentCreationWithAttributes),
                    b.optional(OVERFLOW, b.optional(SEGMENT_ATTRIBUTES_CLAUSE)),
                    b.zeroOrMore(segmentCreationWithAttributes),
                    b.zeroOrMore(b.firstOf(
                            LOB_STORAGE_CLAUSE,
                            VARRAY_COL_PROPERTIES,
                            NESTED_TABLE_COL_PROPERTIES,
                            segmentCreationWithAttributes)),
                    b.optional(PARTITION_LEVEL_SUBPARTITION))

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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html
            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLE.html
            // CREATE and ALTER TABLE parse the In-Memory clauses identically. Each clause is one table property,
            // repeatable in any order: TEXT must directly follow INMEMORY (ORA-00922 after other attributes,
            // unlike the diagram), NO INMEMORY TEXT is also accepted, and the column clause has no ALL.
            // A second table-level INMEMORY/NO INMEMORY fails with ORA-64350, a duplicate-option error that,
            // like the other repeated table options, is not encoded. MEMCOMPRESS AUTO is rejected.
            val memcompress = b.firstOf(
                    b.sequence(MEMCOMPRESS, FOR, b.firstOf(
                            DML,
                            b.sequence(QUERY, b.optional(b.firstOf(LOW, HIGH))),
                            b.sequence(CAPACITY, b.optional(b.firstOf(LOW, HIGH))))),
                    b.sequence(NO, MEMCOMPRESS))
            val inmemoryAttribute = b.firstOf(
                    memcompress,
                    b.sequence(PRIORITY, b.firstOf(NONE, LOW, MEDIUM, HIGH, CRITICAL)),
                    b.sequence(DISTRIBUTE,
                            b.optional(b.firstOf(
                                    AUTO,
                                    b.sequence(BY, b.firstOf(b.sequence(ROWID, RANGE_KEYWORD), PARTITION, SUBPARTITION)))),
                            b.optional(FOR, SERVICE, b.firstOf(DEFAULT, ALL, NONE, IDENTIFIER_NAME))),
                    b.sequence(DUPLICATE, b.optional(ALL)),
                    b.sequence(NO, DUPLICATE))
            // Text columns are names (expressions fail with ORA-00904) with an optional literal policy name
            // (ORA-01780 for an identifier).
            val inmemoryTextColumn = b.sequence(
                    IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)),
                    b.optional(USING, CHARACTER_LITERAL))
            val inmemoryProperty = b.firstOf(
                    b.sequence(b.optional(NO), INMEMORY, TEXT,
                            LPARENTHESIS, inmemoryTextColumn, b.zeroOrMore(COMMA, inmemoryTextColumn), RPARENTHESIS),
                    b.sequence(INMEMORY, b.optional(memcompress), ONE_OR_MORE_IDENTIFIERS),
                    b.sequence(NO, INMEMORY, ONE_OR_MORE_IDENTIFIERS),
                    b.sequence(INMEMORY, b.zeroOrMore(inmemoryAttribute)),
                    b.sequence(NO, INMEMORY))

            // NO DROP and NO DELETE retention clauses, shared by CREATE of immutable/blockchain tables and ALTER.
            // Oracle 26 reports a missing retention count as a value-range error (ORA-05741), not a syntax
            // error, in both statements, so the count is optional here.
            val noDropClause = b.sequence(NO, DROP, b.optional(UNTIL, b.optional(INTEGER_LITERAL), DAYS, IDLE))
            val noDeleteClause = b.sequence(
                    NO, DELETE, b.optional(UNTIL, b.optional(INTEGER_LITERAL), DAYS, AFTER, INSERT), b.optional(LOCKED))

            // Oracle 26 accepts the table-level segment attributes, PARALLEL/NOPARALLEL and annotations in any
            // order, both before and after the partitioning clause. Column properties such as LOB storage may
            // also follow the segment attributes, but not the partitioning clause (ORA-14301). Duplicate
            // PCTFREE, TABLESPACE or PARALLEL options fail with duplicate-option errors (ORA-02212/ORA-02215/
            // ORA-12812), which the shared SEGMENT_ATTRIBUTES_CLAUSE does not encode either. Annotations must
            // not precede ORGANIZATION INDEX (ORA-64303) or ON COMMIT (ORA-00922).
            fun rowMovementClause() = b.sequence(b.firstOf(ENABLE, DISABLE), ROW, MOVEMENT)

            // Oracle 26 also accepts ROW MOVEMENT anywhere among these properties, but only once (ORA-14190).
            // The In-Memory clauses and FOR STAGING are equally position-free, before or after partitioning;
            // CREATE rejects NOT FOR STAGING (ORA-00922), and a repeated FOR STAGING fails with ORA-12990.
            fun tableLevelProperty() = b.firstOf(
                    ANNOTATIONS_CLAUSE, SEGMENT_ATTRIBUTES_CLAUSE, INDEX_PARALLEL_CLAUSE, rowMovementClause(),
                    inmemoryProperty, b.sequence(FOR, STAGING), TABLE_COMPRESSION)

            fun tableSuffixesWithAnnotations() = b.sequence(
                    b.zeroOrMore(b.firstOf(
                            tableLevelProperty(),
                            NESTED_TABLE_COL_PROPERTIES,
                            LOB_STORAGE_CLAUSE,
                            VARRAY_COL_PROPERTIES,
                            XMLTYPE_COLUMN_PROPERTIES)),
                    tablePartitioning(),
                    b.zeroOrMore(tableLevelProperty()))

            // Table-level SEGMENT CREATION must come first: after relational properties but before column
            // properties, ORGANIZATION INDEX and other physical properties (ORA-00922), and not before ON COMMIT.

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLE.html
            // Immutable and blockchain tables take their clauses right after the relational properties, or
            // after the name for CTAS, before any other property (ORA-02000 otherwise). Oracle 26 requires them
            // in this fixed order, each once (ORA-02000/ORA-00922), unlike the separately listed optional
            // clauses of the diagrams:
            //   NO DROP … NO DELETE … HASHING USING alg [WITH …] [CONFIGURE n SYSTEM CHAINS PER INSTANCE]
            //   VERSION v                                                  (blockchain, all but WITH/CONFIGURE required)
            //   NO DROP … NO DELETE … [WITH ROW VERSION name (…)] [VERSION v]   (immutable; row version first,
            //                                                                  no HASHING, USER CHAIN or CONFIGURE)
            // The hash algorithm and version are identifiers; unsupported values fail later (ORA-05716/ORA-05770)
            // while literals fail during parsing (ORA-05700). Row-version columns may be parenthesized
            // individually, as in the diagram. A missing CONFIGURE count is a range error (ORA-05804), so the
            // count is optional. BLOCKCHAIN IMMUTABLE is not documented and Oracle 26 answers any
            // CREATE BLOCKCHAIN not followed by TABLE with ORA-00439 before parsing further, so it stays out.
            val rowVersionColumn = b.firstOf(b.sequence(LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS), IDENTIFIER_NAME)
            val rowVersionColumns = b.sequence(
                    LPARENTHESIS, rowVersionColumn, b.zeroOrMore(COMMA, rowVersionColumn), RPARENTHESIS)
            val blockchainTableClauses = b.sequence(
                    noDropClause, noDeleteClause,
                    HASHING, USING, IDENTIFIER_NAME,
                    b.optional(
                            WITH,
                            b.firstOf(
                                    b.sequence(USER, CHAIN),
                                    b.sequence(ROW, VERSION, b.optional(AND, USER, CHAIN))),
                            IDENTIFIER_NAME, rowVersionColumns),
                    b.optional(CONFIGURE, b.optional(INTEGER_LITERAL), SYSTEM, CHAINS, PER, INSTANCE),
                    VERSION, IDENTIFIER_NAME)
            val immutableTableClauses = b.sequence(
                    noDropClause, noDeleteClause,
                    b.optional(WITH, ROW, VERSION, IDENTIFIER_NAME, rowVersionColumns),
                    b.optional(VERSION, IDENTIFIER_NAME))

            // Share the physical-property parser across ordinary/CTAS and ledger branches without adding
            // an AST boundary or compiling another copy of these large anonymous expressions per branch.
            b.rule(CREATE_TABLE_RELATIONAL_TAIL).define(b.firstOf(
                        b.sequence(
                                deferredSegmentCreation,
                                tablePropertyClauses(),
                                b.optional(INDEX_ORGANIZED_TABLE_CLAUSE),
                                tableSuffixesWithAnnotations()),
                        b.sequence(
                                b.optional(TABLE_CLUSTER_CLAUSE),
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
                                                    b.sequence(
                                                        b.requireContext(PRIVATE_TEMPORARY_TABLE_CONTEXT, true),
                                                        b.firstOf(DROP, PRESERVE), DEFINITION),
                                                    b.sequence(
                                                        b.nextNot(b.requireContext(PRIVATE_TEMPORARY_TABLE_CONTEXT, true)),
                                                        b.firstOf(DELETE, PRESERVE), ROWS)),
                                                b.zeroOrMore(tableLevelProperty())),
                                        tableSuffixesWithAnnotations())))).skip()

            fun createTableBody(ledgerTableClauses: Any?): Any {
                val relationalTail = if (ledgerTableClauses == null) CREATE_TABLE_RELATIONAL_TAIL
                    else b.sequence(ledgerTableClauses, CREATE_TABLE_RELATIONAL_TAIL)
                return b.sequence(
                    UNIT_NAME,
                    b.withContext(CREATE_ANNOTATIONS_CONTEXT, true, b.firstOf(
                            b.sequence(
                                    b.firstOf(XMLTYPE_TABLE, OBJECT_TABLE_CLAUSE),
                                    tablePropertyClauses(),
                                    b.optional(INDEX_ORGANIZED_TABLE_CLAUSE),
                                    tableSuffixesWithAnnotations(),
                                    b.firstOf(
                                        b.sequence(b.requireContext(OUTLINE_CREATE_TABLE_CONTEXT, true),
                                            AS, DmlGrammar.SELECT_EXPRESSION),
                                        b.sequence(b.nextNot(b.requireContext(OUTLINE_CREATE_TABLE_CONTEXT, true)),
                                            b.optional(AS, DmlGrammar.SELECT_EXPRESSION)))),
                            b.sequence(
                                    b.next(ctasPropertiesStart),
                                    LPARENTHESIS, CTAS_RELATIONAL_PROPERTIES, RPARENTHESIS,
                                    relationalTail, AS, DmlGrammar.SELECT_EXPRESSION),
                            b.sequence(
                                    LPARENTHESIS, TABLE_RELATIONAL_PROPERTIES, RPARENTHESIS,
                                    relationalTail,
                                    b.nextNot(AS),
                                    b.nextNot(b.requireContext(OUTLINE_CREATE_TABLE_CONTEXT, true))),
                            b.sequence(
                                    relationalTail, AS, DmlGrammar.SELECT_EXPRESSION))),
                    b.optional(SEMICOLON))
            }

            b.rule(CREATE_TABLE).define(
                    CREATE,
                    b.firstOf(
                            b.sequence(b.optional(IMMUTABLE), BLOCKCHAIN, TABLE,
                                    createTableBody(blockchainTableClauses)),
                            b.sequence(IMMUTABLE, TABLE,
                                    createTableBody(immutableTableClauses)),
                            b.sequence(PRIVATE, TEMPORARY, TABLE,
                                    b.withContext(PRIVATE_TEMPORARY_TABLE_CONTEXT, true, createTableBody(null))),
                            b.sequence(JSON, COLLECTION, TABLE, UNIT_NAME,
                                    b.withContext(CREATE_ANNOTATIONS_CONTEXT, true, b.sequence(
                                            b.optional(WITH, ETAG),
                                            b.optional(JSON_COLLECTION_COLUMNS),
                                            CREATE_TABLE_RELATIONAL_TAIL,
                                            b.optional(AS, DmlGrammar.SELECT_EXPRESSION))),
                                    b.optional(SEMICOLON)),
                            b.sequence(b.optional(GLOBAL, TEMPORARY), TABLE, createTableBody(null))))

            // Oracle parses three-part object names for every non-column family; resolution rejects
            // nonexistent objects. Columns require table.column, optionally prefixed by a schema.
            val statisticsColumnName = b.sequence(
                IDENTIFIER_NAME, DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val statisticsObjectName = b.sequence(
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME), b.optional(DOT, IDENTIFIER_NAME))
            b.rule(STATISTICS_ASSOCIATION_TARGET).define(b.firstOf(
                b.sequence(COLUMNS, statisticsColumnName, b.zeroOrMore(COMMA, statisticsColumnName)),
                b.sequence(b.firstOf(FUNCTIONS, PACKAGES, TYPES, INDEXES, INDEXTYPES),
                    statisticsObjectName, b.zeroOrMore(COMMA, statisticsObjectName))))

            val statisticsType = b.sequence(
                USING, b.firstOf(NULL, b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))))
            // Oracle checks whether numeric costs are integral; keep that validation out of the parser.
            val statisticsCostValue = b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)
            val defaultCost = b.sequence(
                DEFAULT, COST, LPARENTHESIS, statisticsCostValue,
                COMMA, statisticsCostValue,
                COMMA, statisticsCostValue, RPARENTHESIS)
            val defaultSelectivity = b.sequence(DEFAULT, SELECTIVITY, b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL))
            val statisticsDefaults = b.firstOf(
                b.sequence(defaultCost, b.zeroOrMore(b.optional(COMMA), defaultSelectivity)),
                b.sequence(defaultSelectivity, b.zeroOrMore(b.optional(COMMA), defaultSelectivity),
                    b.optional(b.optional(COMMA), defaultCost)))
            val statisticsStorage = b.sequence(
                WITH, b.firstOf(SYSTEM, USER), MANAGED, STORAGE, TABLES)
            b.rule(ASSOCIATE_STATISTICS).define(
                ASSOCIATE, STATISTICS, WITH,
                b.firstOf(
                    b.sequence(b.next(INDEXTYPES), STATISTICS_ASSOCIATION_TARGET,
                        statisticsType, b.optional(statisticsStorage)),
                    b.sequence(STATISTICS_ASSOCIATION_TARGET,
                        b.firstOf(statisticsType, statisticsDefaults))),
                b.optional(SEMICOLON))

            b.rule(DISASSOCIATE_STATISTICS).define(
                DISASSOCIATE, STATISTICS, FROM, STATISTICS_ASSOCIATION_TARGET,
                b.optional(FORCE), b.optional(SEMICOLON))

            // XMLIndex parameter syntax is carried inside the same quoted parameter string.
            b.rule(INDEX_PARAMETERS_CLAUSE).define(
                PARAMETERS, LPARENTHESIS, CHARACTER_LITERAL, RPARENTHESIS)

            b.rule(INDEX_SIZE_CLAUSE).define(
                b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL), b.optional(b.firstOf("K", "M", "G", "T", "P", "E")))

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
                    // Oracle 26 also parses CELL_FLASH_CACHE without the documented parentheses.
                    b.sequence(CELL_FLASH_CACHE, b.firstOf(KEEP, NONE, DEFAULT)),
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
                    b.sequence(MAXTRANS, INTEGER_LITERAL),
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
                    ANNOTATIONS_CLAUSE,
                    computeStatistics))

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
                // COMPUTE STATISTICS is recognized for domain indexes, although Oracle rejects it
                // semantically for supported indextypes (ORA-29850). Keep it separate from ordinary attributes.
                b.zeroOrMore(computeStatistics),
                b.optional(INDEX_PARAMETERS_CLAUSE),
                b.zeroOrMore(computeStatistics))

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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/ccref/create-search-index.html
            // The PARAMETERS payload stays an opaque string literal. Oracle 26 accepts the options below in
            // any order, except that FILTER BY must precede ORDER BY, both must precede PARAMETERS, and
            // neither may be split by another option or repeated (ORA-29850); PARAMETERS may appear once
            // (ORA-29850). Duplicate ONLINE, LOCAL or PARALLEL options (ORA-02158/ORA-14000/ORA-12812)
            // are not encoded. Oracle rejects TABLESPACE and STORAGE here (ORA-29850).
            val searchIndexLocalPartition = b.sequence(PARTITION, b.optional(IDENTIFIER_NAME), b.optional(INDEX_PARAMETERS_CLAUSE))
            val searchIndexOption = b.firstOf(
                ONLINE,
                b.sequence(LOCAL, b.optional(LPARENTHESIS, searchIndexLocalPartition,
                    b.zeroOrMore(COMMA, searchIndexLocalPartition), RPARENTHESIS)),
                INDEX_PARALLEL_CLAUSE,
                UNUSABLE)
            // Oracle 26 parses qualified FILTER BY and ORDER BY columns and only fails to resolve them (ORA-00904),
            // but rejects expressions, NULLS and ASC/DESC after a FILTER BY column (ORA-02158).
            val searchIndexColumn = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME), b.optional(DOT, IDENTIFIER_NAME))
            val searchIndexOrderItem = b.sequence(searchIndexColumn, b.optional(b.firstOf(ASC, DESC)))
            val searchIndexOrderBy = b.sequence(ORDER, BY, searchIndexOrderItem, b.zeroOrMore(COMMA, searchIndexOrderItem))
            val searchIndexFilterBy = b.sequence(FILTER, BY, searchIndexColumn, b.zeroOrMore(COMMA, searchIndexColumn))

            // Several targets (ORA-29851), expressions (ORA-29958) and qualified columns (ORA-00904) fail only after
            // parsing, so the target list accepts expressions. ASC/DESC after a target is rejected (ORA-29850), which
            // is why CREATE_INDEX_EXPR is not reused.
            b.rule(CREATE_SEARCH_INDEX).define(
                CREATE, SEARCH, INDEX, b.optional(IF, NOT, EXISTS), CREATE_INDEX_SCHEMA_OBJECT_NAME,
                ON, CREATE_INDEX_SCHEMA_OBJECT_NAME, b.optional(IDENTIFIER_NAME),
                LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS,
                b.optional(FOR, b.firstOf(TEXT, JSON, XML)),
                b.zeroOrMore(searchIndexOption),
                b.optional(
                    b.firstOf(b.sequence(searchIndexFilterBy, b.optional(searchIndexOrderBy)), searchIndexOrderBy),
                    b.zeroOrMore(searchIndexOption)),
                b.optional(INDEX_PARAMETERS_CLAUSE, b.zeroOrMore(searchIndexOption)),
                b.optional(SEMICOLON))

            // VECTOR indexes have structured parameters, unlike the quoted payload of ordinary/domain indexes.
            // Oracle validates duplicate keys, incompatible organizations and numeric ranges after parsing.
            val vectorParameterName = b.firstOf(
                b.sequence(NEIGHBOR, b.firstOf(
                    b.sequence(PARTITION, GROUPING), PARTITIONS)),
                b.sequence(RESCORE, FACTOR),
                OFFLOAD_CREDENTIAL_NAME, OFFLOAD_URL,
                TYPE, NEIGHBORS, M, EFCONSTRUCTION, SAMPLES_PER_PARTITION,
                MIN_VECTORS_PER_PARTITION, ALGORITHM)
            val vectorParameterValue = b.firstOf(
                b.sequence(b.optional(b.firstOf(PLUS, MINUS)), b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)),
                ON, IDENTIFIER_NAME, CHARACTER_LITERAL)
            val vectorParameter = b.sequence(vectorParameterName, b.optional(vectorParameterValue))
            val vectorOrganization = b.sequence(
                ORGANIZATION,
                b.firstOf(
                    b.sequence(INMEMORY, b.optional(NEIGHBOR), GRAPH),
                    b.sequence(b.optional(NEIGHBOR), PARTITIONS)))
            val vectorOption = b.firstOf(
                vectorOrganization,
                b.sequence(b.optional(WITH), DISTANCE, b.firstOf(
                    b.sequence(CUSTOM, IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME)),
                    IDENTIFIER_NAME)),
                b.sequence(WITH, TARGET, ACCURACY, b.firstOf(
                    b.sequence(b.optional(b.firstOf(PLUS, MINUS)), b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)),
                    b.sequence(LPARENTHESIS, INTEGER_LITERAL, RPARENTHESIS),
                    IDENTIFIER_NAME)),
                b.sequence(PARAMETERS, LPARENTHESIS,
                    b.optional(vectorParameter, b.zeroOrMore(COMMA, vectorParameter)), RPARENTHESIS),
                b.sequence(QUANTIZATION, SCALAR, COMPRESSION, RATIO, INTEGER_LITERAL),
                b.sequence(DUPLICATE, ALL),
                b.sequence(DISTRIBUTE, b.optional(b.firstOf(
                    AUTO, b.sequence(BY, b.firstOf(
                        b.sequence(ROWID, RANGE_KEYWORD), PARTITION, SUBPARTITION))))),
                b.sequence(PARALLEL, b.optional(INTEGER_LITERAL)),
                ONLINE, LOCAL,
                b.sequence(TABLESPACE, IDENTIFIER_NAME))
            b.rule(CREATE_VECTOR_INDEX).define(
                CREATE, VECTOR, INDEX, b.optional(IF, NOT, EXISTS), CREATE_INDEX_SCHEMA_OBJECT_NAME,
                ON, CREATE_INDEX_SCHEMA_OBJECT_NAME,
                LPARENTHESIS, EXPRESSION, b.zeroOrMore(COMMA, EXPRESSION), RPARENTHESIS,
                b.optional(INCLUDE, LPARENTHESIS, IDENTIFIER_NAME,
                    b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                b.optional(GLOBAL),
                vectorOrganization,
                b.zeroOrMore(vectorOption),
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
                                    ENCRYPT, DECRYPT, b.sequence(SCOPE, IS), identityStart)),
                            DATATYPE)),
                    b.optional(COLLATE, IDENTIFIER_NAME),
                    b.optional(b.firstOf(b.sequence(DEFAULT, EXPRESSION), identityClause(true))),
                    b.optional(b.firstOf(columnEncryptionClause(), DECRYPT)),
                    b.zeroOrMore(b.firstOf(INLINE_REF_CONSTRAINT, INLINE_CONSTRAINT)),
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
            val otherSegmentAttribute = b.firstOf(PHYSICAL_ATRIBUTES_CLAUSE, LOGGING_CLAUSE)
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

            // RENAME CONSTRAINT (ORA-23290) and RENAME TO (ORA-14047) are also standalone operations.
            fun renameConstraintClause() = b.sequence(RENAME, CONSTRAINT, IDENTIFIER_NAME, TO, IDENTIFIER_NAME)
            fun renameTableClause() = b.sequence(RENAME, TO, IDENTIFIER_NAME)

            val resultCacheMode = b.sequence(MODE, b.firstOf(DEFAULT, FORCE))
            val resultCacheStandby = b.sequence(STANDBY, b.firstOf(ENABLE, DISABLE))

            // Oracle 26 accepts these properties in any order, together with the READ ONLY, ROW ARCHIVAL,
            // FOR STAGING, DEFAULT COLLATION and annotation clauses that the SQLRF diagram lists separately,
            // and the immutable-table retention clauses. Repeated PARALLEL, LOGGING or CACHE options fail with
            // duplicate-option errors (ORA-12812/ORA-14102/ORA-12814) that are not encoded here.
            // ANNOTATIONS followed by ENABLE CONSTRAINT parses but raises ORA-00600 when executed.
            val alterTableProperty = b.firstOf(
                    PHYSICAL_ATRIBUTES_CLAUSE,
                    LOGGING_CLAUSE,
                    TABLE_COMPRESSION,
                    inmemoryProperty,
                    INDEX_ALLOCATE_EXTENT_CLAUSE,
                    INDEX_DEALLOCATE_UNUSED_CLAUSE,
                    b.firstOf(CACHE, NOCACHE),
                    b.sequence(RESULT_CACHE, LPARENTHESIS, b.firstOf(
                            b.sequence(resultCacheMode, b.optional(COMMA, resultCacheStandby)),
                            b.sequence(resultCacheStandby, b.optional(COMMA, resultCacheMode))), RPARENTHESIS),
                    b.sequence(UPGRADE, b.optional(b.optional(NOT), INCLUDING, DATA)),
                    b.sequence(b.firstOf(MINIMIZE, NOMINIMIZE), RECORDS_PER_BLOCK),
                    INDEX_PARALLEL_CLAUSE,
                    rowMovementClause(),
                    b.sequence(DISABLE, LOGICAL, REPLICATION),
                    b.sequence(ENABLE, LOGICAL, REPLICATION, b.zeroOrMore(b.firstOf(
                            b.sequence(ALL, KEYS),
                            b.sequence(ALLOW, NOVALIDATE, KEYS),
                            b.sequence(b.optional(NO), PARTIAL, JSON)))),
                    b.sequence(b.optional(BLOCKCHAIN), FLASHBACK, ARCHIVE, b.optional(IDENTIFIER_NAME)),
                    b.sequence(NO, FLASHBACK, ARCHIVE),
                    noDropClause,
                    noDeleteClause,
                    b.sequence(READ, b.firstOf(ONLY, WRITE)),
                    b.sequence(b.optional(NO), ROW, ARCHIVAL),
                    b.sequence(b.optional(NOT), FOR, STAGING),
                    b.sequence(DEFAULT, COLLATION, IDENTIFIER_NAME),
                    ANNOTATIONS_CLAUSE)

            // alter_iot_clauses that may follow the properties. ADD OVERFLOW and COALESCE are standalone
            // (ORA-14048 when combined with a property).
            val alterIotClause = b.firstOf(
                    b.sequence(OVERFLOW, b.oneOrMore(b.firstOf(SEGMENT_ATTRIBUTES_CLAUSE,
                            INDEX_ALLOCATE_EXTENT_CLAUSE, INDEX_SHRINK_CLAUSE, INDEX_DEALLOCATE_UNUSED_CLAUSE))),
                    b.sequence(MAPPING, TABLE, b.firstOf(INDEX_ALLOCATE_EXTENT_CLAUSE, INDEX_DEALLOCATE_UNUSED_CLAUSE)),
                    b.oneOrMore(b.firstOf(b.sequence(PCTTHRESHOLD, INTEGER_LITERAL), KEY_COMPRESSION)))
            val addOverflowPartition = b.sequence(PARTITION, b.optional(SEGMENT_ATTRIBUTES_CLAUSE))
            val addOverflowClause = b.sequence(ADD, OVERFLOW, b.optional(SEGMENT_ATTRIBUTES_CLAUSE),
                    b.optional(LPARENTHESIS, addOverflowPartition, b.zeroOrMore(COMMA, addOverflowPartition), RPARENTHESIS))

            // SHRINK must be the last operation (ORA-10630), and MOVE cannot be combined (ORA-14133).
            val shrinkTableClause = b.sequence(b.zeroOrMore(alterTableProperty), INDEX_SHRINK_CLAUSE)
            // Oracle 26 accepts the MOVE options, including the row filter and UPDATE INDEXES, in any order.
            // A second ONLINE is a syntax error (ORA-01735); other repeats fail as duplicate options
            // (ORA-02215/ORA-12812/ORA-14460).
            val moveTableOption = b.firstOf(
                    b.sequence(INCLUDING, ROWS, DmlGrammar.WHERE_CLAUSE),
                    SEGMENT_ATTRIBUTES_CLAUSE,
                    TABLE_COMPRESSION,
                    LOB_STORAGE_CLAUSE,
                    VARRAY_COL_PROPERTIES,
                    INDEX_PARALLEL_CLAUSE,
                    b.sequence(UPDATE, INDEXES, b.optional(
                            LPARENTHESIS, IDENTIFIER_NAME, SEGMENT_ATTRIBUTES_CLAUSE,
                            b.zeroOrMore(COMMA, IDENTIFIER_NAME, SEGMENT_ATTRIBUTES_CLAUSE), RPARENTHESIS)))
            val moveTableClause = b.sequence(
                    MOVE,
                    b.zeroOrMore(moveTableOption),
                    b.optional(ONLINE, b.zeroOrMore(moveTableOption)))

            val alterTableTrailingClause = b.firstOf(
                    ENABLE_DISABLE_CLAUSE,
                    b.sequence(b.firstOf(ENABLE, DISABLE), b.firstOf(
                            b.sequence(TABLE, LOCK), b.sequence(ALL, TRIGGERS), CONTAINER_MAP, CONTAINERS_DEFAULT)))
            val statementEnd = b.next(b.firstOf(SEMICOLON, DIVISION, EOF))

            fun alterTableAction() = b.firstOf(
                            b.sequence(
                                    ADD,
                                    b.firstOf(
                                            b.sequence(LPARENTHESIS, TABLE_RELATIONAL_PROPERTIES, RPARENTHESIS),
                                            TABLE_RELATIONAL_PROPERTIES),
                                    tablePropertyClauses()),
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
                            b.sequence(DROP, b.next(PARTITION), TABLE_RELATIONAL_PROPERTIES))

            b.rule(ALTER_TABLE).define(
                    ALTER, TABLE, UNIT_NAME,
                    b.firstOf(
                            renameColumnClause(),
                            renameConstraintClause(),
                            renameTableClause(),
                            RENAME_PARTITION_SUBPART,
                            MOVE_TABLE_PARTITION,
                            TRUNCATE_PARTITION_SUBPART,
                            EXCHANGE_PARTITION_SUBPART,
                            ADD_RANGE_TABLE_PARTITIONS,
                            SPLIT_TABLE_PARTITION,
                            MERGE_TABLE_PARTITIONS,
                            MODIFY_PARTITION_LOCAL_INDEXES,
                            moveTableClause,
                            shrinkTableClause,
                            // Tried before ADD column; the end-of-statement lookahead keeps a column named OVERFLOW
                            // (ADD overflow NUMBER) parsing as a column.
                            b.sequence(addOverflowClause, b.zeroOrMore(alterTableTrailingClause), statementEnd),
                            COALESCE,
                            b.sequence(
                                    b.oneOrMore(DROP_CONSTRAINT_CLAUSE),
                                    b.zeroOrMore(alterTableTrailingClause)),
                            b.sequence(alterTableAction(), b.zeroOrMore(alterTableTrailingClause)),
                            b.sequence(
                                    b.firstOf(
                                            b.sequence(b.oneOrMore(alterTableProperty), b.optional(alterIotClause)),
                                            alterIotClause),
                                    b.zeroOrMore(alterTableTrailingClause)),
                            b.oneOrMore(alterTableTrailingClause)),
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
            createProfile(b)
            createTablespace(b)
            createRole(b)
            createRollbackSegment(b)
            administerKeyManagement(b)
            alterPluggableDatabase(b)
            createPluggableDatabase(b)
            alterDatabase(b)
            alterDiskgroup(b)
            createCluster(b)
            createAnalyze(b)
            createAudit(b)
            createAssertion(b)
            createZonemap(b)
            createAttributeDimension(b)
            createDimension(b)
            createDatabaseLink(b)
            createOutline(b)
            createInmemoryJoinGroup(b)
            createAlterMleModule(b)
            createAlterMleEnv(b)
            createAlterView(b)
            createFlashbackArchive(b)
            createPurge(b)
            createParameterFile(b)
            createRestorePoint(b)
            createFlashbackTable(b)
            createEdition(b)
            createOperator(b)
            createIndextype(b)

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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/CREATE-LIBRARY-statement.html
            val libraryName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val libraryEditionability = b.optional(b.firstOf(EDITIONABLE, NONEDITIONABLE))
            b.rule(CREATE_LIBRARY).define(
                    CREATE,
                    b.firstOf(
                            b.sequence(OR, REPLACE, libraryEditionability, LIBRARY),
                            b.sequence(libraryEditionability, LIBRARY, b.optional(IF, NOT, EXISTS))),
                    libraryName,
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE)),
                    b.firstOf(IS, AS),
                    CHARACTER_LITERAL,
                    b.optional(IN, IDENTIFIER_NAME),
                    b.optional(AGENT, CHARACTER_LITERAL),
                    b.optional(CREDENTIAL, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)),
                    b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/ALTER-LIBRARY-statement.html
            // One action only (ORA-03049 for a second); at most one name qualifier (ORA-00922).
            b.rule(ALTER_LIBRARY).define(
                    ALTER, LIBRARY, b.optional(IF, EXISTS), libraryName,
                    b.firstOf(EDITIONABLE, NONEDITIONABLE, COMPILE_CLAUSE),
                    b.optional(SEMICOLON))

            // Package and type units name the part to compile; a type has no PACKAGE option (ORA-03049).
            // https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/ALTER-TYPE-statement.html
            fun unitCompileClause(vararg parts: Any) = b.sequence(
                    COMPILE, b.optional(DEBUG),
                    b.optional(b.firstOf(parts[0], parts[1], *parts.drop(2).toTypedArray())),
                    b.zeroOrMore(COMPILER_PARAMETERS_CLAUSE),
                    b.optional(REUSE, SETTINGS))
            b.rule(PACKAGE_COMPILE_CLAUSE).define(unitCompileClause(PACKAGE, SPECIFICATION, BODY))
            b.rule(TYPE_COMPILE_CLAUSE).define(unitCompileClause(SPECIFICATION, BODY))

            // Oracle 26 diagrams share the quota/contents suffix, but only ordinary DROP accepts IF EXISTS.
            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLESPACE-SET.html
            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/DROP-TABLESPACE.html
            b.rule(TABLESPACE_DROP_OPTIONS).define(
                b.optional(b.firstOf(DROP, KEEP), QUOTA),
                b.optional(INCLUDING, CONTENTS,
                    b.optional(b.firstOf(AND, KEEP), DATAFILES),
                    b.optional(CASCADE, CONSTRAINTS))).skip()

            // Check the statement end so a malformed suffix cannot become another generic DROP in FILE_INPUT.
            val tablespaceDropEnd = b.sequence(b.next(b.firstOf(SEMICOLON, DIVISION, EOF)), b.optional(SEMICOLON))
            b.rule(DROP_TABLESPACE_SET).define(
                DROP, TABLESPACE, SET, IDENTIFIER_NAME, TABLESPACE_DROP_OPTIONS, tablespaceDropEnd)
            b.rule(DROP_TABLESPACE).define(
                DROP, TABLESPACE, b.nextNot(SET), b.optional(IF, EXISTS), IDENTIFIER_NAME,
                TABLESPACE_DROP_OPTIONS, tablespaceDropEnd)

            // Tablespaces have explicit productions; never let an invalid one fall back to arbitrary tokens.
            b.rule(DROP_COMMAND).define(DROP, b.nextNot(TABLESPACE),
                b.oneOrMore(b.anyTokenButNot(b.firstOf(SEMICOLON, DIVISION, EOF))), b.optional(SEMICOLON))

            b.rule(CREATE_JAVA).define(
                CREATE,
                b.optional(OR, REPLACE),
                b.optional(AND, b.firstOf(RESOLVE, COMPILE)),
                b.optional(NOFORCE),
                JAVA,
                b.optional(IF, NOT, EXISTS),
                CREATE_JAVA_OBJECT,
                b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-JAVA.html
            // Oracle 26 takes IF EXISTS after SOURCE/CLASS, not after JAVA as diagrammed (ORA-02000). Either
            // kind accepts RESOLVE or COMPILE; exactly one of RESOLVE, COMPILE or AUTHID ends the statement
            // (ORA-03049 for a second), and RESOLVER cannot stand alone (ORA-00922).
            b.rule(ALTER_JAVA).define(
                ALTER, JAVA, b.firstOf(SOURCE, CLASS), b.optional(IF, EXISTS), UNIT_NAME,
                b.optional(JAVA_RESOLVER_CLAUSE),
                b.firstOf(RESOLVE, COMPILE, JAVA_AUTHID_CLAUSE),
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
                // Oracle 26 accepts an empty RESOLVER () in both CREATE and ALTER JAVA.
                b.zeroOrMore(JAVA_RESOLVER_ENTRY),
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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SYNONYM.html
            // Oracle 26 accepts exactly one action (ORA-03049 for a second) and rejects a schema-qualified
            // public synonym at the dot (ORA-00922).
            b.rule(ALTER_SYNONYM).define(
                    ALTER,
                    b.firstOf(
                            b.sequence(PUBLIC, SYNONYM, b.optional(IF, EXISTS), IDENTIFIER_NAME),
                            b.sequence(SYNONYM, b.optional(IF, EXISTS), IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))),
                    b.firstOf(EDITIONABLE, NONEDITIONABLE, COMPILE),
                    b.optional(SEMICOLON))

            val sequenceInteger = b.sequence(
                    b.optional(b.firstOf(PLUS, MINUS)),
                    b.next(INTEGER_LITERAL),
                    NUMERIC_LITERAL)

            // Options shared by CREATE and ALTER SEQUENCE. Oracle 26 accepts them in any order and parses SCALE
            // without EXTEND/NOEXTEND; duplicate or conflicting options fail after parsing (ORA-02279..ORA-02281).
            val sequenceOption = b.firstOf(
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
                    b.sequence(SCALE, b.optional(b.firstOf(EXTEND, NOEXTEND))),
                    NOSCALE,
                    SESSION,
                    GLOBAL)

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SEQUENCE.html
            // RESTART (ORA-64602), SHARD, OR REPLACE and EDITIONABLE are not CREATE SEQUENCE syntax.
            b.rule(CREATE_SEQUENCE).define(
                    CREATE, SEQUENCE, b.optional(IF, NOT, EXISTS), UNIT_NAME,
                    b.optional(SHARING, EQUALS, b.firstOf(METADATA, DATA, NONE)),
                    b.zeroOrMore(sequenceOption),
                    b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-SEQUENCE.html
            // At least one option is required (ORA-02286), and SHARING is not an ALTER option. START WITH without
            // RESTART (ORA-02283) and a repeated RESTART (ORA-64601) fail only after parsing. The probe instance
            // rejects SHARD before parsing (ORA-02511), so SHARD follows the documented diagram.
            b.rule(ALTER_SEQUENCE).define(
                    ALTER, SEQUENCE, b.optional(IF, EXISTS), UNIT_NAME,
                    b.oneOrMore(b.firstOf(
                            sequenceOption,
                            RESTART,
                            b.sequence(SHARD, b.firstOf(EXTEND, NOEXTEND)),
                            NOSHARD)),
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

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/TRUNCATE-CLUSTER.html
            // Oracle 26 rejects DROP ALL STORAGE, CASCADE and a second storage clause (ORA-03291). It also
            // parses the undocumented TRUNCATE TABLE materialized view log clause, before or after the
            // storage clause. A database link (dblink[.domain][@connection]) parses and fails later with
            // ORA-02021; a missing or numeric link name is ORA-01729.
            val clusterStorageClause = b.sequence(b.firstOf(DROP, REUSE), STORAGE)
            val clusterMaterializedViewLogClause = b.sequence(b.firstOf(PRESERVE, PURGE), MATERIALIZED, VIEW, LOG)
            b.rule(TRUNCATE_CLUSTER).define(
                TRUNCATE, CLUSTER, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                b.optional(REMOTE, DATABASE_LINK_NAME),
                b.optional(b.firstOf(
                    b.sequence(clusterStorageClause, b.optional(clusterMaterializedViewLogClause)),
                    b.sequence(clusterMaterializedViewLogClause, b.optional(clusterStorageClause)))),
                b.optional(SEMICOLON))

            // Oracle accepts repeated resource names up to later validation (ORA-02376). Decimal
            // and scientific numeric tokens are checked for integral values after parsing.
            b.rule(ALTER_RESOURCE_COST).define(
                ALTER, RESOURCE, COST,
                b.oneOrMore(
                    b.firstOf(CPU_PER_SESSION, CONNECT_TIME, LOGICAL_READS_PER_SESSION, PRIVATE_SGA),
                    b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)),
                b.optional(SEMICOLON))

            // Oracle 26 rejects owners only after parsing (ORA-01765), even for deeper dotted names.
            // A source @dblink is accepted and renames the local object; the destination rejects @.
            val renameObjectName = b.sequence(IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME))
            b.rule(RENAME_STATEMENT).define(
                RENAME, renameObjectName,
                b.optional(REMOTE, IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME)),
                TO, renameObjectName, b.optional(SEMICOLON))

            b.rule(DDL_COMMAND).define(b.firstOf(
                DDL_COMMENT,
                CREATE_TABLE,
                CREATE_INDEX,
                CREATE_SEARCH_INDEX,
                CREATE_VECTOR_INDEX,
                ASSOCIATE_STATISTICS,
                DISASSOCIATE_STATISTICS,
                RENAME_STATEMENT,
                ALTER_RESOURCE_COST,
                ADMINISTER_KEY_MANAGEMENT,
                ALTER_PLUGGABLE_DATABASE,
                CREATE_JAVA,
                ALTER_JAVA,
                CREATE_CONTEXT,
                CREATE_DOMAIN,
                ALTER_DOMAIN,
                CREATE_FLEXIBLE_DOMAIN,
                CREATE_MATERIALIZED_ZONEMAP,
                ALTER_MATERIALIZED_ZONEMAP,
                ALTER_MATERIALIZED_VIEW,
                ALTER_MATERIALIZED_VIEW_LOG,
                CREATE_ATTRIBUTE_DIMENSION,
                CREATE_HIERARCHY,
                CREATE_DIMENSION,
                ALTER_DIMENSION,
                ALTER_ATTRIBUTE_DIMENSION,
                ALTER_HIERARCHY,
                ALTER_ANALYTIC_VIEW,
                CREATE_DATABASE_LINK,
                ALTER_DATABASE_LINK,
                ALTER_DATABASE,
                ALTER_DISKGROUP,
                CREATE_OUTLINE,
                ALTER_OUTLINE,
                CREATE_INMEMORY_JOIN_GROUP,
                ALTER_INMEMORY_JOIN_GROUP,
                CREATE_MLE_ENV,
                CREATE_MLE_MODULE,
                ALTER_MLE_MODULE,
                ALTER_MLE_ENV,
                ALTER_VIEW,
                CREATE_FLASHBACK_ARCHIVE,
                ALTER_FLASHBACK_ARCHIVE,
                PURGE_STATEMENT,
                CREATE_PFILE,
                CREATE_SPFILE,
                CREATE_RESTORE_POINT,
                FLASHBACK_TABLE,
                CREATE_EDITION,
                CREATE_OPERATOR,
                ALTER_OPERATOR,
                CREATE_INDEXTYPE,
                ALTER_INDEXTYPE,
                CREATE_AUDIT_POLICY,
                ALTER_AUDIT_POLICY,
                CREATE_PROPERTY_GRAPH,
                CREATE_USER,
                ALTER_USER,
                CREATE_PROFILE,
                ALTER_PROFILE,
                CREATE_TABLESPACE,
                CREATE_TABLESPACE_SET,
                ALTER_TABLESPACE,
                ALTER_TABLESPACE_SET,
                CREATE_ROLE,
                ALTER_ROLE,
                CREATE_ROLLBACK_SEGMENT,
                ALTER_ROLLBACK_SEGMENT,
                CREATE_CLUSTER,
                ALTER_CLUSTER,
                ANALYZE_STATEMENT,
                AUDIT_STATEMENT,
                NOAUDIT_STATEMENT,
                CREATE_ASSERTION,
                CALL_COMMAND,
                ALTER_SYSTEM,
                ALTER_LOCKDOWN_PROFILE,
                CREATE_LOCKDOWN_PROFILE,
                CREATE_PLUGGABLE_DATABASE,
                ALTER_TABLE,
                ALTER_INDEX,
                ALTER_TRIGGER,
                ALTER_PROCEDURE,
                ALTER_FUNCTION,
                ALTER_PACKAGE,
                ALTER_TYPE,
                CREATE_LIBRARY,
                ALTER_LIBRARY,
                CREATE_SYNONYM,
                ALTER_SYNONYM,
                CREATE_SEQUENCE,
                ALTER_SEQUENCE,
                CREATE_DIRECTORY,
                DROP_DIRECTORY,
                DROP_TABLESPACE_SET,
                DROP_TABLESPACE,
                DROP_COMMAND,
                TRUNCATE_CLUSTER,
                TRUNCATE_TABLE))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html
        // The single-column (`AS datatype` or `AS ENUM (...)`), multi-column `AS (...)` and flexible branches.
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

            val defaultProperty = b.sequence(
                DEFAULT,
                b.optional(ON, NULL, b.optional(FOR, INSERT, b.firstOf(ONLY, b.sequence(AND, UPDATE)))),
                EXPRESSION)
            val nullProperty = b.sequence(b.optional(NOT), NULL)
            val validateProperty = b.sequence(VALIDATE, b.optional(CAST), b.optional(USING), CHARACTER_LITERAL)
            val collateProperty = b.sequence(COLLATE, IDENTIFIER_NAME)
            val displayProperty = b.sequence(DISPLAY, EXPRESSION)
            val orderProperty = b.sequence(ORDER, EXPRESSION)
            val annotationsProperty = b.withContext(CREATE_ANNOTATIONS_CONTEXT, true, ANNOTATIONS_CLAUSE)

            // Oracle 26 accepts these properties in any order after the datatype and STRICT, with CHECK
            // constraints repeatable. Oracle rejects a repeated singleton property (ORA-00139/ORA-02258),
            // but tracking that per property makes the compiled grammar grow factorially, so the parser
            // accepts repeats.
            val domainProperty = b.firstOf(
                DOMAIN_CONSTRAINT, defaultProperty, nullProperty, validateProperty, collateProperty,
                displayProperty, orderProperty, annotationsProperty)

            // ENUM (name [= alias]... [= value], ...) exists only in domains: a table column typed ENUM fails with
            // ORA-03060, so it stays out of DATATYPE. An alias is a bare name followed by `=`, `,` or `)`; anything
            // else after `=` is the constant value, which must come last (ORA-00917 for `a = 1 = b`), hence a
            // non-boolean expression. Oracle 26 also accepts an empty list and a trailing comma.
            val enumItem = b.sequence(
                IDENTIFIER_NAME,
                b.zeroOrMore(EQUALS, IDENTIFIER_NAME, b.next(b.firstOf(EQUALS, COMMA, RPARENTHESIS))),
                b.optional(EQUALS, PlSqlGrammar.CONCATENATION_EXPRESSION))
            b.rule(DOMAIN_ENUM).define(
                ENUM, LPARENTHESIS,
                b.optional(enumItem, b.zeroOrMore(COMMA, enumItem), b.optional(COMMA)),
                RPARENTHESIS)

            // Unquoted ENUM always starts the enum branch: Oracle 26 reports ORA-00904 right after a bare ENUM,
            // while "ENUM" or other names are datatypes (ORA-11531 afterwards).
            val domainType = b.firstOf(DOMAIN_ENUM, b.sequence(b.nextNot(ENUM), DATATYPE))

            // In a multi-column domain each column takes the column-level properties (plus annotations, which the
            // diagram omits) but not DISPLAY or ORDER (ORA-00904/ORA-03050); the domain as a whole takes only
            // CHECK constraints, DISPLAY, ORDER and annotations (ORA-03048/ORA-03049 for the others). Both lists
            // accept any order.
            b.rule(DOMAIN_COLUMN).define(
                IDENTIFIER_NAME, AS, domainType, b.optional(STRICT),
                b.zeroOrMore(b.firstOf(
                    DOMAIN_CONSTRAINT, defaultProperty, nullProperty, validateProperty, collateProperty,
                    annotationsProperty)))
            val multiColumnProperty = b.firstOf(DOMAIN_CONSTRAINT, displayProperty, orderProperty, annotationsProperty)

            b.rule(CREATE_DOMAIN).define(
                CREATE, b.optional(USECASE), DOMAIN, b.optional(IF, NOT, EXISTS),
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                AS,
                b.firstOf(
                    // Oracle 26 also accepts a trailing comma before the closing parenthesis.
                    b.sequence(
                        LPARENTHESIS, DOMAIN_COLUMN, b.zeroOrMore(COMMA, DOMAIN_COLUMN), b.optional(COMMA), RPARENTHESIS,
                        b.zeroOrMore(multiColumnProperty)),
                    b.sequence(
                        domainType,
                        // STRICT must follow the datatype immediately (ORA-03049 elsewhere).
                        b.optional(STRICT),
                        b.zeroOrMore(domainProperty))),
                b.optional(SEMICOLON))

            // The flexible domain is a separate statement shape: bare column names (ORA-03050 for `v1 NUMBER`),
            // then `name datatype` discriminants (ORA-00902 without a datatype). Oracle 26 accepts any expression
            // after FROM while parsing (a plain call, NVL, `|| 'x'`); the documented DECODE/CASE restriction is
            // semantic. Nothing may follow it (ORA-03049 for DISPLAY or ANNOTATIONS). Both lists may be empty,
            // and the column list accepts a trailing comma.
            b.rule(CREATE_FLEXIBLE_DOMAIN).define(
                CREATE, b.optional(USECASE), FLEXIBLE, DOMAIN, b.optional(IF, NOT, EXISTS),
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                LPARENTHESIS,
                b.optional(IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), b.optional(COMMA)),
                RPARENTHESIS,
                CHOOSE, DOMAIN, USING,
                LPARENTHESIS,
                b.optional(IDENTIFIER_NAME, DATATYPE, b.zeroOrMore(COMMA, IDENTIFIER_NAME, DATATYPE)),
                RPARENTHESIS,
                FROM, EXPRESSION,
                b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/alter-domain.html
            // A single action per statement (ORA-03048/ORA-03049 at a second one). Outside CREATE, the
            // annotations clause keeps its ADD/DROP/REPLACE directives.
            b.rule(ALTER_DOMAIN).define(
                ALTER, b.optional(USECASE), DOMAIN, b.optional(IF, EXISTS),
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(b.firstOf(ADD, MODIFY), b.firstOf(DISPLAY, ORDER), EXPRESSION),
                    b.sequence(DROP, b.firstOf(DISPLAY, ORDER)),
                    ANNOTATIONS_CLAUSE),
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

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PROFILE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PROFILE.html
        private fun createProfile(b: PlSqlGrammarBuilder) {
            val unlimitedOrDefault = b.firstOf(UNLIMITED, DEFAULT)
            // Resource limits take a plain integer: Oracle 26 rejects `CPU_PER_CALL 1+1` at the operator.
            val resourceParameter = b.firstOf(
                b.sequence(
                    b.firstOf(
                        SESSIONS_PER_USER, CPU_PER_SESSION, CPU_PER_CALL, CONNECT_TIME, IDLE_TIME,
                        LOGICAL_READS_PER_SESSION, LOGICAL_READS_PER_CALL, COMPOSITE_LIMIT),
                    b.firstOf(INTEGER_LITERAL, unlimitedOrDefault)),
                b.sequence(PRIVATE_SGA, b.firstOf(INDEX_SIZE_CLAUSE, unlimitedOrDefault)))
            // Oracle 26 also accepts UNLIMITED for PASSWORD_ROLLOVER_TIME, which the diagram omits.
            val passwordParameter = b.firstOf(
                b.sequence(
                    b.firstOf(
                        FAILED_LOGIN_ATTEMPTS, PASSWORD_LIFE_TIME, PASSWORD_REUSE_TIME, PASSWORD_REUSE_MAX,
                        PASSWORD_LOCK_TIME, PASSWORD_GRACE_TIME, INACTIVE_ACCOUNT_TIME, PASSWORD_ROLLOVER_TIME),
                    b.firstOf(unlimitedOrDefault, EXPRESSION)),
                b.sequence(
                    PASSWORD_VERIFY_FUNCTION,
                    b.firstOf(NULL, DEFAULT, b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)))))

            b.rule(PROFILE_LIMIT_CLAUSE).define(
                LIMIT, b.oneOrMore(b.firstOf(resourceParameter, passwordParameter)),
                b.optional(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)))

            b.rule(CREATE_PROFILE).define(
                CREATE, b.optional(MANDATORY), PROFILE, IDENTIFIER_NAME, PROFILE_LIMIT_CLAUSE, b.optional(SEMICOLON))

            b.rule(ALTER_PROFILE).define(
                ALTER, PROFILE, b.firstOf(DEFAULT, IDENTIFIER_NAME), PROFILE_LIMIT_CLAUSE, b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DATABASE-LINK.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE-LINK.html
        private fun createDatabaseLink(b: PlSqlGrammarBuilder) {
            // A link name is any number of dot-separated components (a global name such as remote.us.example.com,
            // not schema.object) with an optional @connection_qualifier.
            b.rule(DATABASE_LINK_NAME).define(
                IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME), b.optional(REMOTE, IDENTIFIER_NAME))

            // Passwords are identifiers (ORA-00988 for a literal); VALUES takes the hashed string.
            val password = b.firstOf(b.sequence(VALUES, CHARACTER_LITERAL), IDENTIFIER_NAME)
            val credential = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val connectAs = b.sequence(IDENTIFIER_NAME, IDENTIFIED, BY, password)
            val authentication = b.sequence(
                AUTHENTICATED, b.firstOf(b.sequence(BY, connectAs), b.sequence(WITH, CREDENTIAL)))
            // The connect string must be a literal (ORA-02010) and comes last (ORA-03048 for CONNECT after it).
            val usingClause = b.optional(USING, CHARACTER_LITERAL)
            // The lookahead keeps these rules from consuming the prefix of CREATE/ALTER DATABASE statements.
            fun header(start: Any, shared: Boolean) = b.sequence(
                b.next(start, b.optional(SHARED), b.optional(PUBLIC), DATABASE, LINK),
                start, if (shared) SHARED else b.nextNot(SHARED), b.optional(PUBLIC), DATABASE, LINK)

            // At most one CONNECT, before AUTHENTICATED (ORA-03048). AUTHENTICATED is required for a shared link
            // and rejected otherwise (ORA-00922/ORA-00905). Everything else is optional: `CREATE DATABASE LINK l`
            // parses. PUBLIC must follow SHARED (ORA-00901), and OR REPLACE is rejected.
            val createConnect = b.optional(
                CONNECT,
                b.firstOf(b.sequence(TO, b.firstOf(CURRENT_USER, connectAs)), b.sequence(WITH, credential)))
            val createName = b.sequence(b.optional(IF, NOT, EXISTS), DATABASE_LINK_NAME)
            b.rule(CREATE_DATABASE_LINK).define(
                b.firstOf(
                    b.sequence(header(CREATE, true), createName, createConnect, authentication, usingClause),
                    b.sequence(header(CREATE, false), createName, createConnect, usingClause)),
                b.optional(SEMICOLON))

            // ALTER cannot switch to CURRENT_USER (ORA-00987) and needs at least one clause (ORA-03048). It also
            // accepts the undocumented trailing USING. AUTHENTICATED stays shared-only (ORA-03049) but is optional.
            val alterConnect = b.sequence(
                CONNECT, b.firstOf(b.sequence(TO, connectAs), b.sequence(WITH, credential)))
            val alterName = b.sequence(b.optional(IF, EXISTS), DATABASE_LINK_NAME)
            b.rule(ALTER_DATABASE_LINK).define(
                b.firstOf(
                    b.sequence(
                        header(ALTER, true), alterName,
                        b.firstOf(
                            b.sequence(alterConnect, b.optional(authentication), usingClause),
                            b.sequence(authentication, usingClause),
                            b.sequence(USING, CHARACTER_LITERAL))),
                    b.sequence(
                        header(ALTER, false), alterName,
                        b.firstOf(b.sequence(alterConnect, usingClause), b.sequence(USING, CHARACTER_LITERAL)))),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-OUTLINE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-OUTLINE.html
        private fun createOutline(b: PlSqlGrammarBuilder) {
            val categoryClause = b.sequence(FOR, CATEGORY, IDENTIFIER_NAME)
            // Oracle permits only query/DML and CREATE TABLE AS SELECT here. Keep INSERT restricted to its
            // single-table subquery form; VALUES and multitable INSERT are not valid outline statements.
            val insertSelect = b.sequence(
                INSERT,
                DmlGrammar.INSERT_INTO_CLAUSE,
                b.optional(BY, b.firstOf(NAME, POSITION)),
                DmlGrammar.SELECT_EXPRESSION,
                b.optional(DmlGrammar.ERROR_LOGGING_CLAUSE))
            val outlinedStatement = b.firstOf(
                DmlGrammar.SELECT_EXPRESSION,
                DmlGrammar.DELETE_EXPRESSION,
                DmlGrammar.UPDATE_EXPRESSION,
                insertSelect,
                b.withContext(OUTLINE_CREATE_TABLE_CONTEXT, true, CREATE_TABLE))

            b.rule(CREATE_OUTLINE).define(
                CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(PUBLIC, PRIVATE)), OUTLINE,
                b.optional(IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(
                        FROM, b.optional(b.firstOf(PUBLIC, PRIVATE)), IDENTIFIER_NAME,
                        b.optional(categoryClause)),
                    b.sequence(b.optional(categoryClause), ON, outlinedStatement)),
                b.optional(SEMICOLON))

            val alterAction = b.firstOf(
                REBUILD,
                b.sequence(RENAME, TO, IDENTIFIER_NAME),
                b.sequence(CHANGE, CATEGORY, TO, IDENTIFIER_NAME),
                ENABLE,
                DISABLE)
            // Oracle 26 accepts PUBLIC/PRIVATE before OUTLINE, despite the generated syntax text placing it after.
            b.rule(ALTER_OUTLINE).define(
                ALTER, b.optional(b.firstOf(PUBLIC, PRIVATE)), OUTLINE, IDENTIFIER_NAME,
                b.oneOrMore(alterAction),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-FLASHBACK-ARCHIVE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-FLASHBACK-ARCHIVE.html
        private fun createFlashbackArchive(b: PlSqlGrammarBuilder) {
            val quotaClause = b.sequence(QUOTA, INDEX_SIZE_CLAUSE)
            val retentionClause = b.sequence(RETENTION, INTEGER_LITERAL, b.firstOf(YEAR, MONTH, DAY))
            val optimizeClause = b.firstOf(b.sequence(OPTIMIZE, DATA), b.sequence(NO, OPTIMIZE, DATA))

            // Oracle 26 enforces strict ordering: TABLESPACE, QUOTA, [NO] OPTIMIZE DATA, RETENTION.
            // Reordering produces ORA-55603. TABLESPACE and RETENTION are mandatory per the production.
            b.rule(CREATE_FLASHBACK_ARCHIVE).define(
                CREATE, FLASHBACK, ARCHIVE, b.optional(DEFAULT), IDENTIFIER_NAME,
                TABLESPACE, IDENTIFIER_NAME,
                b.optional(quotaClause),
                b.optional(optimizeClause),
                retentionClause,
                b.optional(SEMICOLON))

            // ALTER allows exactly one action per statement (ORA-03048 for two). No schema qualification.
            val alterAction = b.firstOf(
                b.sequence(SET, DEFAULT),
                b.sequence(b.firstOf(ADD, MODIFY), TABLESPACE, IDENTIFIER_NAME, b.optional(quotaClause)),
                b.sequence(REMOVE, TABLESPACE, IDENTIFIER_NAME),
                b.sequence(MODIFY, RETENTION, INTEGER_LITERAL, b.firstOf(YEAR, MONTH, DAY)),
                b.sequence(PURGE, b.firstOf(
                    ALL,
                    b.sequence(BEFORE, b.firstOf(
                        b.sequence(SCN, EXPRESSION),
                        b.sequence(TIMESTAMP, EXPRESSION))))),
                optimizeClause)
            b.rule(ALTER_FLASHBACK_ARCHIVE).define(
                ALTER, FLASHBACK, ARCHIVE, IDENTIFIER_NAME,
                alterAction,
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/PURGE.html
        private fun createPurge(b: PlSqlGrammarBuilder) {
            // TABLE and INDEX accept schema-qualified names (ORA-01435 for missing user) and
            // system-generated recycle-bin names containing $. TABLESPACE and USER are unqualified
            // (ORA-38303 for dot-qualified).
            b.rule(PURGE_STATEMENT).define(
                PURGE,
                b.firstOf(
                    b.sequence(b.firstOf(TABLE, INDEX), UNIT_NAME),
                    b.sequence(TABLESPACE, b.optional(SET), IDENTIFIER_NAME, b.optional(USER, IDENTIFIER_NAME)),
                    RECYCLEBIN,
                    DBA_RECYCLEBIN),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-PFILE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-SPFILE.html
        private fun createParameterFile(b: PlSqlGrammarBuilder) {
            val optionalFilename = b.optional(EQUALS, CHARACTER_LITERAL)
            // AS COPY is rejected on CREATE PFILE (ORA-03048).
            b.rule(CREATE_PFILE).define(
                CREATE, PFILE, optionalFilename,
                FROM, b.firstOf(b.sequence(SPFILE, optionalFilename), MEMORY),
                b.optional(SEMICOLON))

            // Oracle 26 parses AS COPY after both FROM PFILE and FROM MEMORY (ORA-01031 vs ORA-00922 for
            // AS GARBAGE), even though the syntax diagram places it only on the PFILE branch.
            b.rule(CREATE_SPFILE).define(
                CREATE, SPFILE, optionalFilename,
                FROM, b.firstOf(b.sequence(PFILE, optionalFilename), MEMORY),
                b.optional(AS, COPY),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-RESTORE-POINT.html
        // Oracle 26 enforces clause order FOR PLUGGABLE DATABASE, AS OF, then PRESERVE or GUARANTEE
        // (ORA-03048/ORA-03049 otherwise); PRESERVE and GUARANTEE are mutually exclusive and no clause
        // repeats. Names are unqualified (ORA-03048 for dotted or @dblink). CLEAN without a PDB,
        // non-scalar AS OF expressions (ORA-38730) and GUARANTEE with AS OF (ORA-38864) fail after parsing.
        private fun createRestorePoint(b: PlSqlGrammarBuilder) {
            b.rule(CREATE_RESTORE_POINT).define(
                CREATE, b.optional(CLEAN), RESTORE, POINT, IDENTIFIER_NAME,
                b.optional(FOR, PLUGGABLE, DATABASE, IDENTIFIER_NAME),
                b.optional(AS, OF, b.firstOf(SCN, TIMESTAMP), EXPRESSION),
                b.optional(b.firstOf(PRESERVE, b.sequence(GUARANTEE, FLASHBACK, DATABASE))),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/FLASHBACK-TABLE.html
        // Oracle 26 parses database links on the flashed-back table (ORA-02021 afterwards) and accepts
        // TRIGGER as well as TRIGGERS. TO BEFORE DROP takes one table, no trigger clause, and an
        // unqualified RENAME target (ORA-03048/ORA-03049). The SCN/TIMESTAMP value, including binds and
        // scalar subqueries, is validated after parsing.
        private fun createFlashbackTable(b: PlSqlGrammarBuilder) {
            val triggersClause = b.sequence(b.firstOf(ENABLE, DISABLE), b.firstOf(TRIGGERS, TRIGGER))
            b.rule(FLASHBACK_TABLE).define(
                FLASHBACK, TABLE,
                b.firstOf(
                    b.sequence(
                        DmlGrammar.TABLE_REFERENCE, TO, BEFORE, DROP,
                        b.optional(RENAME, TO, IDENTIFIER_NAME)),
                    b.sequence(
                        DmlGrammar.TABLE_REFERENCE, b.zeroOrMore(COMMA, DmlGrammar.TABLE_REFERENCE),
                        TO,
                        b.firstOf(
                            b.sequence(b.firstOf(SCN, TIMESTAMP), EXPRESSION),
                            b.sequence(RESTORE, POINT, IDENTIFIER_NAME)),
                        b.optional(triggersClause))),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-EDITION.html
        // Oracle 26 rejects qualified or @dblink edition and parent names during parsing
        // (ORA-02000/ORA-03048). Parent existence and single-child rules are checked afterwards.
        private fun createEdition(b: PlSqlGrammarBuilder) {
            b.rule(CREATE_EDITION).define(
                CREATE, EDITION, b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME,
                b.optional(AS, CHILD, OF, IDENTIFIER_NAME),
                b.optional(SEMICOLON))
        }

        // Parameter type list shared by operator bindings and indextype operator signatures; Oracle 26
        // parses both identically. Types are bare names: sizes/modifiers are rejected (ORA-00907), REF
        // always fails during parsing (ORA-29834), and INTERVAL/NATIONAL are invalid datatypes (ORA-00902).
        // Unknown names, LONG and LONG RAW parse and are only rejected by later signature checks.
        private fun operatorType(b: PlSqlGrammarBuilder) = b.firstOf(
            b.sequence(DOUBLE, PRECISION),
            b.sequence(LONG, RAW),
            b.sequence(b.nextNot(b.firstOf(REF, INTERVAL, NATIONAL)), UNIT_NAME))

        private fun operatorParameterTypes(b: PlSqlGrammarBuilder): Any {
            val type = operatorType(b)
            return b.sequence(LPARENTHESIS, type, b.zeroOrMore(COMMA, type), RPARENTHESIS)
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-OPERATOR.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-OPERATOR.html
        private fun createOperator(b: PlSqlGrammarBuilder) {
            val operatorType = operatorType(b)
            val parameterTypes = operatorParameterTypes(b)

            // Oracle 26 also accepts WITH COLUMN CONTEXT before ANCILLARY TO (the diagram makes them
            // exclusive), but not after it (ORA-00922). COMPUTE ANCILLARY DATA requires the index context.
            val columnContext = b.sequence(WITH, COLUMN, CONTEXT)
            val implementationClause = b.firstOf(
                b.sequence(
                    WITH, INDEX, CONTEXT, COMMA, SCAN, CONTEXT, UNIT_NAME,
                    b.optional(COMPUTE, ANCILLARY, DATA),
                    b.optional(columnContext)),
                b.sequence(
                    b.optional(columnContext),
                    ANCILLARY, TO, UNIT_NAME, parameterTypes,
                    b.zeroOrMore(COMMA, UNIT_NAME, parameterTypes)),
                columnContext)

            // Standalone, packaged or type-method functions: up to three components (four fail, ORA-00922).
            val usingFunctionClause = b.sequence(
                USING, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)))

            // ADD BINDING takes an unparenthesized RETURN type (ORA-00902), unlike its diagram.
            val binding = b.sequence(
                parameterTypes, RETURN, operatorType, b.optional(implementationClause), usingFunctionClause)

            // BINDING appears once before the comma-separated list (ORA-00906 when repeated). SHARING
            // precedes BINDING and accepts DATA as well (ORA-65021 afterwards, ORA-65014 for invalid values).
            b.rule(CREATE_OPERATOR).define(
                CREATE,
                b.firstOf(
                    b.sequence(OR, REPLACE, OPERATOR),
                    b.sequence(OPERATOR, b.optional(IF, NOT, EXISTS))),
                UNIT_NAME,
                b.optional(SHARING, EQUALS, b.firstOf(METADATA, DATA, NONE)),
                BINDING, binding, b.zeroOrMore(COMMA, binding),
                b.optional(SEMICOLON))

            // Exactly one action; ADD BINDING takes a single binding.
            b.rule(ALTER_OPERATOR).define(
                ALTER, OPERATOR, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    b.sequence(ADD, BINDING, binding),
                    b.sequence(DROP, BINDING, parameterTypes, b.optional(FORCE)),
                    COMPILE),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INDEXTYPE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INDEXTYPE.html
        private fun createIndextype(b: PlSqlGrammarBuilder) {
            val operatorSignature = b.sequence(UNIT_NAME, operatorParameterTypes(b))

            // The indexed type accepts a wider datatype language than operator signatures: INTERVAL and
            // NATIONAL CHAR parse, while REF, LONG and LONG RAW fail at once (ORA-29892) and sizes are
            // rejected. The varray type is a [schema.]name and cannot be omitted, despite the diagram.
            val arrayDmlType = b.firstOf(
                b.sequence(INTERVAL, b.firstOf(b.sequence(DAY, TO, SECOND), b.sequence(YEAR, TO, MONTH))),
                b.sequence(NATIONAL, CHAR),
                b.sequence(DOUBLE, PRECISION),
                b.sequence(b.nextNot(b.firstOf(REF, LONG, INTERVAL, NATIONAL)), UNIT_NAME))
            val arrayDmlMapping = b.sequence(LPARENTHESIS, arrayDmlType, COMMA, UNIT_NAME, RPARENTHESIS)
            // WITH or WITHOUT is required, and WITHOUT takes no mappings (ORA-00922).
            val arrayDmlClause = b.firstOf(
                b.sequence(WITH, ARRAY, DML, b.optional(arrayDmlMapping, b.zeroOrMore(COMMA, arrayDmlMapping))),
                b.sequence(WITHOUT, ARRAY, DML))
            val localPartition = b.sequence(WITH, LOCAL, b.optional(RANGE_KEYWORD), PARTITION)
            val storageTables = b.sequence(WITH, b.firstOf(SYSTEM, USER), MANAGED, STORAGE, TABLES)

            // Unlike the diagrams, array DML, local partitioning and storage tables follow USING in any
            // order, each at most once (ORA-00922 when repeated), and none of them is valid without USING.
            val usingTypeClause = b.sequence(
                USING, UNIT_NAME,
                b.anyOrder(arrayDmlClause, localPartition, storageTables))

            b.rule(CREATE_INDEXTYPE).define(
                CREATE,
                b.firstOf(
                    b.sequence(OR, REPLACE, INDEXTYPE),
                    b.sequence(INDEXTYPE, b.optional(IF, NOT, EXISTS))),
                UNIT_NAME,
                b.optional(SHARING, EQUALS, b.firstOf(METADATA, DATA, NONE)),
                FOR, operatorSignature, b.zeroOrMore(COMMA, operatorSignature),
                usingTypeClause,
                b.optional(SEMICOLON))

            // Every ADD/DROP repeats its keyword and may be followed by one comma, including a trailing one.
            // ADDs must precede DROPs (ORA-29841), and DROP has no FORCE.
            val addOperator = b.sequence(ADD, operatorSignature, b.optional(COMMA))
            val dropOperator = b.sequence(DROP, operatorSignature, b.optional(COMMA))
            b.rule(ALTER_INDEXTYPE).define(
                ALTER, INDEXTYPE, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(
                    COMPILE,
                    b.sequence(b.oneOrMore(addOperator), b.zeroOrMore(dropOperator), b.optional(usingTypeClause)),
                    b.sequence(b.oneOrMore(dropOperator), b.optional(usingTypeClause)),
                    usingTypeClause),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-INMEMORY-JOIN-GROUP.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-INMEMORY-JOIN-GROUP.html
        // Oracle 26 accepts one CREATE member and comma-separated ALTER members despite the narrower diagrams.
        private fun createInmemoryJoinGroup(b: PlSqlGrammarBuilder) {
            val member = b.sequence(UNIT_NAME, LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS)
            val members = b.sequence(LPARENTHESIS, member, b.zeroOrMore(COMMA, member), RPARENTHESIS)

            b.rule(CREATE_INMEMORY_JOIN_GROUP).define(
                CREATE, INMEMORY, JOIN, GROUP, b.optional(IF, NOT, EXISTS), UNIT_NAME,
                members, b.optional(SEMICOLON))

            b.rule(ALTER_INMEMORY_JOIN_GROUP).define(
                ALTER, INMEMORY, JOIN, GROUP, b.optional(IF, EXISTS), UNIT_NAME,
                b.firstOf(ADD, REMOVE), members, b.optional(SEMICOLON))
        }

        // Unlike the diagram, Oracle rejects PURE after CLONE and takes unparenthesised import items.
        private fun createAlterMleEnv(b: PlSqlGrammarBuilder) {
            val name = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val importItem = b.sequence(CHARACTER_LITERAL, MODULE, name)
            val importItems = b.sequence(LPARENTHESIS, importItem, b.zeroOrMore(COMMA, importItem), RPARENTHESIS)
            val languageOptions = b.sequence(LANGUAGE, OPTIONS, CHARACTER_LITERAL)

            b.rule(CREATE_MLE_ENV).define(
                CREATE, b.optional(OR, REPLACE), MLE, ENV, b.optional(IF, NOT, EXISTS), name,
                b.firstOf(
                    b.sequence(CLONE, name),
                    b.sequence(
                        b.optional(IMPORTS, importItems),
                        b.optional(languageOptions),
                        b.optional(PURE))),
                b.optional(SEMICOLON))

            b.rule(ALTER_MLE_ENV).define(
                ALTER, MLE, ENV, b.optional(IF, EXISTS), name,
                b.firstOf(
                    b.sequence(b.firstOf(ADD, ALTER), IMPORTS, importItems),
                    b.sequence(
                        DROP, IMPORTS,
                        LPARENTHESIS, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL), RPARENTHESIS),
                    b.sequence(SET, languageOptions),
                    COMPILE),
                b.optional(SEMICOLON))
        }

        // Oracle requires USING before CLOB, BLOB and BFILE, unlike the diagram, and rejects a parenthesised BFILE query.
        private fun createAlterMleModule(b: PlSqlGrammarBuilder) {
            val name = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val query = b.firstOf(
                b.sequence(LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION, RPARENTHESIS),
                DmlGrammar.SELECT_EXPRESSION)

            b.rule(MLE_MODULE_USING_CLAUSE).define(
                USING,
                b.firstOf(
                    b.sequence(BFILE, LPARENTHESIS, IDENTIFIER_NAME, COMMA, CHARACTER_LITERAL, RPARENTHESIS),
                    b.sequence(b.firstOf(CLOB, BLOB), query),
                    b.sequence(BFILE, b.nextNot(LPARENTHESIS), DmlGrammar.SELECT_EXPRESSION)))

            b.rule(CREATE_MLE_MODULE).define(
                CREATE, b.optional(OR, REPLACE), MLE, MODULE, b.optional(IF, NOT, EXISTS), name,
                LANGUAGE, name, b.optional(VERSION, CHARACTER_LITERAL),
                b.firstOf(
                    b.sequence(AS, PlSqlTokenType.MLE_MODULE_SOURCE),
                    MLE_MODULE_USING_CLAUSE),
                b.optional(SEMICOLON))

            b.rule(ALTER_MLE_MODULE).define(
                ALTER, MLE, MODULE, b.optional(IF, EXISTS), name,
                SET, METADATA, USING, CLOB,
                b.firstOf(
                    b.sequence(LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION, RPARENTHESIS),
                    DmlGrammar.SELECT_EXPRESSION),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-VIEW.html
        private fun createAlterView(b: PlSqlGrammarBuilder) {
            // A view does not accept CHECK constraints, although the shared out-of-line
            // constraint rule also serves tables. Keep its AST and restrict only this use.
            val viewConstraint = b.sequence(
                b.nextNot(b.sequence(b.optional(b.firstOf(CONSTRAINT, CONSTRAINTS), IDENTIFIER_NAME), CHECK)),
                b.withContext(VIEW_CONSTRAINT_CONTEXT, true, OUT_OF_LINE_CONSTRAINT))
            val addConstraint = b.sequence(ADD, b.firstOf(
                b.sequence(LPARENTHESIS, viewConstraint, b.zeroOrMore(COMMA, viewConstraint), RPARENTHESIS),
                viewConstraint))
            val columnAnnotations = b.sequence(IDENTIFIER_NAME, ANNOTATIONS_CLAUSE)
            val modify = b.sequence(MODIFY, b.firstOf(
                b.sequence(LPARENTHESIS, columnAnnotations, b.zeroOrMore(COMMA, columnAnnotations), RPARENTHESIS),
                b.sequence(b.firstOf(
                    b.sequence(CONSTRAINT, IDENTIFIER_NAME),
                    b.sequence(PRIMARY, KEY)),
                    b.firstOf(RELY, NORELY))))
            val drop = b.sequence(DROP, b.firstOf(
                b.sequence(CONSTRAINT, IDENTIFIER_NAME),
                b.sequence(PRIMARY, KEY),
                b.sequence(UNIQUE, ONE_OR_MORE_IDENTIFIERS)))
            val action = b.firstOf(
                addConstraint, modify, drop,
                COMPILE, RECOMPILE,
                b.sequence(READ, b.firstOf(ONLY, WRITE)),
                EDITIONABLE, NONEDITIONABLE,
                ANNOTATIONS_CLAUSE)

            // Oracle 26 accepts repeated COMPILE, toggled EDITIONABLE, repeated ANNOTATIONS,
            // and mixed ADD/COMPILE/MODIFY actions despite its single-action diagram.
            b.rule(ALTER_VIEW).define(
                ALTER, VIEW, b.optional(IF, EXISTS), UNIT_NAME,
                b.oneOrMore(action), b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DIMENSION.html
        // Legacy dimensions: unrelated to ATTRIBUTE DIMENSION apart from the shared words.
        private fun createDimension(b: PlSqlGrammarBuilder) {
            val column = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME), b.optional(DOT, IDENTIFIER_NAME))
            // A single column may drop its parentheses everywhere.
            fun columns(item: Any) =
                b.firstOf(b.sequence(LPARENTHESIS, item, b.zeroOrMore(COMMA, item), RPARENTHESIS), item)

            // Level columns need a table qualifier (ORA-30347 for a bare column).
            val levelColumn = b.sequence(
                IDENTIFIER_NAME, DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            b.rule(DIMENSION_LEVEL_CLAUSE).define(
                LEVEL, IDENTIFIER_NAME, IS, columns(levelColumn), b.optional(SKIP, WHEN, NULL))

            // At least one CHILD OF is required (ORA-02000). JOIN KEY columns and repetition are only checked
            // semantically (ORA-30365/ORA-30344).
            b.rule(DIMENSION_HIERARCHY_CLAUSE).define(
                HIERARCHY, IDENTIFIER_NAME, LPARENTHESIS,
                IDENTIFIER_NAME, b.oneOrMore(CHILD, OF, IDENTIFIER_NAME),
                b.zeroOrMore(JOIN, KEY, columns(column), REFERENCES, IDENTIFIER_NAME),
                RPARENTHESIS)

            // `ATTRIBUTE level DETERMINES ...`, or the extended `ATTRIBUTE name {LEVEL level DETERMINES ...}...`.
            val determines = b.sequence(DETERMINES, columns(column))
            b.rule(DIMENSION_ATTRIBUTE_CLAUSE).define(
                ATTRIBUTE, IDENTIFIER_NAME,
                b.firstOf(determines, b.oneOrMore(LEVEL, IDENTIFIER_NAME, determines)))

            // Levels come first (ORA-03048 for a later LEVEL); hierarchies and attributes then interleave and may
            // be absent (a levels-only dimension is created). OR REPLACE and IF NOT EXISTS are rejected.
            b.rule(CREATE_DIMENSION).define(
                CREATE, DIMENSION, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                b.oneOrMore(DIMENSION_LEVEL_CLAUSE),
                b.zeroOrMore(b.firstOf(DIMENSION_HIERARCHY_CLAUSE, DIMENSION_ATTRIBUTE_CLAUSE)),
                b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DIMENSION.html
            // ADD reuses the CREATE clauses. ADD and DROP actions cannot be mixed (ORA-30348 at the first action of
            // the other kind), while COMPILE may appear anywhere and repeat. DROP ATTRIBUTE takes at most one LEVEL
            // and one COLUMN (ORA-03048 at a second one). IF EXISTS and RENAME are rejected (ORA-11600/ORA-02000).
            val compile = b.zeroOrMore(COMPILE)
            val addAction = b.sequence(
                ADD, b.firstOf(DIMENSION_LEVEL_CLAUSE, DIMENSION_HIERARCHY_CLAUSE, DIMENSION_ATTRIBUTE_CLAUSE))
            val dropAction = b.sequence(
                DROP,
                b.firstOf(
                    b.sequence(LEVEL, IDENTIFIER_NAME, b.optional(b.firstOf(RESTRICT, CASCADE))),
                    b.sequence(HIERARCHY, IDENTIFIER_NAME),
                    b.sequence(
                        ATTRIBUTE, IDENTIFIER_NAME,
                        b.optional(LEVEL, IDENTIFIER_NAME, b.optional(COLUMN, column)))))
            b.rule(ALTER_DIMENSION).define(
                ALTER, DIMENSION, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                b.firstOf(
                    b.sequence(compile, b.oneOrMore(addAction, compile)),
                    b.sequence(compile, b.oneOrMore(dropAction, compile)),
                    b.oneOrMore(COMPILE)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ATTRIBUTE-DIMENSION.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-HIERARCHY.html
        private fun createAttributeDimension(b: PlSqlGrammarBuilder) {
            val schemaName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val attribute = IDENTIFIER_NAME
            val attributeList = b.sequence(LPARENTHESIS, attribute, b.zeroOrMore(COMMA, attribute), RPARENTHESIS)
            val header = b.sequence(
                CREATE, b.optional(OR, REPLACE), b.optional(b.firstOf(FORCE, NOFORCE)))
            val sharing = b.optional(SHARING, EQUALS, b.firstOf(METADATA, NONE))

            val classification = b.sequence(
                CLASSIFICATION, IDENTIFIER_NAME,
                b.optional(VALUE, CHARACTER_LITERAL), b.optional(LANGUAGE, CHARACTER_LITERAL))

            // Shared by both statements. The parts keep this order (ORA-02000 for DESCRIPTION before CAPTION or
            // LANGUAGE before VALUE), and every value is a string literal (ORA-01780).
            b.rule(AV_CLASSIFICATION_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        CAPTION, CHARACTER_LITERAL, b.optional(DESCRIPTION, CHARACTER_LITERAL),
                        b.zeroOrMore(classification)),
                    b.sequence(DESCRIPTION, CHARACTER_LITERAL, b.zeroOrMore(classification)),
                    b.oneOrMore(classification)))

            // Tables with an optional alias, joined only by `col = col` equalities (ORA-02000 for other operators);
            // Oracle 26 rejects a parenthesized source despite the diagram (ORA-00931).
            val column = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val source = b.sequence(
                schemaName, b.optional("REMOTE"),
                b.optional(b.optional(AS), b.nextNot(b.firstOf(ATTRIBUTES, JOIN)), IDENTIFIER_NAME))
            val usingClause = b.sequence(
                USING, source, b.zeroOrMore(COMMA, source),
                b.zeroOrMore(
                    JOIN, PATH, IDENTIFIER_NAME, ON,
                    column, EQUALS, column, b.zeroOrMore(AND, column, EQUALS, column)))

            val attributeItem = b.sequence(
                column, b.optional(b.optional(AS), b.nextNot(b.firstOf(CAPTION, DESCRIPTION, CLASSIFICATION)), IDENTIFIER_NAME),
                b.optional(AV_CLASSIFICATION_CLAUSE))

            // Member values are value expressions: a full condition would read `b MEMBER CAPTION` as a
            // `MEMBER [OF]` membership test.
            val memberValue = PlSqlGrammar.CONCATENATION_EXPRESSION

            val orderItem = b.sequence(
                b.optional(b.firstOf(MIN, MAX)), attribute, b.optional(b.firstOf(ASC, DESC)),
                b.optional(NULLS, b.firstOf(FIRST, LAST)))

            // Every part keeps this order (ORA-02000/ORA-03048/ORA-03049 otherwise), and KEY is required.
            b.rule(ATTRIBUTE_DIMENSION_LEVEL_CLAUSE).define(
                LEVEL, IDENTIFIER_NAME,
                b.optional(b.firstOf(b.sequence(NOT, NULL), b.sequence(SKIP, WHEN, NULL))),
                b.optional(
                    LEVEL, TYPE,
                    b.firstOf(STANDARD, YEARS, HALF_YEARS, QUARTERS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS)),
                b.optional(AV_CLASSIFICATION_CLAUSE),
                KEY, b.firstOf(attributeList, attribute),
                b.optional(ALTERNATE, KEY, b.firstOf(attributeList, attribute)),
                b.optional(MEMBER, NAME, memberValue),
                b.optional(MEMBER, CAPTION, memberValue),
                b.optional(MEMBER, DESCRIPTION, memberValue),
                b.optional(ORDER, BY, orderItem, b.zeroOrMore(COMMA, orderItem)),
                b.optional(DETERMINES, attributeList))

            // At least one level is required, also before ALL MEMBER (ORA-02000).
            b.rule(CREATE_ATTRIBUTE_DIMENSION).define(
                header, ATTRIBUTE, DIMENSION, b.optional(IF, NOT, EXISTS), schemaName, sharing,
                b.optional(AV_CLASSIFICATION_CLAUSE),
                b.optional(DIMENSION, TYPE, b.firstOf(STANDARD, TIME)),
                usingClause,
                ATTRIBUTES, LPARENTHESIS, attributeItem, b.zeroOrMore(COMMA, attributeItem), RPARENTHESIS,
                b.oneOrMore(ATTRIBUTE_DIMENSION_LEVEL_CLAUSE),
                b.optional(
                    ALL, MEMBER,
                    b.firstOf(
                        b.sequence(
                            NAME, memberValue,
                            b.optional(MEMBER, CAPTION, memberValue), b.optional(MEMBER, DESCRIPTION, memberValue)),
                        b.sequence(CAPTION, memberValue, b.optional(MEMBER, DESCRIPTION, memberValue)),
                        b.sequence(DESCRIPTION, memberValue))),
                b.optional(SEMICOLON))

            // Levels form a parenthesized CHILD OF chain (ORA-02000 for a comma or a missing OF). Hierarchical
            // attribute names are checked semantically, so any name parses.
            b.rule(CREATE_HIERARCHY).define(
                header, HIERARCHY, b.optional(IF, NOT, EXISTS), schemaName, sharing,
                b.optional(AV_CLASSIFICATION_CLAUSE),
                USING, schemaName,
                LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(CHILD, OF, IDENTIFIER_NAME), RPARENTHESIS,
                b.optional(
                    HIERARCHICAL, ATTRIBUTES, LPARENTHESIS,
                    IDENTIFIER_NAME, b.optional(AV_CLASSIFICATION_CLAUSE),
                    b.zeroOrMore(COMMA, IDENTIFIER_NAME, b.optional(AV_CLASSIFICATION_CLAUSE)),
                    RPARENTHESIS),
                b.optional(SEMICOLON))

            // Analytic view objects share the same header and action set: one action, the new name cannot be
            // schema-qualified (ORA-03048 at the dot), and COMPILE takes no options (ORA-03049).
            val renameOrCompile = b.firstOf(b.sequence(RENAME, TO, IDENTIFIER_NAME), COMPILE)

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ATTRIBUTE-DIMENSION.html
            b.rule(ALTER_ATTRIBUTE_DIMENSION).define(
                ALTER, ATTRIBUTE, DIMENSION, b.optional(IF, EXISTS), schemaName,
                renameOrCompile,
                b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-HIERARCHY.html
            b.rule(ALTER_HIERARCHY).define(
                ALTER, HIERARCHY, b.optional(IF, EXISTS), schemaName,
                renameOrCompile,
                b.optional(SEMICOLON))

            // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ANALYTIC-VIEW.html
            // The cache clause diagrams are empty in the Oracle 26 reference; this follows runtime behavior.
            // Exactly one cache specification: MEASURE GROUP and LEVELS are both required, in that order.
            // Measures are unqualified and at least one is needed (ORA-00931); ALL is not valid inside the
            // list. Levels take up to dim.hier.level (ORA-02000 for four parts) and may be empty. Unlike
            // CREATE ANALYTIC VIEW, level specifications are not individually parenthesized (ORA-00931).
            val levelName = b.sequence(
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)))
            val cacheSpecification = b.sequence(
                MEASURE, GROUP,
                b.firstOf(
                    ALL,
                    b.sequence(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)),
                LEVELS, LPARENTHESIS, b.optional(levelName, b.zeroOrMore(COMMA, levelName)), RPARENTHESIS)
            // ADD requires MATERIALIZED (ORA-02000, even for the documented example without it) and may name
            // the backing table; DROP accepts neither (ORA-03049).
            b.rule(ALTER_ANALYTIC_VIEW).define(
                ALTER, ANALYTIC, VIEW, b.optional(IF, EXISTS), schemaName,
                b.firstOf(
                    renameOrCompile,
                    b.sequence(
                        ADD, CACHE, cacheSpecification,
                        MATERIALIZED, b.optional(USING, schemaName)),
                    b.sequence(DROP, CACHE, cacheSpecification)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-ZONEMAP.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-MATERIALIZED-ZONEMAP.html
        private fun createZonemap(b: PlSqlGrammarBuilder) {
            val schemaName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val cacheClause = b.firstOf(CACHE, NOCACHE)
            val pruningClause = b.sequence(b.firstOf(ENABLE, DISABLE), PRUNING)

            // Unlike the materialized view refresh clause, this one has LOAD / DATA MOVEMENT triggers and no
            // START WITH / NEXT. A bare REFRESH fails with ORA-00905, so a method or an ON trigger is required.
            val refreshTrigger = b.sequence(
                ON,
                b.firstOf(DEMAND, COMMIT, b.sequence(LOAD, b.optional(DATA, MOVEMENT)), b.sequence(DATA, MOVEMENT)))
            b.rule(ZONEMAP_REFRESH_CLAUSE).define(
                REFRESH,
                b.firstOf(
                    b.sequence(b.firstOf(FAST, COMPLETE, FORCE), b.optional(refreshTrigger)),
                    refreshTrigger))

            // The attributes, refresh and pruning clauses keep this order (ORA-02000 otherwise). Repeated
            // attributes fail after parsing (ORA-12814/ORA-12990) and are not tracked. The AS query is a single
            // query block, optionally with a WITH clause: ORDER BY fails with ORA-00922 and set operators with
            // ORA-31956.
            b.rule(CREATE_MATERIALIZED_ZONEMAP).define(
                CREATE, MATERIALIZED, ZONEMAP, b.optional(IF, NOT, EXISTS), schemaName,
                b.zeroOrMore(b.firstOf(b.sequence(TABLESPACE, IDENTIFIER_NAME), b.sequence(SCALE, INTEGER_LITERAL), cacheClause)),
                b.optional(ZONEMAP_REFRESH_CLAUSE),
                b.optional(pruningClause),
                b.firstOf(
                    b.sequence(
                        ON, schemaName,
                        LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS),
                    b.sequence(AS, b.optional(DmlGrammar.WITH_CLAUSE), DmlGrammar.QUERY_BLOCK)),
                b.optional(SEMICOLON))

            // A single action per statement (ORA-00922 for `COMPILE REBUILD`); only the attributes repeat.
            b.rule(ALTER_MATERIALIZED_ZONEMAP).define(
                ALTER, MATERIALIZED, ZONEMAP, b.optional(IF, EXISTS), schemaName,
                b.firstOf(
                    b.oneOrMore(b.firstOf(
                        b.sequence(PCTFREE, INTEGER_LITERAL), b.sequence(PCTUSED, INTEGER_LITERAL), cacheClause)),
                    ZONEMAP_REFRESH_CLAUSE,
                    pruningClause,
                    COMPILE,
                    REBUILD,
                    UNUSABLE),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
        private fun createAssertion(b: PlSqlGrammarBuilder) {
            // ALL ... SATISFY exists only here: Oracle 26 rejects it in WHERE, CASE, after NOT and inside SATISFY
            // (ORA-00936). The alias is optional and may follow AS.
            b.rule(ASSERTION_UNIVERSAL_EXPRESSION).define(
                ALL, LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION, RPARENTHESIS,
                b.optional(b.optional(AS), b.nextNot(SATISFY), IDENTIFIER_NAME),
                SATISFY, LPARENTHESIS, EXPRESSION, RPARENTHESIS)

            // The existential form is a condition that must start with [NOT] EXISTS or a parenthesis (`1 = 1`
            // fails, `EXISTS (...) AND 1 = 1` parses). The universal form can only be parenthesized, not combined.
            b.rule(ASSERTION_CONDITION).define(
                b.firstOf(
                    ASSERTION_UNIVERSAL_EXPRESSION,
                    b.sequence(b.next(b.firstOf(NOT, EXISTS, LPARENTHESIS)), EXPRESSION),
                    b.sequence(LPARENTHESIS, ASSERTION_CONDITION, RPARENTHESIS)))

            // Unlike table constraints, RELY, USING INDEX and EXCEPTIONS are rejected here (ORA-00911), and the
            // states may come in any order.
            val assertionState = b.firstOf(
                b.sequence(b.optional(NOT), DEFERRABLE),
                b.sequence(INITIALLY, b.firstOf(DEFERRED, IMMEDIATE)),
                ENABLE, DISABLE, VALIDATE, NOVALIDATE)

            b.rule(CREATE_ASSERTION).define(
                CREATE, ASSERTION, b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME),
                CHECK, LPARENTHESIS, ASSERTION_CONDITION, RPARENTHESIS,
                b.zeroOrMore(assertionState),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/AUDIT-Unified-Auditing.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Unified-Auditing.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/NOAUDIT-Traditional-Auditing.html
        private fun createAudit(b: PlSqlGrammarBuilder) {
            val name = DclGrammar.IDENTIFIER_OR_KEYWORD
            val users = b.sequence(name, b.zeroOrMore(COMMA, name))
            val whenever = b.sequence(WHENEVER, b.optional(NOT), SUCCESSFUL)

            // Shared by AUDIT and NOAUDIT: Oracle 26 also accepts EXCEPT and WHENEVER in NOAUDIT POLICY, which its
            // diagram omits. The policy name cannot be schema-qualified (ORA-03048 at the dot).
            b.rule(AUDIT_POLICY_CLAUSE).define(
                POLICY, IDENTIFIER_NAME,
                b.optional(b.firstOf(
                    b.sequence(BY, USERS, WITH, GRANTED, AUDIT_ROLE_CLAUSE),
                    b.sequence(BY, users),
                    b.sequence(EXCEPT, users))),
                b.optional(whenever))

            // WHENEVER and role lists are rejected here in both statements (ORA-03048), unlike the NOAUDIT diagram.
            val namespace = b.sequence(
                CONTEXT, NAMESPACE, name,
                ATTRIBUTES, name, b.zeroOrMore(COMMA, b.nextNot(CONTEXT), name))
            b.rule(AUDIT_CONTEXT_CLAUSE).define(
                namespace, b.zeroOrMore(COMMA, namespace), b.optional(BY, users))

            b.rule(AUDIT_STATEMENT).define(
                AUDIT, b.firstOf(AUDIT_POLICY_CLAUSE, AUDIT_CONTEXT_CLAUSE), b.optional(SEMICOLON))

            // Traditional auditing: each option is a run of words such as SELECT TABLE, DELETE ANY TABLE, ROLE,
            // ALL STATEMENTS or DIRECT_PATH LOAD. Oracle 26 rejects BY after ON object (ORA-01708/ORA-01718).
            // Traditional AUDIT itself is desupported (ORA-46401 at its first option), so only NOAUDIT has it.
            val operation = b.oneOrMore(b.nextNot(b.firstOf(ON, BY, WHENEVER, CONTAINER)), name)
            val schemaObjectName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val traditional = b.sequence(
                operation, b.zeroOrMore(COMMA, operation),
                b.firstOf(
                    b.sequence(
                        ON,
                        b.firstOf(
                            b.sequence(DIRECTORY, IDENTIFIER_NAME),
                            b.sequence(MINING, MODEL, schemaObjectName),
                            b.sequence(SQL, TRANSLATION, PROFILE, schemaObjectName),
                            DEFAULT,
                            schemaObjectName)),
                    b.optional(BY, users)),
                b.optional(whenever),
                b.optional(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)))

            b.rule(NOAUDIT_STATEMENT).define(
                NOAUDIT, b.firstOf(AUDIT_POLICY_CLAUSE, AUDIT_CONTEXT_CLAUSE, traditional), b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ANALYZE.html
        private fun createAnalyze(b: PlSqlGrammarBuilder) {
            val objectName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val intoClause = b.sequence(INTO, objectName)

            // Oracle 26 rejects the FOR forms of the partition extension here (ORA-00906 at FOR), and any
            // partition after a cluster name (ORA-14052).
            val target = b.firstOf(
                b.sequence(
                    b.firstOf(TABLE, INDEX), objectName,
                    b.optional(b.firstOf(PARTITION, SUBPARTITION), LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS)),
                b.sequence(CLUSTER, objectName))

            // The diagram requires FAST or COMPLETE after CASCADE and ties ONLINE/OFFLINE to COMPLETE, but Oracle 26
            // parses a bare CASCADE and ONLINE/OFFLINE without CASCADE. CASCADE FAST ends the clause (ORA-03048).
            val validateStructure = b.sequence(
                STRUCTURE,
                b.firstOf(
                    b.sequence(CASCADE, FAST),
                    b.sequence(
                        b.optional(CASCADE, b.optional(COMPLETE)),
                        b.optional(b.firstOf(ONLINE, OFFLINE)),
                        b.optional(intoClause))))

            // LIST CHAINED ROWS on an index fails at parse time (ORA-01492), but is not singled out here.
            b.rule(ANALYZE_STATEMENT).define(
                ANALYZE, target,
                b.firstOf(
                    b.sequence(
                        VALIDATE,
                        b.firstOf(b.sequence(REF, UPDATE, b.optional(SET, DANGLING, TO, NULL)), validateStructure)),
                    b.sequence(LIST, CHAINED, ROWS, b.optional(intoClause)),
                    b.sequence(DELETE, b.optional(SYSTEM), STATISTICS)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-CLUSTER.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-CLUSTER.html
        private fun createCluster(b: PlSqlGrammarBuilder) {
            val clusterName = b.sequence(IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))
            val clusterColumn = b.sequence(
                IDENTIFIER_NAME, DATATYPE, b.optional(COLLATE, IDENTIFIER_NAME), b.optional(SORT))
            val cacheClause = b.firstOf(CACHE, NOCACHE)

            // Oracle 26 accepts these options in any order, including the parallel, row dependency and cache
            // clauses the diagram places last, and HASH IS before HASHKEYS. Repeats and INDEX with HASHKEYS fail
            // later (ORA-02228/ORA-02464) and are not tracked. SHARING is rejected at parse time (ORA-00922).
            val createOption = b.firstOf(
                PHYSICAL_ATRIBUTES_CLAUSE,
                b.sequence(SIZE, INDEX_SIZE_CLAUSE),
                b.sequence(TABLESPACE, IDENTIFIER_NAME),
                INDEX,
                b.sequence(SINGLE, TABLE),
                b.sequence(HASHKEYS, INTEGER_LITERAL),
                b.sequence(PlSqlKeyword.HASH, IS, EXPRESSION),
                INDEX_PARALLEL_CLAUSE,
                b.firstOf(ROWDEPENDENCIES, NOROWDEPENDENCIES),
                cacheClause)

            b.rule(CREATE_CLUSTER).define(
                CREATE, CLUSTER, b.optional(IF, NOT, EXISTS), clusterName,
                LPARENTHESIS, clusterColumn, b.zeroOrMore(COMMA, clusterColumn), RPARENTHESIS,
                b.zeroOrMore(createOption),
                b.optional(PARTITION_BY_RANGE),
                b.optional(SEMICOLON))

            // The parallel clause may also appear between the other options (ORA-02144 without any option).
            b.rule(ALTER_CLUSTER).define(
                ALTER, CLUSTER, b.optional(IF, EXISTS), clusterName,
                b.oneOrMore(b.firstOf(
                    PHYSICAL_ATRIBUTES_CLAUSE,
                    b.sequence(SIZE, INDEX_SIZE_CLAUSE),
                    b.sequence(b.optional(MODIFY, PARTITION, IDENTIFIER_NAME), INDEX_ALLOCATE_EXTENT_CLAUSE),
                    INDEX_DEALLOCATE_UNUSED_CLAUSE,
                    cacheClause,
                    INDEX_PARALLEL_CLAUSE)),
                b.optional(SEMICOLON))

            // A clustered table rejects TABLESPACE, CACHE, PARALLEL and partitioning afterwards
            // (ORA-01771/ORA-14026) but keeps its column properties such as LOB storage.
            b.rule(TABLE_CLUSTER_CLAUSE).define(
                CLUSTER, clusterName, LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLLBACK-SEGMENT.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLLBACK-SEGMENT.html
        private fun createRollbackSegment(b: PlSqlGrammarBuilder) {
            // TABLESPACE and STORAGE may come in any order; a second TABLESPACE fails with ORA-02215, which is
            // not tracked.
            b.rule(CREATE_ROLLBACK_SEGMENT).define(
                CREATE, b.optional(PUBLIC), ROLLBACK, SEGMENT, IDENTIFIER_NAME,
                b.zeroOrMore(b.firstOf(b.sequence(TABLESPACE, IDENTIFIER_NAME), INDEX_STORAGE_CLAUSE)),
                b.optional(SEMICOLON))

            // Oracle 26 accepts a single option (ORA-03049 at a second one) and also parses the undocumented
            // ALTER PUBLIC ROLLBACK SEGMENT.
            b.rule(ALTER_ROLLBACK_SEGMENT).define(
                ALTER, b.optional(PUBLIC), ROLLBACK, SEGMENT, IDENTIFIER_NAME,
                b.firstOf(
                    ONLINE,
                    OFFLINE,
                    INDEX_STORAGE_CLAUSE,
                    b.sequence(SHRINK, b.optional(TO, INDEX_SIZE_CLAUSE))),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ADMINISTER-KEY-MANAGEMENT.html
        private fun administerKeyManagement(b: PlSqlGrammarBuilder) {
            // The password is an identifier, not a literal (ORA-00988).
            b.rule(KEYSTORE_IDENTIFIED_BY).define(
                IDENTIFIED, BY,
                b.firstOf(b.sequence(EXTERNAL, STORE), b.sequence(b.nextNot(EXTERNAL), IDENTIFIER_NAME))).skip()
            b.rule(KEYSTORE_PASSWORD_IDENTIFIED_BY).define(IDENTIFIED, BY, IDENTIFIER_NAME).skip()

            // Oracle accepts an identifier token after CONTAINER = but reports ORA-65013
            // for values other than the documented ALL and CURRENT.
            b.rule(KEYSTORE_CONTAINER_CLAUSE).define(CONTAINER, EQUALS, b.firstOf(ALL, CURRENT)).skip()
            b.rule(FORCE_KEYSTORE).define(FORCE, KEYSTORE).skip()

            // Unlike the diagram, Oracle also accepts identifier-valued backup names.
            b.rule(KEYSTORE_BACKUP_IDENTIFIER).define(b.firstOf(CHARACTER_LITERAL, IDENTIFIER)).skip()
            b.rule(KEYSTORE_WITH_BACKUP).define(
                WITH, BACKUP, b.optional(USING, KEYSTORE_BACKUP_IDENTIFIER)).skip()

            b.rule(SECRET_KEYSTORE_TARGET).define(
                b.firstOf(
                    b.sequence(b.optional(LOCAL), AUTO_LOGIN, KEYSTORE, CHARACTER_LITERAL),
                    b.sequence(KEYSTORE, CHARACTER_LITERAL, KEYSTORE_PASSWORD_IDENTIFIED_BY))).skip()

            val tag = b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME)
            val currentKeystore = b.sequence(b.optional(FORCE_KEYSTORE), KEYSTORE_IDENTIFIED_BY)
            // WITH BACKUP is optional despite the diagram.
            b.rule(ADD_UPDATE_SECRET).define(
                b.firstOf(ADD, UPDATE), SECRET, CHARACTER_LITERAL, FOR, CLIENT, CHARACTER_LITERAL,
                b.optional(USING, TAG, tag),
                b.firstOf(b.sequence(TO, SECRET_KEYSTORE_TARGET), currentKeystore),
                b.optional(KEYSTORE_WITH_BACKUP))

            // SEPS DELETE's text wrongly includes 'secret'; its diagram and Oracle require FOR.
            b.rule(DELETE_SECRET).define(
                DELETE, SECRET, FOR, CLIENT, CHARACTER_LITERAL,
                b.firstOf(b.sequence(FROM, SECRET_KEYSTORE_TARGET), currentKeystore),
                b.optional(KEYSTORE_WITH_BACKUP))

            b.rule(SECRET_MANAGEMENT_CLAUSES).define(b.firstOf(ADD_UPDATE_SECRET, DELETE_SECRET))

            b.rule(OPEN_KEYSTORE).define(
                SET, KEYSTORE, OPEN,
                b.optional(FORCE_KEYSTORE),
                KEYSTORE_IDENTIFIED_BY,
                b.optional(KEYSTORE_CONTAINER_CLAUSE))

            b.rule(CLOSE_KEYSTORE).define(
                SET, KEYSTORE, CLOSE,
                b.optional(KEYSTORE_IDENTIFIED_BY),
                b.optional(KEYSTORE_CONTAINER_CLAUSE))

            b.rule(CREATE_KEYSTORE).define(
                CREATE,
                b.firstOf(
                    KEYSTORE,
                    b.sequence(b.optional(LOCAL), AUTO_LOGIN, KEYSTORE, FROM, KEYSTORE)),
                b.optional(CHARACTER_LITERAL),
                KEYSTORE_PASSWORD_IDENTIFIED_BY)

            b.rule(BACKUP_KEYSTORE).define(
                BACKUP, KEYSTORE,
                b.optional(USING, KEYSTORE_BACKUP_IDENTIFIER),
                b.optional(FORCE_KEYSTORE),
                KEYSTORE_IDENTIFIED_BY,
                b.optional(TO, CHARACTER_LITERAL))

            // WITH BACKUP is optional for ALTER PASSWORD and MERGE INTO EXISTING, despite their diagrams.
            b.rule(ALTER_KEYSTORE_PASSWORD).define(
                ALTER, KEYSTORE, PASSWORD,
                b.optional(FORCE_KEYSTORE),
                KEYSTORE_PASSWORD_IDENTIFIED_BY,
                SET, IDENTIFIER_NAME,
                b.optional(KEYSTORE_WITH_BACKUP))

            b.rule(MERGE_KEYSTORE_SOURCE).define(
                KEYSTORE, CHARACTER_LITERAL, b.optional(KEYSTORE_PASSWORD_IDENTIFIED_BY))

            b.rule(MERGE_INTO_NEW_KEYSTORE).define(
                MERGE, MERGE_KEYSTORE_SOURCE,
                AND, MERGE_KEYSTORE_SOURCE,
                INTO, NEW, KEYSTORE, CHARACTER_LITERAL,
                KEYSTORE_IDENTIFIED_BY)

            b.rule(MERGE_INTO_EXISTING_KEYSTORE).define(
                MERGE, MERGE_KEYSTORE_SOURCE,
                INTO, EXISTING, KEYSTORE, CHARACTER_LITERAL,
                KEYSTORE_IDENTIFIED_BY,
                b.optional(KEYSTORE_WITH_BACKUP))

            keyManagementCreation(b)
            keyManagementTransfers(b)
            keyManagementMigration(b)
            b.rule(KEY_MANAGEMENT_CLAUSES).define(
                b.firstOf(MIGRATE_KEY, REVERSE_MIGRATE_KEY,
                    SET_KEY, CREATE_KEY, USE_KEY, SET_KEY_TAG, EXPORT_KEYS, IMPORT_KEYS))

            b.rule(ADMINISTER_KEY_MANAGEMENT).define(
                ADMINISTER, KEY, MANAGEMENT,
                b.firstOf(OPEN_KEYSTORE, CLOSE_KEYSTORE, SECRET_MANAGEMENT_CLAUSES,
                    CREATE_KEYSTORE, BACKUP_KEYSTORE, ALTER_KEYSTORE_PASSWORD,
                    MERGE_INTO_NEW_KEYSTORE, MERGE_INTO_EXISTING_KEYSTORE, KEY_MANAGEMENT_CLAUSES),
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))
        }

        private fun keyManagementMigration(b: PlSqlGrammarBuilder) {
            // Oracle parses USING TAG and forward WITH BACKUP omitted by the migration diagram.
            val keyIdentifier = b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME)
            val tag = b.optional(USING, TAG, b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME))
            val authentication = b.sequence(KEYSTORE_PASSWORD_IDENTIFIED_BY, b.optional(FORCE_KEYSTORE))

            b.rule(MIGRATE_KEY).define(
                b.firstOf(
                    b.sequence(SET, b.optional(ENCRYPTION), KEY),
                    b.sequence(USE, b.optional(ENCRYPTION), KEY, keyIdentifier)),
                tag, authentication,
                MIGRATE, USING, IDENTIFIER_NAME,
                b.optional(KEYSTORE_WITH_BACKUP))

            b.rule(REVERSE_MIGRATE_KEY).define(
                SET, b.optional(ENCRYPTION), KEY,
                tag, authentication,
                REVERSE, MIGRATE, USING, IDENTIFIER_NAME,
                b.optional(KEYSTORE_WITH_BACKUP))
        }

        private fun keyManagementTransfers(b: PlSqlGrammarBuilder) {
            val authentication = b.sequence(b.optional(FORCE_KEYSTORE), KEYSTORE_IDENTIFIED_BY)
            val keyIdentifier = b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME)
            val identifierFilter = b.sequence(
                WITH, IDENTIFIER_KEYWORD, IN,
                b.firstOf(
                    b.sequence(LPARENTHESIS, DmlGrammar.SELECT_EXPRESSION, RPARENTHESIS),
                    b.sequence(keyIdentifier, b.zeroOrMore(COMMA, keyIdentifier))))

            b.rule(EXPORT_KEYS).define(
                EXPORT, b.optional(ENCRYPTION), KEYS, WITH, SECRET, IDENTIFIER_NAME,
                TO, CHARACTER_LITERAL, authentication, b.optional(identifierFilter))

            // Oracle 26 parses IMPORT without WITH BACKUP, despite its diagram.
            b.rule(IMPORT_KEYS).define(
                IMPORT, b.optional(ENCRYPTION), KEYS, WITH, SECRET, IDENTIFIER_NAME,
                FROM, CHARACTER_LITERAL, authentication, b.optional(KEYSTORE_WITH_BACKUP))
        }

        private fun keyManagementCreation(b: PlSqlGrammarBuilder) {
            val nameOrLiteral = b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME)
            val tag = b.sequence(USING, TAG, nameOrLiteral)
            val authentication = b.sequence(b.optional(FORCE_KEYSTORE), KEYSTORE_IDENTIFIED_BY)
            // MKID:MK is one literal, not two tokens separated by a SQL colon. CREATE's diagram
            // omits this documented material; Oracle 26 parses it for both SET and CREATE.
            val creation = b.sequence(
                b.optional(ENCRYPTION), KEY, b.optional(CHARACTER_LITERAL),
                b.optional(tag),
                b.optional(USING, ALGORITHM, CHARACTER_LITERAL),
                authentication,
                b.optional(KEYSTORE_WITH_BACKUP),
                b.optional(KEYSTORE_CONTAINER_CLAUSE))
            b.rule(SET_KEY).define(SET, creation)
            b.rule(CREATE_KEY).define(CREATE, creation)

            b.rule(USE_KEY).define(
                USE, b.optional(ENCRYPTION), KEY, nameOrLiteral,
                b.optional(tag), authentication, b.optional(KEYSTORE_WITH_BACKUP))
            // BACKUP is optional here and in the creation clauses despite their diagrams.
            b.rule(SET_KEY_TAG).define(
                SET, TAG, nameOrLiteral, FOR, nameOrLiteral,
                authentication, b.optional(KEYSTORE_WITH_BACKUP))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-PLUGGABLE-DATABASE.html
        private fun alterPluggableDatabase(b: PlSqlGrammarBuilder) {
            fun literalList() = b.sequence(
                LPARENTHESIS, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL), RPARENTHESIS)
            fun allOrList(keyword: PlSqlKeyword) = b.sequence(
                keyword, EQUALS, b.firstOf(literalList(), b.sequence(ALL, b.optional(EXCEPT, literalList()))))

            b.rule(PDB_INSTANCES_CLAUSE).define(allOrList(INSTANCES)).skip()

            // Oracle 26 accepts RESTRICTED and FORCE after every mode, although the diagram omits
            // FORCE after UPGRADE and both after RESETLOGS.
            b.rule(PDB_OPEN).define(
                OPEN,
                b.optional(b.firstOf(
                    b.sequence(READ, WRITE, b.optional(UPGRADE)),
                    b.sequence(READ, ONLY),
                    b.sequence(HYBRID, READ, ONLY),
                    UPGRADE,
                    RESETLOGS)),
                b.optional(RESTRICTED),
                b.optional(FORCE),
                b.optional(PDB_INSTANCES_CLAUSE),
                b.optional(b.firstOf(b.sequence(SERVICES, EQUALS, NONE), allOrList(SERVICES))))

            b.rule(PDB_CLOSE).define(
                CLOSE,
                b.firstOf(
                    b.sequence(ABORT, b.optional(PDB_INSTANCES_CLAUSE)),
                    b.sequence(
                        b.optional(IMMEDIATE),
                        b.optional(b.firstOf(
                            PDB_INSTANCES_CLAUSE,
                            b.sequence(RELOCATE, b.optional(TO, CHARACTER_LITERAL)),
                            NORELOCATE)))))

            b.rule(PDB_SAVE_OR_DISCARD_STATE).define(
                b.firstOf(SAVE, DISCARD), STATE, b.optional(PDB_INSTANCES_CLAUSE))

            val pdbNames = b.sequence(IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME))
            val state = b.firstOf(PDB_OPEN, PDB_CLOSE, PDB_SAVE_OR_DISCARD_STATE)
            // The target may itself be named OPEN, CLOSE, SAVE or DISCARD, so the named form is tried first.
            b.rule(PDB_CHANGE_STATE).define(
                b.firstOf(
                    b.sequence(b.firstOf(b.sequence(ALL, b.optional(EXCEPT, pdbNames)), pdbNames), state),
                    state))

            // The diagram quotes the transport secret, but Oracle takes an identifier.
            b.rule(PDB_UNPLUG_CLAUSE).define(
                IDENTIFIER_NAME, UNPLUG, INTO, CHARACTER_LITERAL,
                b.optional(ENCRYPT, USING, IDENTIFIER_NAME))

            // The diagram and text disagree on the redundancy names; Oracle takes UNPROTECTED, MIRROR or HIGH.
            b.rule(PREPARE_CLAUSE).define(
                PREPARE, MIRROR, COPY, IDENTIFIER_NAME,
                b.optional(WITH, b.firstOf(UNPROTECTED, MIRROR, HIGH), REDUNDANCY))
            b.rule(DROP_MIRROR_COPY).define(DROP, MIRROR, COPY, IDENTIFIER_NAME)
            val pdbMirrorCopy = b.firstOf(
                b.sequence(PREPARE_CLAUSE, b.optional(FOR, DATABASE, IDENTIFIER_NAME)),
                DROP_MIRROR_COPY)

            b.rule(PDB_CONTAINERS_CLAUSE).define(
                CONTAINERS,
                b.firstOf(
                    b.sequence(DEFAULT, TARGET, EQUALS, b.firstOf(NONE, b.sequence(LPARENTHESIS, IDENTIFIER_NAME, RPARENTHESIS))),
                    b.sequence(HOST, EQUALS, CHARACTER_LITERAL),
                    b.sequence(PORT, EQUALS, b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL))))

            val applicationNames = b.sequence(IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME))
            b.rule(PDB_APPLICATION_SYNC_CLAUSE).define(
                APPLICATION,
                b.firstOf(
                    b.sequence(ALL, b.optional(EXCEPT, applicationNames), SYNC),
                    b.sequence(
                        IDENTIFIER_NAME, SYNC, TO,
                        b.firstOf(CHARACTER_LITERAL, b.sequence(PATCH, b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)))),
                    b.sequence(applicationNames, SYNC)))

            b.rule(ALTER_PLUGGABLE_DATABASE).define(
                ALTER, PLUGGABLE, DATABASE,
                b.firstOf(
                    b.sequence(PDB_CHANGE_STATE, b.next(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.sequence(
                        b.firstOf(LOST_WRITE_PROTECTION, b.sequence(IDENTIFIER_NAME, LOST_WRITE_PROTECTION)),
                        b.next(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.sequence(
                        b.firstOf(pdbMirrorCopy, b.sequence(IDENTIFIER_NAME, pdbMirrorCopy)),
                        b.next(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.sequence(PDB_UNPLUG_CLAUSE, b.next(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.sequence(
                        b.firstOf(b.sequence(IDENTIFIER_NAME, PDB_CONTAINERS_CLAUSE), PDB_CONTAINERS_CLAUSE),
                        b.next(b.firstOf(SEMICOLON, DIVISION, EOF))),
                    b.sequence(PDB_APPLICATION_SYNC_CLAUSE, b.next(b.firstOf(SEMICOLON, DIVISION, EOF)))),
                b.optional(SEMICOLON))
        }

        private fun lostWriteAction(b: PlSqlGrammarBuilder) =
            b.sequence(b.firstOf(ENABLE, REMOVE, SUSPEND), LOST, WRITE, PROTECTION)

        // Oracle takes the options in any order and rejects repeats only afterwards (ORA-12990). REFRESH MODE and
        // RELOCATE need a database link, and the Free runtime rejects ENABLE SNAPSHOT, FILESYSTEM_LIKE_LOGGING and
        // USING SNAPSHOT AT <timestamp>, so those are not modelled.
        private fun createPluggableDatabase(b: PlSqlGrammarBuilder) {
            val number = b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)
            val literalList = b.sequence(
                LPARENTHESIS, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL), RPARENTHESIS)
            val literalPair = b.sequence(CHARACTER_LITERAL, COMMA, CHARACTER_LITERAL)
            val pairList = b.sequence(LPARENTHESIS, literalPair, b.zeroOrMore(COMMA, literalPair), RPARENTHESIS)
            val allOrList = b.firstOf(NONE, b.sequence(ALL, b.optional(EXCEPT, literalList)), literalList)

            b.rule(PDB_FILE_NAME_CONVERT).define(FILE_NAME_CONVERT, EQUALS, b.firstOf(NONE, pairList))
            b.rule(PDB_SERVICE_NAME_CONVERT).define(SERVICE_NAME_CONVERT, EQUALS, b.firstOf(NONE, pairList))
            b.rule(PDB_SOURCE_FILE_NAME_CONVERT).define(SOURCE_FILE_NAME_CONVERT, EQUALS, b.firstOf(NONE, pairList))
            b.rule(PDB_SOURCE_FILE_DIRECTORY).define(SOURCE_FILE_DIRECTORY, EQUALS, b.firstOf(NONE, CHARACTER_LITERAL))
            b.rule(PDB_PATH_PREFIX).define(PATH_PREFIX, EQUALS, b.firstOf(NONE, CHARACTER_LITERAL, IDENTIFIER_NAME))
            b.rule(PDB_TEMPFILE_REUSE).define(TEMPFILE, REUSE)
            b.rule(PDB_STANDBYS).define(STANDBYS, EQUALS, allOrList)
            b.rule(PDB_USER_TABLESPACES).define(
                USER_TABLESPACES, EQUALS, allOrList,
                b.optional(b.firstOf(b.sequence(SNAPSHOT, COPY), b.sequence(NO, DATA))))
            b.rule(PDB_LOGGING).define(b.firstOf(LOGGING, NOLOGGING))
            b.rule(PDB_CREATE_FILE_DEST).define(CREATE_FILE_DEST, EQUALS, b.firstOf(NONE, CHARACTER_LITERAL, IDENTIFIER_NAME))
            b.rule(PDB_STORAGE_CLAUSE).define(
                STORAGE,
                b.firstOf(
                    UNLIMITED,
                    b.sequence(
                        LPARENTHESIS,
                        b.oneOrMore(
                            b.firstOf(MAXSIZE, MAX_AUDIT_SIZE, MAX_DIAG_SIZE),
                            b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE)),
                        RPARENTHESIS)))
            b.rule(PDB_DEFAULT_TABLESPACE).define(DEFAULT, TABLESPACE, IDENTIFIER_NAME)
            b.rule(PDB_DEFAULT_TABLESPACE_FILES).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(DATAFILE, DATAFILE_TEMPFILE_SPEC, b.zeroOrMore(COMMA, DATAFILE_TEMPFILE_SPEC)),
                    EXTENT_MANAGEMENT_CLAUSE)))
            b.rule(PDB_ROLES_CLAUSE).define(
                ROLES, EQUALS, LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)
            b.rule(PDB_KEYSTORE_CLAUSE).define(
                KEYSTORE, KEYSTORE_IDENTIFIED_BY,
                b.optional(b.firstOf(b.sequence(NO, REKEY), b.sequence(REKEY, USING, CHARACTER_LITERAL))))
            b.rule(PDB_REFRESH_MODE_CLAUSE).define(
                REFRESH, MODE,
                b.firstOf(MANUAL, b.sequence(EVERY, number, b.firstOf(MINUTES, HOURS)), NONE))
            b.rule(PDB_RELOCATE_CLAUSE).define(
                RELOCATE,
                b.optional(KEEP, SOURCE),
                b.optional(AVAILABILITY, b.firstOf(MAX, NORMAL)),
                b.optional(PDB_REFRESH_MODE_CLAUSE))
            b.rule(PDB_USING_SNAPSHOT).define(
                USING, SNAPSHOT, b.firstOf(b.sequence(AT, SCN, number), IDENTIFIER_NAME))
            b.rule(PDB_DECRYPT_CLAUSE).define(DECRYPT, USING, IDENTIFIER_NAME)

            val commonWithoutFileNameConvert = b.firstOf(
                b.sequence(PARALLEL, b.optional(number)),
                PDB_STORAGE_CLAUSE, PDB_SERVICE_NAME_CONVERT, PDB_PATH_PREFIX,
                PDB_TEMPFILE_REUSE, PDB_USER_TABLESPACES, PDB_STANDBYS, PDB_LOGGING, PDB_CREATE_FILE_DEST,
                b.sequence(HOST, EQUALS, CHARACTER_LITERAL),
                b.sequence(PORT, EQUALS, number))
            val commonOptions = b.firstOf(commonWithoutFileNameConvert, PDB_FILE_NAME_CONVERT)

            b.rule(PDB_FROM_SEED).define(
                ADMIN, USER, IDENTIFIER_NAME, KEYSTORE_PASSWORD_IDENTIFIED_BY,
                b.optional(PDB_ROLES_CLAUSE),
                b.zeroOrMore(b.firstOf(
                    commonOptions,
                    b.sequence(PDB_DEFAULT_TABLESPACE, b.optional(PDB_DEFAULT_TABLESPACE_FILES)))))

            val dblink = b.sequence(REMOTE, IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME))
            val cloneOptions = b.firstOf(
                commonOptions, PDB_DEFAULT_TABLESPACE, b.sequence(SNAPSHOT, COPY), b.sequence(NO, DATA),
                PDB_KEYSTORE_CLAUSE)
            b.rule(PDB_CLONE).define(
                b.firstOf(
                    b.sequence(
                        b.optional(AS, PROXY), FROM, IDENTIFIER_NAME, dblink, b.optional(PDB_USING_SNAPSHOT),
                        b.zeroOrMore(b.firstOf(cloneOptions, PDB_REFRESH_MODE_CLAUSE, PDB_RELOCATE_CLAUSE))),
                    b.sequence(
                        b.optional(AS, PROXY), FROM, IDENTIFIER_NAME, b.optional(PDB_USING_SNAPSHOT),
                        b.zeroOrMore(cloneOptions))))

            // COPY, MOVE and NOCOPY share one slot (mixing them is a syntax error), and NOCOPY also rejects a
            // FILE_NAME_CONVERT with file names in either order. FILE_NAME_CONVERT = NONE may precede NOCOPY.
            val xmlOptions = b.firstOf(
                commonWithoutFileNameConvert, PDB_DEFAULT_TABLESPACE, PDB_SOURCE_FILE_NAME_CONVERT,
                PDB_SOURCE_FILE_DIRECTORY, PDB_DECRYPT_CLAUSE)
            val xmlOptionsWithFileNameConvert = b.firstOf(xmlOptions, PDB_FILE_NAME_CONVERT)
            val fileNameConvertNone = b.sequence(b.next(FILE_NAME_CONVERT, EQUALS, NONE), PDB_FILE_NAME_CONVERT)
            b.rule(PDB_FROM_XML).define(
                b.optional(AS, CLONE), USING, CHARACTER_LITERAL,
                b.firstOf(
                    b.sequence(
                        b.zeroOrMore(xmlOptionsWithFileNameConvert), COPY,
                        b.zeroOrMore(b.firstOf(xmlOptionsWithFileNameConvert, COPY))),
                    b.sequence(
                        b.zeroOrMore(xmlOptionsWithFileNameConvert), MOVE,
                        b.zeroOrMore(b.firstOf(xmlOptionsWithFileNameConvert, MOVE))),
                    b.sequence(
                        b.zeroOrMore(b.firstOf(xmlOptions, fileNameConvertNone)), NOCOPY,
                        b.zeroOrMore(b.firstOf(xmlOptions, NOCOPY))),
                    b.zeroOrMore(xmlOptionsWithFileNameConvert)))

            b.rule(CREATE_PLUGGABLE_DATABASE).define(
                CREATE, PLUGGABLE, DATABASE, IDENTIFIER_NAME,
                b.firstOf(PDB_FROM_SEED, PDB_CLONE, PDB_FROM_XML),
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DATABASE.html
        private fun databaseFileClauses(b: PlSqlGrammarBuilder) {
            val fileNumber = b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)
            val fileReference = b.firstOf(CHARACTER_LITERAL, fileNumber)
            val fileReferences = b.sequence(fileReference, b.zeroOrMore(COMMA, fileReference))
            val tempfileReferences = b.firstOf(
                b.sequence(CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL)),
                b.sequence(fileNumber, b.zeroOrMore(COMMA, fileNumber)))

            // Oracle accepts AUTOEXTEND here despite the documented restriction.
            val destinations = b.sequence(DATAFILE_TEMPFILE_SPEC, b.zeroOrMore(COMMA, DATAFILE_TEMPFILE_SPEC))
            b.rule(CREATE_DATAFILE_CLAUSE).define(
                CREATE, DATAFILE, fileReferences,
                b.optional(AS, b.firstOf(NEW, destinations)))

            val resize = b.sequence(RESIZE, INDEX_SIZE_CLAUSE)
            b.rule(ALTER_DATAFILE_CLAUSE).define(
                DATAFILE, fileReferences,
                b.firstOf(
                    ONLINE,
                    b.sequence(OFFLINE, b.optional(FOR, DROP)),
                    resize,
                    AUTOEXTEND_CLAUSE,
                    b.sequence(END, BACKUP),
                    ENCRYPT,
                    DECRYPT))
            b.rule(ALTER_TEMPFILE_CLAUSE).define(
                TEMPFILE, tempfileReferences,
                b.firstOf(
                    resize,
                    AUTOEXTEND_CLAUSE,
                    b.sequence(DROP, b.optional(INCLUDING, DATAFILES)),
                    ONLINE,
                    OFFLINE))

            // Oracle also accepts KEEP before REUSE, unlike the diagram.
            b.rule(MOVE_DATAFILE_CLAUSE).define(
                MOVE, DATAFILE, b.firstOf(CHARACTER_LITERAL, fileNumber),
                b.optional(TO, CHARACTER_LITERAL),
                b.anyOrder(REUSE, KEEP))
            b.rule(DATABASE_FILE_CLAUSES).define(
                b.firstOf(
                    CREATE_DATAFILE_CLAUSE,
                    ALTER_DATAFILE_CLAUSE,
                    ALTER_TEMPFILE_CLAUSE,
                    MOVE_DATAFILE_CLAUSE,
                    b.sequence(b.next(RENAME), LOGFILE_CLAUSES)))

            // The documented unquoted `td_file.df` is rejected by Oracle (ORA-02236), as for ordinary DATAFILE.
            b.rule(LOST_WRITE_PROTECTION).define(
                b.firstOf(
                    b.sequence(b.firstOf(ENABLE, DISABLE), LOST, WRITE, PROTECTION),
                    b.sequence(DATAFILE, fileReferences, lostWriteAction(b))))
        }

        private fun alterDatabase(b: PlSqlGrammarBuilder) {
            fun literals() = b.sequence(CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL))
            val literalList = b.sequence(LPARENTHESIS, literals(), RPARENTHESIS)

            b.rule(LOGFILE_DESCRIPTOR).define(
                b.firstOf(b.sequence(GROUP, INTEGER_LITERAL), literalList, CHARACTER_LITERAL))
            val descriptors = b.sequence(LOGFILE_DESCRIPTOR, b.zeroOrMore(COMMA, LOGFILE_DESCRIPTOR))

            b.rule(REDO_LOG_FILE_SPEC).define(
                b.optional(b.firstOf(CHARACTER_LITERAL, literalList)),
                b.optional(SIZE, INDEX_SIZE_CLAUSE),
                b.optional(BLOCKSIZE, INDEX_SIZE_CLAUSE),
                b.optional(REUSE))

            val group = b.sequence(b.optional(GROUP, INTEGER_LITERAL), REDO_LOG_FILE_SPEC)
            // Oracle 26 rejects REUSE before a later member (ORA-00946) and a second TO descriptor
            // (ORA-02236/ORA-00946), unlike the diagram.
            b.rule(ADD_LOGFILE_CLAUSES).define(
                ADD, b.optional(STANDBY), LOGFILE,
                b.firstOf(
                    b.sequence(MEMBER, literals(), b.optional(REUSE), TO, LOGFILE_DESCRIPTOR),
                    b.sequence(
                        b.optional(b.firstOf(
                            b.sequence(INSTANCE, CHARACTER_LITERAL),
                            b.sequence(THREAD, INTEGER_LITERAL))),
                        group,
                        b.zeroOrMore(
                            COMMA, b.next(b.firstOf(GROUP, CHARACTER_LITERAL, LPARENTHESIS, SIZE, BLOCKSIZE, REUSE)),
                            group))))

            b.rule(DROP_LOGFILE_CLAUSES).define(
                DROP, b.optional(STANDBY), LOGFILE,
                b.firstOf(b.sequence(MEMBER, literals()), descriptors))

            b.rule(SUPPLEMENTAL_DB_LOGGING).define(
                b.firstOf(ADD, DROP), SUPPLEMENTAL, LOG,
                b.firstOf(
                    SUPPLEMENTAL_ID_KEY_CLAUSE,
                    b.sequence(DATA, b.optional(b.firstOf(
                        b.sequence(FOR, PROCEDURAL, REPLICATION),
                        b.sequence(SUBSET, DATABASE, REPLICATION))))))

            // Oracle 26 accepts a list of targets with the same count as the sources (ORA-02238 otherwise).
            b.rule(LOGFILE_CLAUSES).define(
                b.firstOf(
                    b.sequence(ARCHIVELOG, b.optional(MANUAL)),
                    NOARCHIVELOG,
                    b.sequence(b.optional(NO), FORCE, LOGGING),
                    b.sequence(
                        SET, STANDBY, NOLOGGING, FOR,
                        b.firstOf(b.sequence(DATA, AVAILABILITY), b.sequence(LOAD, PERFORMANCE))),
                    b.sequence(RENAME, FILE, literals(), TO, literals()),
                    b.sequence(
                        CLEAR, b.optional(UNARCHIVED), LOGFILE, descriptors,
                        b.optional(UNRECOVERABLE, DATAFILE)),
                    ADD_LOGFILE_CLAUSES,
                    DROP_LOGFILE_CLAUSES,
                    b.sequence(SWITCH, ALL, LOGFILES, TO, BLOCKSIZE, INTEGER_LITERAL, b.optional("K")),
                    SUPPLEMENTAL_DB_LOGGING))

            databaseFileClauses(b)

            // Oracle also accepts DATABASE left out, even a bare RECOVER.
            val recoveryFileNumber = b.firstOf(INTEGER_LITERAL, NUMBER_LITERAL)
            val recoveryFile = b.firstOf(CHARACTER_LITERAL, recoveryFileNumber)
            val fullRecoveryOption = b.firstOf(
                b.sequence(
                    UNTIL,
                    b.firstOf(
                        CANCEL, CONSISTENT,
                        b.sequence(TIME, CHARACTER_LITERAL),
                        b.sequence(CHANGE, INTEGER_LITERAL))),
                b.sequence(USING, BACKUP, CONTROLFILE))
            val parallelClause = b.firstOf(NOPARALLEL, b.sequence(PARALLEL, b.optional(INTEGER_LITERAL)))
            b.rule(GENERAL_RECOVERY).define(
                RECOVER,
                b.optional(AUTOMATIC),
                b.optional(FROM, CHARACTER_LITERAL),
                b.firstOf(
                    b.sequence(CONTINUE, b.optional(DEFAULT)),
                    CANCEL,
                    b.sequence(
                        b.firstOf(
                            b.sequence(TABLESPACE, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME)),
                            b.sequence(DATAFILE, recoveryFile, b.zeroOrMore(COMMA, recoveryFile)),
                            b.sequence(LOGFILE, CHARACTER_LITERAL),
                            b.sequence(b.firstOf(b.sequence(STANDBY, DATABASE), DATABASE), b.zeroOrMore(fullRecoveryOption)),
                            b.zeroOrMore(fullRecoveryOption)),
                        b.zeroOrMore(b.firstOf(
                            TEST,
                            b.sequence(ALLOW, INTEGER_LITERAL, CORRUPTION),
                            parallelClause)))))

            // FINISH FORCE/WAIT/NOWAIT, CANCEL IMMEDIATE/WAIT/NOWAIT and USING CURRENT LOGFILE are deprecated but
            // documented. Oracle rejects WAIT with NOWAIT and repeats of either option on CANCEL (ORA-00274).
            b.rule(MANAGED_STANDBY_RECOVERY).define(
                RECOVER, MANAGED, STANDBY, DATABASE,
                b.optional(b.firstOf(
                    b.sequence(FINISH, b.optional(b.firstOf(FORCE, WAIT, NOWAIT))),
                    b.sequence(
                        CANCEL,
                        b.optional(b.firstOf(
                            b.sequence(IMMEDIATE, b.optional(b.firstOf(WAIT, NOWAIT))),
                            b.sequence(b.firstOf(WAIT, NOWAIT), b.optional(IMMEDIATE))))),
                    b.oneOrMore(b.firstOf(
                        b.sequence(USING, b.firstOf(ARCHIVED, CURRENT), LOGFILE),
                        b.sequence(DISCONNECT, b.optional(FROM, SESSION)),
                        NODELAY,
                        b.sequence(UNTIL, b.firstOf(b.sequence(CHANGE, INTEGER_LITERAL), CONSISTENT)),
                        b.sequence(USING, INSTANCES, b.firstOf(ALL, INTEGER_LITERAL)),
                        parallelClause)))))

            b.rule(RECOVER_TO_LOGICAL_STANDBY).define(
                RECOVER, TO, LOGICAL, STANDBY,
                b.firstOf(b.sequence(KEEP, IDENTITY), b.sequence(b.nextNot(KEEP), IDENTIFIER_NAME)))

            // Oracle never takes a clause keyword or LINK as the database name (`ADD ADD LOGFILE` fails).
            val notName = b.firstOf(
                ARCHIVELOG, NOARCHIVELOG, NO, FORCE, SET, RENAME, CLEAR, ADD, DROP, SWITCH, LINK,
                CREATE, DATAFILE, TEMPFILE, MOVE, ENABLE, DISABLE, RECOVER, PREPARE, MOUNT, OPEN)
            b.rule(DEFAULT_TABLESPACE_SETTINGS).define(
                b.firstOf(
                    b.sequence(SET, DEFAULT, b.firstOf(BIGFILE, SMALLFILE), TABLESPACE),
                    b.sequence(DEFAULT, b.optional(b.optional(LOCAL), TEMPORARY), TABLESPACE, IDENTIFIER_NAME)))

            b.rule(STARTUP_CLAUSES).define(
                b.firstOf(
                    b.sequence(MOUNT, b.optional(b.firstOf(STANDBY, CLONE), DATABASE)),
                    b.sequence(
                        OPEN,
                        b.firstOf(
                            b.sequence(READ, ONLY),
                            b.sequence(
                                b.optional(READ, WRITE),
                                b.optional(b.firstOf(RESETLOGS, NORESETLOGS)),
                                b.optional(b.firstOf(UPGRADE, DOWNGRADE)))))))

            b.rule(RENAME_GLOBAL_NAME_CLAUSE).define(
                RENAME, GLOBAL_NAME, TO, IDENTIFIER_NAME, b.zeroOrMore(DOT, IDENTIFIER_NAME))

            // Oracle also recognises USING after DISABLE but rejects it only afterwards (ORA-19768), so DISABLE
            // takes no options here.
            b.rule(BLOCK_CHANGE_TRACKING_CLAUSE).define(
                b.firstOf(
                    b.sequence(ENABLE, BLOCK, CHANGE, TRACKING, b.optional(USING, FILE, CHARACTER_LITERAL, b.optional(REUSE))),
                    b.sequence(DISABLE, BLOCK, CHANGE, TRACKING)))

            b.rule(ALTER_DATABASE).define(
                ALTER, DATABASE,
                b.optional(b.nextNot(notName), IDENTIFIER_NAME),
                b.firstOf(
                    DATABASE_FILE_CLAUSES, LOST_WRITE_PROTECTION, MANAGED_STANDBY_RECOVERY, RECOVER_TO_LOGICAL_STANDBY,
                    GENERAL_RECOVERY, PREPARE_CLAUSE, DROP_MIRROR_COPY,
                    DEFAULT_TABLESPACE_SETTINGS, STARTUP_CLAUSES, RENAME_GLOBAL_NAME_CLAUSE,
                    BLOCK_CHANGE_TRACKING_CLAUSE, LOGFILE_CLAUSES),
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-DISKGROUP.html
        private fun alterDiskgroup(b: PlSqlGrammarBuilder) {
            val forceState = b.firstOf(FORCE, NOFORCE)
            val diskKind = b.firstOf(QUORUM, REGULAR)

            b.rule(QUALIFIED_DISK_CLAUSE).define(
                CHARACTER_LITERAL,
                b.optional(NAME, IDENTIFIER_NAME),
                b.optional(SIZE, INDEX_SIZE_CLAUSE),
                b.optional(forceState))

            // The diagram's repeated group takes no ADD keyword, and a comma only continues the disk list.
            val addGroup = b.sequence(
                b.optional(SITE, IDENTIFIER_NAME),
                b.optional(diskKind),
                b.optional(FAILGROUP, IDENTIFIER_NAME),
                DISK, QUALIFIED_DISK_CLAUSE, b.zeroOrMore(COMMA, QUALIFIED_DISK_CLAUSE))
            b.rule(ADD_DISK_CLAUSE).define(ADD, b.oneOrMore(addGroup))

            val dropTarget = b.sequence(b.nextNot(b.firstOf(ADD, DROP)), IDENTIFIER_NAME, b.optional(forceState))
            val dropTargets = b.sequence(dropTarget, b.zeroOrMore(COMMA, dropTarget))
            b.rule(DROP_DISK_CLAUSE).define(
                DROP,
                b.firstOf(
                    b.sequence(b.optional(diskKind), DISK, dropTargets),
                    b.sequence(DISKS, IN, b.optional(diskKind), FAILGROUP, dropTargets)))

            b.rule(UNDROP_DISK_CLAUSE).define(UNDROP, DISKS)
            b.rule(RESIZE_DISK_CLAUSE).define(RESIZE, ALL, b.optional(SIZE, INDEX_SIZE_CLAUSE))

            val phase = b.firstOf(RESTORE, BALANCE, PREPARE, COMPACT)
            b.rule(REBALANCE_DISKGROUP_CLAUSE).define(
                REBALANCE,
                b.firstOf(
                    b.sequence(MODIFY, POWER, b.optional(INTEGER_LITERAL)),
                    b.sequence(
                        b.optional(b.firstOf(WITH, WITHOUT), phase, b.zeroOrMore(b.optional(COMMA), phase)),
                        b.optional(POWER, INTEGER_LITERAL),
                        b.optional(b.firstOf(WAIT, NOWAIT)))))

            // Oracle takes a comma between items only after a DROP item (a comma after an ADD item continues its
            // disk list) and also accepts items with no comma at all.
            val diskItem = b.firstOf(
                ADD_DISK_CLAUSE,
                b.sequence(DROP_DISK_CLAUSE, b.optional(COMMA, b.next(b.firstOf(ADD, DROP)))))
            val addDropItems = b.oneOrMore(diskItem)

            b.rule(DISKGROUP_AVAILABILITY).define(
                b.firstOf(
                    b.sequence(MOUNT, b.optional(b.firstOf(RESTRICTED, NORMAL)), b.optional(forceState)),
                    b.sequence(DISMOUNT, b.optional(forceState))))

            b.rule(CHECK_DISKGROUP_CLAUSE).define(
                CHECK,
                b.optional(b.firstOf(
                    ALL,
                    b.sequence(DISK, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME)),
                    b.sequence(FILE, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL)))),
                b.optional(b.firstOf(REPAIR, NOREPAIR)))

            // Oracle also accepts the singular ATTRIBUTE, any attribute order, an empty list and items without commas.
            val templateAttributes = b.sequence(
                b.firstOf(ATTRIBUTE, ATTRIBUTES), LPARENTHESIS,
                b.zeroOrMore(b.firstOf(MIRROR, HIGH, UNPROTECTED, PARITY, DOUBLE, FINE, COARSE)),
                RPARENTHESIS)
            val templateWithAttributes = b.sequence(IDENTIFIER_NAME, templateAttributes)
            b.rule(DISKGROUP_TEMPLATE_CLAUSES).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(
                        b.firstOf(ADD, MODIFY, ALTER), TEMPLATE,
                        templateWithAttributes, b.zeroOrMore(COMMA, templateWithAttributes)),
                    b.sequence(DROP, TEMPLATE, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME)))))

            val literalList = b.sequence(CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL))
            fun literalPairs(joiner: PlSqlKeyword): Any {
                val pair = b.sequence(CHARACTER_LITERAL, joiner, CHARACTER_LITERAL)
                return b.sequence(pair, b.zeroOrMore(COMMA, pair))
            }
            val directoryDrop = b.sequence(CHARACTER_LITERAL, b.optional(forceState))
            b.rule(DISKGROUP_DIRECTORY_CLAUSES).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(ADD, DIRECTORY, literalList),
                    b.sequence(DROP, DIRECTORY, directoryDrop, b.zeroOrMore(COMMA, directoryDrop)),
                    b.sequence(RENAME, DIRECTORY, literalPairs(TO)))))
            b.rule(DISKGROUP_ALIAS_CLAUSES).define(
                b.oneOrMore(b.firstOf(
                    b.sequence(ADD, ALIAS, literalPairs(FOR)),
                    b.sequence(DROP, ALIAS, literalList),
                    b.sequence(RENAME, ALIAS, literalPairs(TO)))))

            b.rule(SCRUB_CLAUSE).define(
                SCRUB,
                b.firstOf(
                    STOP,
                    b.sequence(
                        b.optional(b.firstOf(
                            b.sequence(FILE, CHARACTER_LITERAL),
                            b.sequence(DISK, IDENTIFIER_NAME))),
                        b.optional(b.firstOf(REPAIR, NOREPAIR)),
                        b.optional(POWER, b.firstOf(AUTO, LOW, HIGH, MAX)),
                        b.optional(b.firstOf(WAIT, NOWAIT)),
                        b.optional(forceState))))

            // Oracle accepts FROM TEMPLATE after every client kind, although the diagram lists it only after TEMPLATE.
            val filegroupProperty = b.sequence(CHARACTER_LITERAL, EQUALS, CHARACTER_LITERAL)
            val clientName = b.firstOf(IDENTIFIER_NAME, CHARACTER_LITERAL)
            b.rule(FILEGROUP_PROPERTY).define(SET, filegroupProperty, b.zeroOrMore(COMMA, filegroupProperty))

            b.rule(ADD_FILEGROUP_CLAUSE).define(
                ADD, FILEGROUP, IDENTIFIER_NAME,
                b.firstOf(
                    b.sequence(b.firstOf(DATABASE, CLUSTER), clientName),
                    b.sequence(VOLUME, IDENTIFIER_NAME),
                    TEMPLATE),
                b.optional(FROM, TEMPLATE, IDENTIFIER_NAME),
                b.optional(FILEGROUP_PROPERTY))

            b.rule(MODIFY_FILEGROUP_CLAUSE).define(MODIFY, FILEGROUP, IDENTIFIER_NAME, FILEGROUP_PROPERTY)

            b.rule(MOVE_TO_FILEGROUP_CLAUSE).define(MOVE, FILE, CHARACTER_LITERAL, TO, FILEGROUP, IDENTIFIER_NAME)

            b.rule(DROP_FILEGROUP_CLAUSE).define(
                DROP, FILEGROUP, IDENTIFIER_NAME,
                b.optional(CASCADE),
                b.optional(FOR, b.optional(PLUGGABLE, DATABASE, IDENTIFIER_NAME), DATABASE, IDENTIFIER_NAME))

            // Oracle parses runs of actions (and comma-separated properties) and rejects the combination only afterwards
            // (ORA-15116, ORA-15396).
            val filegroupAction = b.firstOf(
                ADD_FILEGROUP_CLAUSE, MODIFY_FILEGROUP_CLAUSE, MOVE_TO_FILEGROUP_CLAUSE, DROP_FILEGROUP_CLAUSE)
            b.rule(DISKGROUP_FILEGROUP_CLAUSE).define(
                b.zeroOrMore(diskItem),
                filegroupAction,
                b.zeroOrMore(b.firstOf(diskItem, filegroupAction)))

            b.rule(ALTER_DISKGROUP).define(
                ALTER, DISKGROUP,
                b.firstOf(
                    b.sequence(ALL, b.firstOf(UNDROP_DISK_CLAUSE, DISKGROUP_AVAILABILITY)),
                    b.sequence(
                        IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME),
                        b.firstOf(UNDROP_DISK_CLAUSE, DISKGROUP_AVAILABILITY)),
                    b.sequence(
                        IDENTIFIER_NAME,
                        b.firstOf(
                            DISKGROUP_FILEGROUP_CLAUSE,
                            b.sequence(
                                b.firstOf(addDropItems, RESIZE_DISK_CLAUSE),
                                b.optional(REBALANCE_DISKGROUP_CLAUSE)),
                            REBALANCE_DISKGROUP_CLAUSE,
                            CHECK_DISKGROUP_CLAUSE,
                            DISKGROUP_TEMPLATE_CLAUSES,
                            DISKGROUP_DIRECTORY_CLAUSES,
                            DISKGROUP_ALIAS_CLAUSES,
                            SCRUB_CLAUSE))),
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-ROLE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-ROLE.html
        private fun createRole(b: PlSqlGrammarBuilder) {
            // Unlike USER_AUTHENTICATION_CLAUSE there is no NO AUTHENTICATION, AND FACTOR, DIGEST or
            // EXTERNALLY AS here (ORA-00922), so the user rule is not reused.
            b.rule(ROLE_IDENTIFICATION_CLAUSE).define(
                b.firstOf(
                    b.sequence(NOT, IDENTIFIED),
                    b.sequence(
                        IDENTIFIED,
                        b.firstOf(
                            b.sequence(BY, IDENTIFIER_NAME),
                            b.sequence(USING, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)),
                            EXTERNALLY,
                            b.sequence(GLOBALLY, b.optional(AS, CHARACTER_LITERAL))))))

            // Oracle 26 accepts CONTAINER before or after the identification clause. A second clause of
            // either kind fails after parsing (ORA-01944/ORA-65022), so repeats are not tracked.
            val roleOption = b.firstOf(ROLE_IDENTIFICATION_CLAUSE, b.sequence(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)))

            b.rule(CREATE_ROLE).define(
                CREATE, ROLE, b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME, b.zeroOrMore(roleOption),
                b.optional(SEMICOLON))

            b.rule(ALTER_ROLE).define(
                ALTER, ROLE, b.optional(IF, EXISTS), IDENTIFIER_NAME, b.oneOrMore(roleOption),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLESPACE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLESPACE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-TABLESPACE-SET.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-TABLESPACE-SET.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/file_specification.html
        private fun createTablespace(b: PlSqlGrammarBuilder) {
            val fileNameOrNumber = b.firstOf(CHARACTER_LITERAL, INTEGER_LITERAL)
            val keepSize = b.optional(KEEP, INDEX_SIZE_CLAUSE)

            // Oracle 26 keeps these parts in the documented order (ORA-02180 for REUSE before SIZE, or NEXT after
            // MAXSIZE). The file name is optional for Oracle Managed Files.
            b.rule(AUTOEXTEND_CLAUSE).define(
                AUTOEXTEND,
                b.firstOf(
                    OFF,
                    b.sequence(
                        ON,
                        b.optional(NEXT, INDEX_SIZE_CLAUSE),
                        b.optional(MAXSIZE, b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE)))))

            b.rule(DATAFILE_TEMPFILE_SPEC).define(
                b.optional(CHARACTER_LITERAL),
                b.optional(SIZE, INDEX_SIZE_CLAUSE),
                b.optional(REUSE),
                b.optional(AUTOEXTEND_CLAUSE))

            val fileSpecifications = b.sequence(DATAFILE_TEMPFILE_SPEC, b.zeroOrMore(COMMA, DATAFILE_TEMPFILE_SPEC))

            // Oracle 26 also parses EXTENT MANAGEMENT DICTIONARY, which the diagram omits.
            b.rule(EXTENT_MANAGEMENT_CLAUSE).define(
                EXTENT, MANAGEMENT,
                b.firstOf(
                    b.sequence(LOCAL, b.optional(b.firstOf(
                        AUTOALLOCATE,
                        b.sequence(UNIFORM, b.optional(SIZE, INDEX_SIZE_CLAUSE))))),
                    DICTIONARY))

            // Oracle 26 accepts the encryption spec without MODE, and a bare ENCRYPTION.
            val encryptionSpec = b.sequence(USING, CHARACTER_LITERAL, b.optional(MODE, CHARACTER_LITERAL))

            b.rule(TABLESPACE_ENCRYPTION_CLAUSE).define(
                ENCRYPTION, b.optional(encryptionSpec), b.optional(b.firstOf(ENCRYPT, DECRYPT)))

            // The TABLE keyword is optional: Oracle 26 still accepts `DEFAULT COMPRESS FOR OLTP`.
            val tableCompression = b.sequence(
                b.optional(TABLE),
                b.firstOf(
                    b.sequence(COMPRESS, b.optional(FOR, b.firstOf(
                        OLTP,
                        b.sequence(b.firstOf(QUERY, ARCHIVE), b.firstOf(LOW, HIGH))))),
                    NOCOMPRESS))

            // Tablespace defaults exclude index prefix counts and index-only storage options.
            b.rule(TABLESPACE_STORAGE_CLAUSE).define(
                STORAGE, LPARENTHESIS,
                b.oneOrMore(b.firstOf(
                    ENCRYPT,
                    b.sequence(INITIAL, INDEX_SIZE_CLAUSE),
                    b.sequence(NEXT, INDEX_SIZE_CLAUSE),
                    b.sequence(MINEXTENTS, INTEGER_LITERAL),
                    b.sequence(MAXEXTENTS, b.firstOf(INTEGER_LITERAL, UNLIMITED)),
                    b.sequence(MAXSIZE, b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE)),
                    b.sequence(PCTINCREASE, INTEGER_LITERAL))),
                RPARENTHESIS).skip()

            // The tablespace distribution variant has ROWID RANGE, not PARTITION/SUBPARTITION.
            // Free 23.26.3 rejects MEMCOMPRESS AUTO, but the current Oracle 26 production includes it.
            b.rule(TABLESPACE_MEMCOMPRESS).define(
                b.firstOf(
                    b.sequence(MEMCOMPRESS, FOR, b.firstOf(
                        DML,
                        b.sequence(QUERY, b.optional(b.firstOf(LOW, HIGH))),
                        b.sequence(CAPACITY, b.optional(b.firstOf(LOW, HIGH))))),
                    b.sequence(MEMCOMPRESS, AUTO),
                    b.sequence(NO, MEMCOMPRESS))).skip()
            b.rule(TABLESPACE_INMEMORY_ATTRIBUTES).define(
                b.zeroOrMore(b.firstOf(
                    TABLESPACE_MEMCOMPRESS,
                    b.sequence(PRIORITY, b.firstOf(NONE, LOW, MEDIUM, HIGH, CRITICAL)),
                    b.sequence(DISTRIBUTE,
                        b.optional(b.firstOf(AUTO, b.sequence(BY, ROWID, RANGE_KEYWORD))),
                        b.optional(FOR, SERVICE, b.firstOf(DEFAULT, ALL, NONE, IDENTIFIER_NAME))),
                    b.sequence(DUPLICATE, b.optional(ALL)),
                    b.sequence(NO, DUPLICATE),
                    b.sequence(SPATIAL, IDENTIFIER_NAME)))).skip()
            b.rule(TABLESPACE_INMEMORY_TEXT_COLUMN).define(
                IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME)),
                b.optional(USING, CHARACTER_LITERAL)).skip()
            // Follow the Oracle 26 diagrams for TEXT/SPATIAL. Free 23.26.3 rejected SPATIAL with
            // ORA-00922 and disconnected on a TEXT control; neither establishes Sharding execution.
            b.rule(TABLESPACE_INMEMORY_CLAUSE).define(
                b.firstOf(
                    b.sequence(INMEMORY, TABLESPACE_INMEMORY_ATTRIBUTES,
                        b.optional(TEXT, LPARENTHESIS,
                            TABLESPACE_INMEMORY_TEXT_COLUMN, b.zeroOrMore(COMMA, TABLESPACE_INMEMORY_TEXT_COLUMN),
                            RPARENTHESIS)),
                    b.sequence(NO, INMEMORY))).skip()

            b.rule(TABLESPACE_ILM_CONDITION).define(
                b.firstOf(
                    b.sequence(AFTER, INTEGER_LITERAL, b.firstOf(DAY, DAYS, MONTH, MONTHS, YEAR, YEARS),
                        OF, b.firstOf(b.sequence(NO, b.firstOf(ACCESS, MODIFICATION)), CREATION)),
                    b.sequence(ON, UNIT_NAME))).skip()
            b.rule(TABLESPACE_ILM_POLICY).define(
                b.firstOf(
                    b.sequence(TABLE_COMPRESSION, b.firstOf(SEGMENT, GROUP), TABLESPACE_ILM_CONDITION),
                    b.sequence(
                        b.firstOf(
                            b.sequence(ROW, STORE, COMPRESS, ADVANCED),
                            b.sequence(COLUMN, STORE, COMPRESS, FOR, QUERY)),
                        ROW, AFTER, INTEGER_LITERAL, b.firstOf(DAY, DAYS, MONTH, MONTHS, YEAR, YEARS),
                        OF, NO, MODIFICATION),
                    b.sequence(TIER, TO, IDENTIFIER_NAME,
                        b.firstOf(
                            b.sequence(READ, ONLY, b.optional(b.firstOf(SEGMENT, GROUP)), TABLESPACE_ILM_CONDITION),
                            b.sequence(b.optional(b.firstOf(SEGMENT, GROUP)), b.optional(ON, UNIT_NAME)))),
                    b.sequence(
                        b.firstOf(
                            b.sequence(SET, INMEMORY, TABLESPACE_INMEMORY_ATTRIBUTES),
                            b.sequence(MODIFY, INMEMORY, TABLESPACE_MEMCOMPRESS),
                            b.sequence(NO, INMEMORY)),
                        b.optional(SEGMENT), TABLESPACE_ILM_CONDITION))).skip()
            b.rule(TABLESPACE_ILM_CLAUSE).define(
                ILM, b.firstOf(
                    b.sequence(ADD, POLICY, TABLESPACE_ILM_POLICY),
                    b.sequence(b.firstOf(DELETE, ENABLE, DISABLE), POLICY, IDENTIFIER_NAME),
                    DELETE_ALL, ENABLE_ALL, DISABLE_ALL)).skip()

            // Oracle permits these defaults in any order before final STORAGE (ORA-02180 otherwise).
            // DEFAULT still requires at least one parameter; an empty DEFAULT is ORA-00905.
            b.rule(DEFAULT_TABLESPACE_PARAMS).define(
                DEFAULT,
                b.firstOf(
                    b.sequence(
                        b.oneOrMore(b.firstOf(
                            tableCompression,
                            b.sequence(INDEX, b.firstOf(
                                b.sequence(COMPRESS, ADVANCED, b.firstOf(LOW, HIGH)), NOCOMPRESS)),
                            TABLESPACE_INMEMORY_CLAUSE,
                            TABLESPACE_ILM_CLAUSE)),
                        b.optional(TABLESPACE_STORAGE_CLAUSE)),
                    TABLESPACE_STORAGE_CLAUSE))

            val retentionClause = b.sequence(RETENTION, b.firstOf(GUARANTEE, NOGUARANTEE))
            val groupClause = b.sequence(TABLESPACE, GROUP, b.firstOf(CHARACTER_LITERAL, IDENTIFIER_NAME))
            val flashbackClause = b.sequence(FLASHBACK, b.firstOf(ON, OFF))
            val shardspace = b.sequence(IN, SHARDSPACE, IDENTIFIER_NAME)

            // Each kind of tablespace has its own option set; Oracle 26 rejects the others at parse time
            // (ORA-30044, ORA-30024, ORA-25139). Repeated options are rejected too (ORA-02197/ORA-02198), but are
            // not tracked here.
            b.rule(TABLESPACE_PERMANENT_ATTRS_COMMON).define(
                b.firstOf(
                    b.sequence(BLOCKSIZE, INTEGER_LITERAL, b.optional("K")),
                    LOGGING_CLAUSE,
                    b.sequence(FORCE, LOGGING),
                    TABLESPACE_ENCRYPTION_CLAUSE,
                    DEFAULT_TABLESPACE_PARAMS,
                    ONLINE,
                    OFFLINE,
                    EXTENT_MANAGEMENT_CLAUSE,
                    flashbackClause)).skip()
            val permanentOption = b.firstOf(
                b.sequence(DATAFILE, fileSpecifications),
                b.sequence(MINIMUM, EXTENT, INDEX_SIZE_CLAUSE),
                TABLESPACE_PERMANENT_ATTRS_COMMON,
                b.sequence(SEGMENT, SPACE, MANAGEMENT, b.firstOf(AUTO, MANUAL)),
                shardspace)
            val undoOption = b.firstOf(
                b.sequence(DATAFILE, fileSpecifications),
                EXTENT_MANAGEMENT_CLAUSE,
                retentionClause,
                TABLESPACE_ENCRYPTION_CLAUSE)
            val temporaryOption = b.firstOf(
                b.sequence(TEMPFILE, fileSpecifications),
                groupClause,
                EXTENT_MANAGEMENT_CLAUSE,
                TABLESPACE_ENCRYPTION_CLAUSE)
            val nameClause = b.sequence(b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME)

            b.rule(CREATE_TABLESPACE).define(
                CREATE,
                b.optional(b.firstOf(BIGFILE, SMALLFILE)),
                b.firstOf(
                    b.sequence(UNDO, TABLESPACE, nameClause, b.zeroOrMore(undoOption)),
                    b.sequence(
                        b.firstOf(
                            b.sequence(TEMPORARY, TABLESPACE),
                            b.sequence(LOCAL, TEMPORARY, TABLESPACE, FOR, b.firstOf(ALL, LEAF))),
                        nameClause,
                        b.zeroOrMore(temporaryOption)),
                    // Only a bare LOST WRITE PROTECTION is accepted, after every other option except
                    // IN SHARDSPACE (ORA-65480 for an option after it, ORA-02180 for ENABLE).
                    b.sequence(
                        TABLESPACE, b.nextNot(SET), nameClause,
                        b.zeroOrMore(permanentOption),
                        b.optional(LOST, WRITE, PROTECTION),
                        b.optional(shardspace))),
                b.optional(SEMICOLON))

            // SET creates bigfile permanent tablespaces automatically: no filenames, REUSE, MINIMUM
            // EXTENT, or MANUAL segment management. DATAFILE precedes the repeatable attribute subset.
            // Keep comma-separated specs non-nullable so a trailing comma cannot stand for a file.
            b.rule(TABLESPACE_SET_FILE_SPEC).define(
                b.firstOf(
                    b.sequence(SIZE, INDEX_SIZE_CLAUSE, b.optional(AUTOEXTEND_CLAUSE)),
                    AUTOEXTEND_CLAUSE)).skip()
            b.rule(TABLESPACE_SET_TEMPLATE).define(
                LPARENTHESIS,
                b.nextNot(RPARENTHESIS),
                b.optional(DATAFILE,
                    b.optional(TABLESPACE_SET_FILE_SPEC, b.zeroOrMore(COMMA, TABLESPACE_SET_FILE_SPEC))),
                b.zeroOrMore(b.firstOf(
                    TABLESPACE_PERMANENT_ATTRS_COMMON,
                    b.sequence(SEGMENT, SPACE, MANAGEMENT, AUTO))),
                b.optional(LOST, WRITE, PROTECTION),
                RPARENTHESIS).skip()
            b.rule(CREATE_TABLESPACE_SET).define(
                CREATE, TABLESPACE, SET, IDENTIFIER_NAME,
                b.optional(shardspace),
                b.optional(USING, TEMPLATE, TABLESPACE_SET_TEMPLATE),
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))

            val fileNameConvert = b.sequence(
                FILE_NAME_CONVERT, EQUALS,
                LPARENTHESIS,
                CHARACTER_LITERAL, COMMA, CHARACTER_LITERAL,
                b.zeroOrMore(COMMA, CHARACTER_LITERAL, COMMA, CHARACTER_LITERAL),
                RPARENTHESIS,
                b.optional(KEEP))
            // ONLINE is the default. Oracle 26 rejects an encryption spec before DECRYPT (ORA-02142).
            b.rule(ALTER_TABLESPACE_ENCRYPTION).define(
                ENCRYPTION,
                b.firstOf(
                    b.sequence(
                        OFFLINE,
                        b.firstOf(b.sequence(b.optional(encryptionSpec), ENCRYPT), DECRYPT)),
                    b.sequence(FINISH, b.firstOf(ENCRYPT, REKEY, DECRYPT), b.optional(fileNameConvert)),
                    b.sequence(
                        b.optional(ONLINE),
                        b.firstOf(b.sequence(b.optional(encryptionSpec), b.firstOf(ENCRYPT, REKEY)), DECRYPT),
                        b.optional(fileNameConvert)))).skip()

            // The SET production has one attribute, with the ordinary-only file and state branches excluded.
            // Skipping the shared helpers keeps ordinary ALTER TABLESPACE's existing AST boundaries.
            b.rule(ALTER_TABLESPACE_ATTRS_COMMON).define(
                b.firstOf(
                    DEFAULT_TABLESPACE_PARAMS,
                    b.sequence(RESIZE, INDEX_SIZE_CLAUSE),
                    COALESCE,
                    b.sequence(RENAME, TO, IDENTIFIER_NAME),
                    b.sequence(
                        RENAME, DATAFILE,
                        CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL),
                        TO, CHARACTER_LITERAL, b.zeroOrMore(COMMA, CHARACTER_LITERAL)),
                    b.sequence(b.firstOf(BEGIN, END), BACKUP),
                    b.sequence(DATAFILE, b.firstOf(ONLINE, OFFLINE)),
                    LOGGING_CLAUSE,
                    b.sequence(b.optional(NO), FORCE, LOGGING),
                    ONLINE,
                    // FOR RECOVER remains documented for backward compatibility in Oracle 26.
                    b.sequence(OFFLINE, b.optional(b.firstOf(NORMAL, TEMPORARY, IMMEDIATE, b.sequence(FOR, RECOVER)))),
                    b.sequence(READ, b.firstOf(ONLY, WRITE)),
                    AUTOEXTEND_CLAUSE,
                    ALTER_TABLESPACE_ENCRYPTION,
                    lostWriteAction(b))).skip()

            // Oracle 26 accepts a single attribute per statement (ORA-03049 at a second one), and requires
            // ENABLE, REMOVE or SUSPEND before LOST WRITE PROTECTION here (ORA-02142).
            val alterAttribute = b.firstOf(
                ALTER_TABLESPACE_ATTRS_COMMON,
                b.sequence(MINIMUM, EXTENT, INDEX_SIZE_CLAUSE),
                b.sequence(SHRINK, SPACE, keepSize),
                b.sequence(SHRINK, TEMPFILE, fileNameOrNumber, keepSize),
                b.sequence(ADD, b.firstOf(DATAFILE, TEMPFILE), fileSpecifications),
                b.sequence(DROP, b.firstOf(DATAFILE, TEMPFILE), fileNameOrNumber),
                b.sequence(TEMPFILE, b.firstOf(ONLINE, OFFLINE)),
                groupClause,
                PERMANENT,
                TEMPORARY,
                flashbackClause,
                retentionClause)

            b.rule(ALTER_TABLESPACE).define(
                ALTER, TABLESPACE, b.nextNot(SET), b.optional(IF, EXISTS), IDENTIFIER_NAME, alterAttribute,
                b.optional(SEMICOLON))

            b.rule(ALTER_TABLESPACE_SET).define(
                ALTER, TABLESPACE, SET, IDENTIFIER_NAME, ALTER_TABLESPACE_ATTRS_COMMON,
                b.next(b.firstOf(SEMICOLON, DIVISION, EOF)),
                b.optional(SEMICOLON))
        }

        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-USER.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ALTER-USER.html
        private fun createUser(b: PlSqlGrammarBuilder) {
            // Oracle 26 rejects a literal password (ORA-00988) and a quoted-identifier external name
            // (ORA-28025); only the documented forms are modeled.
            val externallyOrGlobally = b.firstOf(
                b.sequence(
                    EXTERNALLY,
                    b.optional(AS, CHARACTER_LITERAL, b.optional(WITH, THUMBPRINT, CHARACTER_LITERAL))),
                b.sequence(GLOBALLY, b.optional(AS, CHARACTER_LITERAL)))
            val digest = b.sequence(b.optional(HTTP), DIGEST, b.firstOf(ENABLE, DISABLE))

            b.rule(USER_AUTHENTICATION_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        IDENTIFIED,
                        b.firstOf(
                            b.sequence(
                                BY, IDENTIFIER_NAME,
                                b.optional(digest),
                                b.optional(AND, FACTOR, CHARACTER_LITERAL, AS, CHARACTER_LITERAL)),
                            externallyOrGlobally)),
                    b.sequence(NO, AUTHENTICATION)))

            val nameOrKeyword = DclGrammar.IDENTIFIER_OR_KEYWORD
            fun roleList() = b.sequence(nameOrKeyword, b.zeroOrMore(COMMA, nameOrKeyword))

            val sharedOptions = arrayOf<Any>(
                b.sequence(DEFAULT, COLLATION, IDENTIFIER_NAME),
                b.sequence(DEFAULT, TABLESPACE, IDENTIFIER_NAME),
                b.sequence(b.optional(LOCAL), TEMPORARY, TABLESPACE, IDENTIFIER_NAME),
                b.sequence(QUOTA, b.firstOf(UNLIMITED, INDEX_SIZE_CLAUSE), ON, IDENTIFIER_NAME),
                b.sequence(PROFILE, b.firstOf(DEFAULT, IDENTIFIER_NAME)),
                b.sequence(PASSWORD, EXPIRE),
                b.sequence(ACCOUNT, b.firstOf(LOCK, UNLOCK)),
                b.sequence(CONTAINER, EQUALS, b.firstOf(CURRENT, ALL)),
                b.sequence(READ, b.firstOf(ONLY, WRITE)))

            // Oracle 26 accepts the options in any order. Repeats of most of them are rejected, but tracking
            // that per option makes the compiled grammar grow factorially, so the parser accepts them.
            b.rule(CREATE_USER).define(
                CREATE, USER, b.optional(IF, NOT, EXISTS), IDENTIFIER_NAME,
                b.zeroOrMore(b.firstOf(USER_AUTHENTICATION_CLAUSE, b.sequence(ENABLE, EDITIONS), *sharedOptions)),
                b.optional(SEMICOLON))

            val nameList = b.sequence(LPARENTHESIS, IDENTIFIER_NAME, b.zeroOrMore(COMMA, IDENTIFIER_NAME), RPARENTHESIS)
            val alterUserOption = b.firstOf(
                // ALTER has REPLACE instead of CREATE's DIGEST/AND FACTOR suffixes (ORA-00922 at AND).
                b.sequence(
                    IDENTIFIED,
                    b.firstOf(b.sequence(BY, IDENTIFIER_NAME, b.optional(REPLACE, IDENTIFIER_NAME)), externallyOrGlobally)),
                b.sequence(NO, AUTHENTICATION),
                b.sequence(b.firstOf(ADD, UPDATE), FACTOR, CHARACTER_LITERAL, AS, CHARACTER_LITERAL),
                b.sequence(DROP, FACTOR, CHARACTER_LITERAL),
                b.sequence(DEFAULT, ROLE, b.firstOf(b.sequence(ALL, b.optional(EXCEPT, roleList())), NONE, roleList())),
                b.sequence(EXPIRE, PASSWORD, ROLLOVER, PERIOD),
                b.sequence(
                    ENABLE, EDITIONS,
                    b.optional(FOR, nameOrKeyword, b.zeroOrMore(COMMA, nameOrKeyword)),
                    b.optional(FORCE)),
                digest,
                b.sequence(b.firstOf(ENABLE, DISABLE), DICTIONARY, PROTECTION),
                b.sequence(
                    b.firstOf(
                        b.sequence(SET, CONTAINER_DATA, EQUALS, b.firstOf(ALL, DEFAULT, nameList)),
                        b.sequence(b.firstOf(ADD, REMOVE), CONTAINER_DATA, EQUALS, nameList)),
                    b.optional(FOR, IDENTIFIER_NAME, b.optional(DOT, IDENTIFIER_NAME))),
                *sharedOptions)

            // Oracle 26 also accepts the older AUTHENTICATED USING PASSWORD after a proxy user.
            b.rule(USER_PROXY_CLAUSE).define(
                b.firstOf(
                    b.sequence(
                        GRANT, CONNECT, THROUGH,
                        b.firstOf(
                            b.sequence(ENTERPRISE, USERS),
                            b.sequence(
                                IDENTIFIER_NAME,
                                b.optional(
                                    WITH,
                                    b.firstOf(
                                        b.sequence(ROLE, b.firstOf(b.sequence(ALL, EXCEPT, roleList()), roleList())),
                                        b.sequence(NO, ROLES))),
                                b.optional(b.firstOf(
                                    b.sequence(AUTHENTICATION, REQUIRED),
                                    b.sequence(AUTHENTICATED, USING, PASSWORD)))))),
                    b.sequence(REVOKE, CONNECT, THROUGH, b.firstOf(b.sequence(ENTERPRISE, USERS), IDENTIFIER_NAME))))

            // A user list is only valid with a proxy clause (ORA-28151); options may precede the proxy clause
            // but not follow it.
            b.rule(ALTER_USER).define(
                ALTER, USER, b.optional(IF, EXISTS),
                b.firstOf(
                    b.sequence(IDENTIFIER_NAME, b.oneOrMore(COMMA, IDENTIFIER_NAME), USER_PROXY_CLAUSE),
                    b.sequence(IDENTIFIER_NAME, b.zeroOrMore(alterUserOption), b.optional(USER_PROXY_CLAUSE))),
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

            b.rule(CREATE_LOCKDOWN_PROFILE).define(
                CREATE, LOCKDOWN, PROFILE, IDENTIFIER_NAME,
                b.optional(b.firstOf(FROM, INCLUDING), IDENTIFIER_NAME),
                b.optional(SEMICOLON))

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
