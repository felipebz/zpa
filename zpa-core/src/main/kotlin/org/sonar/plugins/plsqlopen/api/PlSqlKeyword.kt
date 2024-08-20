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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.TokenType

enum class PlSqlKeyword(override val value: String, val isReserved: Boolean = false) : TokenType {
    ALL("all", true),
    ALTER("alter", true),
    AND("and", true),
    ANY("any", true),
    AS("as", true),
    ASC("asc", true),
    BEGIN("begin", true),
    BETWEEN("between", true),
    BY("by", true),
    CHAR("char"),
    CHECK("check", true),
    CLUSTER("cluster", true),
    COMPRESS("compress", true),
    CONNECT("connect", true),
    CREATE("create", true),
    DECLARE("declare", true),
    DEFAULT("default", true),
    DESC("desc", true),
    DISTINCT("distinct", true),
    DROP("drop", true),
    ELSE("else", true),
    END("end", true),
    EXCEPT("except"),
    EXCLUSIVE("exclusive", true),
    FETCH("fetch", true),
    FLOAT("float"),
    FOR("for", true),
    FROM("from", true),
    GRANT("grant", true),
    GROUP("group", true),
    HAVING("having", true),
    IDENTIFIED("identified", true),
    IN("in", true),
    INDEX("index", true),
    INSERT("insert", true),
    INTEGER("integer"),
    INTERSECT("intersect", true),
    INTO("into", true),
    IS("is", true),
    LIKE("like", true),
    LOCK("lock", true),
    LONG("long"),
    MINUS_KEYWORD("minus", true),
    NOCOMPRESS("nocompress", true),
    NOT("not", true),
    NOWAIT("nowait", true),
    NULL("null", true),
    OF("of", true),
    ON("on", true),
    OPTION("option", true),
    OR("or", true),
    ORDER("order", true),
    OUT("out", true),
    PCTFREE("pctfree"),
    PRIOR("prior"),
    PROCEDURE("procedure", true),
    PUBLIC("public", true),
    RAW("raw"),
    RESOURCE("resource", true),
    REVOKE("revoke", true),
    SELECT("select", true),
    SHARE("share", true),
    SIZE("size", true),
    SMALLINT("smallint"),
    START("start", true),
    SYNONYM("synonym"),
    TABLE("table", true),
    THEN("then", true),
    TO("to", true),
    TRIGGER("trigger", true),
    UNION("union", true),
    UNIQUE("unique", true),
    UPDATE("update", true),
    VALUES("values", true),
    VIEW("view", true),
    WHEN("when", true),
    WHERE("where", true),
    WITH("with", true),
    A("a"),
    ABSENT("absent"),
    ACCESSIBLE("accessible"),
    ADD("add"),
    ADMIN("admin"),
    AFTER("after"),
    AGENT("agent"),
    AGGREGATE("aggregate"),
    ALLOW("allow"),
    ANALYZE("analyze"),
    ANY_CS("any_cs"),
    APPEND("append"),
    ARRAY("array"),
    ARROW("arrow"),
    ASCII("ascii"),
    ASSOCIATE("associate"),
    AT("at"),
    AUDIT("audit"),
    AUTHID("authid"),
    AUTONOMOUS_TRANSACTION("autonomous_transaction"),
    BATCH("batch"),
    BEFORE("before"),
    BFILE("bfile"),
    BINARY("binary"),
    BINARY_DOUBLE("binary_double"),
    BINARY_DOUBLE_INFINITY("binary_double_infinity"),
    BINARY_DOUBLE_NAN("binary_double_nan"),
    BINARY_FLOAT("binary_float"),
    BINARY_FLOAT_INFINITY("binary_float_infinity"),
    BINARY_FLOAT_NAN("binary_float_nan"),
    BINARY_INTEGER("binary_integer"),
    BLOB("blob"),
    BODY("body"),
    BOOLEAN("boolean"),
    BOTH("both"),
    BREADTH("breadth"),
    BUFFER_POOL("buffer_pool"),
    BULK("bulk"),
    BULK_ROWCOUNT("bulk_rowcount"),
    BYTE("byte"),
    CACHE("cache"),
    CASCADE("cascade"),
    CASE("case"),
    CASE_SENSITIVE("case_sensitive"),
    CAST("cast"),
    CHARACTER("character"),
    CHARSET("charset"),
    CHARSETFORM("charsetform"),
    CHARSETID("charsetid"),
    CHUNK("chunk"),
    CLOB("clob"),
    CLONE("clone"),
    CLOSE("close"),
    CLUSTERS("clusters"),
    COLAUTH("colauth"),
    COLLATION("collation"),
    COLLECT("collect"),
    COLUMN("column"),
    COLUMNS("columns"),
    COMMENT("comment"),
    COMMIT("commit"),
    COMMITTED("committed"),
    COMPILE("compile"),
    COMPLETE("complete"),
    COMPOUND("compound"),
    CONDITIONAL("conditional"),
    CONNECT_BY_ROOT("connect_by_root"),
    CONSTANT("constant"),
    CONSTRAINT("constraint"),
    CONSTRUCTOR("constructor"),
    CONTAINER("container"),
    CONTENT("content"),
    CONTEXT("context"),
    CONTINUE("continue"),
    CONVERSION("conversion"),
    COPY("copy"),
    COUNT("count"),
    CRASH("crash"),
    CROSS("cross"),
    CROSSEDITION("crossedition"),
    CUBE("cube"),
    CURRENT("current"),
    CURRENT_USER("current_user"),
    CURRVAL("currval"),
    CURSOR("cursor"),
    CYCLE("cycle"),
    DATA("data"),
    DATABASE("database"),
    DATE("date"),
    DAY("day"),
    DB_ROLE_CHANGE("db_role_change"),
    DDL("ddl"),
    DEBUG("debug"),
    DEC("dec"),
    DECIMAL("decimal"),
    DEFAULTS("defaults"),
    DEFINER("definer"),
    DELEGATE("delegate"),
    DELETE("delete"),
    DENSE_RANK("dense_rank"),
    DEPRECATE("deprecate"),
    DEPTH("depth"),
    DETERMINISTIC("deterministic"),
    DIRECTORY("directory"),
    DISABLE("disable"),
    DISALLOW("disallow"),
    DISASSOCIATE("disassociate"),
    DOCUMENT("document"),
    DOUBLE("double"),
    DURATION("duration"),
    EACH("each"),
    EDITIONABLE("editionable"),
    ELEMENT("element"),
    ELSIF("elsif"),
    EMPTY("empty"),
    ENABLE("enable"),
    ENCODING("encoding"),
    ENCRYPT("encrypt"),
    ENTITYESCAPING("entityescaping"),
    ERROR("error"),
    ERRORS("errors"),
    ESCAPE("escape"),
    EVALNAME("evalname"),
    EXCEPTION("exception"),
    EXCEPTIONS("exceptions"),
    EXCEPTION_INIT("exception_init"),
    EXECUTE("execute"),
    EXISTING("existing"),
    EXISTS("exists"),
    EXIT("exit"),
    EXTEND("extend"),
    EXTERNAL("external"),
    EXTRA("extra"),
    EXTRACT("extract"),
    FALSE("false"),
    FINAL("final"),
    FIRST("first"),
    FOLLOWING("following"),
    FOLLOWS("follows"),
    FORALL("forall"),
    FORCE("force"),
    FOREIGN("foreign"),
    FORM("form"),
    FORMAT("format"),
    FORWARD("forward"),
    FOUND("found"),
    FREELIST("freelist"),
    FREELISTS("freelists"),
    FREEPOOLS("freepools"),
    FULL("full"),
    FUNCTION("function"),
    GLOBAL("global"),
    GOTO("goto"),
    GROUPING("grouping"),
    GROUPS("groups"),
    HASH("hash"),
    HIDE("hide"),
    HIERARCHY("hierarchy"),
    HOUR("hour"),
    IF("if"),
    IGNORE("ignore"),
    IMMEDIATE("immediate"),
    IMMUTABLE("immutable"),
    INCREMENT("increment"),
    INDENT("indent"),
    INDEXES("indexes"),
    INDEXTYPE("indextype"),
    INDICATOR("indicator"),
    INDICES("indices"),
    INITIAL("initial"),
    INITRANS("initrans"),
    INLINE("inline"),
    INNER("inner"),
    INSTANTIABLE("instantiable"),
    INSTEAD("instead"),
    INT("int"),
    INTERFACE("interface"),
    INTERVAL("interval"),
    ISOLATION("isolation"),
    ISOPEN("isopen"),
    JAVA("java"),
    JOIN("join"),
    JSON("json"),
    JSON_ARRAY("json_array"),
    JSON_ARRAYAGG("json_arrayagg"),
    JSON_DATAGUIDE("json_dataguide"),
    JSON_EQUAL("json_equal"),
    JSON_EXISTS("json_exists"),
    JSON_MERGEPATCH("json_mergepatch"),
    JSON_OBJECT("json_object"),
    JSON_OBJECTAGG("json_objectagg"),
    JSON_QUERY("json_query"),
    JSON_SCALAR("json_scalar"),
    JSON_SERIALIZE("json_serialize"),
    JSON_TABLE("json_table"),
    JSON_TEXTCONTAINS("json_textcontains"),
    JSON_TRANSFORM("json_transform"),
    JSON_VALUE("json_value"),
    KEEP("keep"),
    KEY("key"),
    KEYS("keys"),
    LANGUAGE("language"),
    LAST("last"),
    LAX("lax"),
    LEADING("leading"),
    LEFT("left"),
    LENGTH("length"),
    LESS("less"),
    LEVEL("level"),
    LEVELS("levels"),
    LIBRARY("library"),
    LIMIT("limit"),
    LIST("list"),
    LISTAGG("listagg"),
    LOB("lob"),
    LOCAL("local"),
    LOCKED("locked"),
    LOG("log"),
    LOGGING("logging"),
    LOGOFF("logoff"),
    LOGON("logon"),
    LOOP("loop"),
    MAP("map"),
    MAPPING("mapping"),
    MATCHED("matched"),
    MATERIALIZED("materialized"),
    MAXEXTENTS("maxextents"),
    MAXLEN("maxlen"),
    MAXVALUE("maxvalue"),
    MEMBER("member"),
    MERGE("merge"),
    METADATA("metadata"),
    MINEXTENTS("minextents"),
    MINING("mining"),
    MINUTE("minute"),
    MINVALUE("minvalue"),
    MISMATCH("mismatch"),
    MISSING("missing"),
    MODE("mode"),
    MODEL("model"),
    MOD_KEYWORD("mod"),
    MONTH("month"),
    MORE("more"),
    MULTISET("multiset"),
    MUTABLE("mutable"),
    NAME("name"),
    NATURAL("natural"),
    NATURALN("naturaln"),
    NCHAR("nchar"),
    NCLOB("nclob"),
    NESTED("nested"),
    NEW("new"),
    NEXT("next"),
    NEXTVAL("nextval"),
    NO("no"),
    NOAUDIT("noaudit"),
    NOCACHE("nocache"),
    NOCOPY("nocopy"),
    NOCYCLE("nocycle"),
    NOENTITYESCAPING("noentityescaping"),
    NOLOGGING("nologging"),
    NOMAPPING("nomapping"),
    NONE("none"),
    NONEDITIONABLE("noneditionable"),
    NOORDER("noorder"),
    NOSCHEMACHECK("noschemacheck"),
    NOTFOUND("notfound"),
    NOW("now"),
    NULLS("nulls"),
    NUMBER("number"),
    NUMERIC("numeric"),
    NVARCHAR2("nvarchar2"),
    OBJECT("object"),
    OFFSET("offset"),
    OLD("old"),
    OMIT("omit"),
    ONLY("only"),
    OPEN("open"),
    OPERATOR("operator"),
    OPTIMAL("optimal"),
    ORDERED("ordered"),
    ORDINALITY("ordinality"),
    OTHERS("others"),
    OUTER("outer"),
    OVER("over"),
    OVERFLOW("overflow"),
    OVERLAPS("overlaps"),
    OVERRIDING("overriding"),
    PACKAGE("package"),
    PAIRS("pairs"),
    PARALLEL_ENABLE("parallel_enable"),
    PARAMETERS("parameters"),
    PARENT("parent"),
    PARTITION("partition"),
    PARTITIONS("partitions"),
    PASSING("passing"),
    PATH("path"),
    PCTINCREASE("pctincrease"),
    PCTUSED("pctused"),
    PCTVERSION("pctversion"),
    PERCENT("percent"),
    PIPE("pipe"),
    PIPELINED("pipelined"),
    PLS_INTEGER("pls_integer"),
    PLUGGABLE("pluggable"),
    POSITIVE("positive"),
    POSITIVEN("positiven"),
    PRAGMA("pragma"),
    PRECEDES("precedes"),
    PRECEDING("preceding"),
    PRECISION("precision"),
    PREPEND("prepend"),
    PRESERVE("preserve"),
    PRETTY("pretty"),
    PRIMARY("primary"),
    PURGE("purge"),
    QUOTES("quotes"),
    RAISE("raise"),
    RANGE_KEYWORD("range"),
    READ("read"),
    READS("reads"),
    REAL("real"),
    RECORD("record"),
    RECYCLE("recycle"),
    REF("ref"),
    REFERENCE("reference"),
    REFERENCES("references"),
    REFERENCING("referencing"),
    REFRESH("refresh"),
    REJECT("reject"),
    RELIES_ON("relies_on"),
    REMOVE("remove"),
    RENAME("rename"),
    REPEAT("repeat"),
    REPLACE("replace"),
    RESTRICT_REFERENCES("restrict_references"),
    RESULT("result"),
    RESULT_CACHE("result_cache"),
    RETENTION("retention"),
    RETURN("return"),
    RETURNING("returning"),
    REUSE("reuse"),
    REVERSE("reverse"),
    RIGHT("right"),
    ROLE("role"),
    ROLLBACK("rollback"),
    ROLLUP("rollup"),
    ROW("row"),
    ROWCOUNT("rowcount"),
    ROWID("rowid"),
    ROWNUM("rownum"),
    ROWS("rows"),
    ROWTYPE("rowtype"),
    SAVE("save"),
    SAVEPOINT("savepoint"),
    SCALAR("scalar"),
    SCALARS("scalars"),
    SCHEMA("schema"),
    SCHEMACHECK("schemacheck"),
    SDO_GEOMETRY("sdo_geometry"),
    SEARCH("search"),
    SECOND("second"),
    SEGMENT("segment"),
    SELF("self"),
    SET("set"),
    SEQUENCE("sequence"),
    SERIALIZABLE("serializable"),
    SERIALLY_REUSABLE("serially_reusable"),
    SERVERERROR("servererror"),
    SESSION("session"),
    SETS("sets"),
    SETTINGS("settings"),
    SHARING("sharing"),
    SHOW("show"),
    SHUTDOWN("shutdown"),
    SIBLINGS("siblings"),
    SIGNTYPE("signtype"),
    SKIP("skip"),
    SOME("some"),
    SORT("sort"),
    SPECIFICATION("specification"),
    SQL("sql"),
    SQLERRM("sqlerrm"),
    STANDALONE("standalone"),
    STARTUP("startup"),
    STATEMENT_KEYWORD("statement"),
    STATIC("static"),
    STATISTICS("statistics"),
    STORAGE("storage"),
    STORE("store"),
    STRICT("strict"),
    STRING("string"),
    STRUCT("struct"),
    SUBMULTISET("submultiset"),
    SUBPARTITION("subpartition"),
    SUBPARTITIONS("subpartitions"),
    SUBSTITUTABLE("substitutable"),
    SUBTYPE("subtype"),
    SUSPEND("suspend"),
    SYSDATE("sysdate"),
    TABAUTH("tabauth"),
    TABLESPACE("tablespace"),
    TDO("tdo"),
    TEMPLATE("template"),
    TEMPORARY("temporary"),
    THAN("than"),
    THE("the"),
    TIES("ties"),
    TIME("time"),
    TIMESTAMP("timestamp"),
    TRAILING("trailing"),
    TRANSACTION("transaction"),
    TREAT("treat"),
    TRIM("trim"),
    TRUE("true"),
    TRUNCATE("truncate"),
    TYPE("type"),
    TYPENAME("typename"),
    UDF("udf"),
    UNBOUNDED("unbounded"),
    UNCONDITIONAL("unconditional"),
    UNDER("under"),
    UNLIMITED("unlimited"),
    UNPLUG("unplug"),
    UROWID("urowid"),
    USE("use"),
    USING("using"),
    USING_NLS_COMP("using_nls_comp"),
    VALIDATE("validate"),
    VALUE("value"),
    VARCHAR("varchar"),
    VARCHAR2("varchar2"),
    VARRAY("varray"),
    VARYING("varying"),
    VECTOR("vector"),
    VERSION("version"),
    VIEWS("views"),
    WAIT("wait"),
    WELLFORMED("wellformed"),
    WHILE("while"),
    WITHIN("within"),
    WITHOUT("without"),
    WORK("work"),
    WRAPPER("wrapper"),
    WRITE("write"),
    XMLAGG("xmlagg"),
    XMLATTRIBUTES("xmlattributes"),
    XMLCAST("xmlcast"),
    XMLCDATA("xmlcdata"),
    XMLCOLATTVAL("xmlcolattval"),
    XMLCOMMENT("xmlcomment"),
    XMLCONCAT("xmlconcat"),
    XMLDIFF("xmldiff"),
    XMLELEMENT("xmlelement"),
    XMLEXISTS("xmlexists"),
    XMLFOREST("xmlforest"),
    XMLISVALID("xmlisvalid"),
    XMLNAMESPACES("xmlnamespaces"),
    XMLPARSE("xmlparse"),
    XMLPATCH("xmlpatch"),
    XMLPI("xmlpi"),
    XMLQUERY("xmlquery"),
    XMLROOT("xmlroot"),
    XMLSEQUENCE("xmlsequence"),
    XMLSERIALIZE("xmlserialize"),
    XMLTABLE("xmltable"),
    XMLTRANSFORM("xmltransform"),
    XMLTYPE("xmltype"),
    YEAR("year"),
    YES("yes"),
    ZONE("zone");

    override fun hasToBeSkippedFromAst(node: AstNode?) = false

    companion object {
        val keywordValues: List<String> =
            entries.map { it.value }

        val nonReservedKeywords: List<PlSqlKeyword> =
            entries.filter { !it.isReserved }
    }
}
