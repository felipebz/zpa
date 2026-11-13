//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** Which column a name of static SQL reads, with the relations' columns from metadata built here. */
class ColumnBindingTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun columns(vararg names: String) = names.mapIndexed { index, it -> ProjectExternalColumn.of(it, index + 1, null) }

    private val metadata = ProjectExternalMetadata.builder("test").completeSchema("SHOP").completePublicSynonyms()
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_LINE", true, columns("ORDER_ID", "QTY", "AMOUNT")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_HEAD", true, columns("ID", "STATUS")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_MARK", true, columns("ID", "L")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_TAIL", true, columns("ORDER_ID", "NOTE")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_NOTE", false, columns("ID")))
        .relation(ProjectExternalRelation.view("SHOP", "ORDER_V", true, columns("ID", "STATUS")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_OBJ", true, columns("ID", "OBJECT_ID", "COLUMN_VALUE", "OBJECT_VALUE", "CURRENT_DATE", "NEXTVAL")))
        .relation(ProjectExternalRelation.of("SHOP", "ORDER_SCN", true, columns("ID", "ORA_ROWSCN", "CONNECT_BY_ISLEAF")))
        .schemaObject(ProjectExternalObject.of("SHOP", "PK_EXT", ProjectExternalObject.Kind.PACKAGE))
        .synonym(ProjectExternalSynonym.of("SHOP", "LINES", "SHOP", "ORDER_LINE", null))
        .build()

    private fun answers(text: String, withMetadata: Boolean = true, answer: (PlSqlVisitorContext, AstNode) -> String = ::render): List<String> {
        val external = ProjectExternalContext.of(if (withMetadata) metadata else ProjectExternalMetadata.none(), ProjectIdentifier.fromSource("SHOP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external).prepare(listOf(ProjectSource(FileId("u.sql")) { text }), concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
                    .filter { !(it.type === PlSqlGrammar.VARIABLE_NAME && it.parent.type === PlSqlGrammar.MEMBER_EXPRESSION) }
                    .forEach { name -> rows += "${name.token.line}:${name.tokens.joinToString("") { it.originalValue }}=${answer(this.context, name)}" }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", text), listOf(check), FileId("u.sql"))
        return rows
    }

    private fun render(context: PlSqlVisitorContext, name: AstNode): String = render(context.columnBinding(name))

    private fun render(binding: ColumnBinding): String = when (binding) {
        is ColumnBinding.Column -> {
            val relation = binding.source.relation?.lookup?.orElse(null)?.relation?.orElse(null)
                ?.let { "${it.schema.originalSpelling.trim('"')}.${it.name.originalSpelling.trim('"')}" } ?: binding.source.label
            val exists = when (binding.exists) {
                true -> ""
                false -> " missing"
                null -> " unverified"
            }
            "column $relation.${binding.name}$exists"
        }
        is ColumnBinding.QueryColumn -> "query ${binding.source.label}.${binding.name}" + (binding.item?.let { " from ${it.token.line}:${it.token.column}" } ?: "")
        is ColumnBinding.SelectItem -> "item ${binding.item.token.line}:${binding.item.token.column}"
        is ColumnBinding.Source -> "source ${binding.source.label}"
        is ColumnBinding.Joined -> "joined " + binding.columns.joinToString { render(it) }
        is ColumnBinding.Ambiguous -> "ambiguous " + binding.candidates.joinToString { render(it) }
        is ColumnBinding.PlSqlName -> "plsql ${binding.reason}"
        is ColumnBinding.Unknown -> "Unknown${binding.reasons}" + binding.candidates.joinToString("") { " maybe " + render(it) }
        else -> binding.toString()
    }

    private fun procedure(vararg statements: String) =
        "create procedure p_cols is\n  qty number;\n  v number;\n  status varchar2(5);\n  zz number;\n  amount number;\nbegin\n" +
            statements.joinToString("") { "  $it\n" } + "end;\n/\n"

    @Test
    fun aColumnComesBeforeAPlSqlName() {
        assertThat(answers(procedure(
            "select qty into v from order_line;",
            "select count(*) into v from order_line l where l.qty = qty;",
            "select zz into v from order_line;",
            "select zz into v from dual;"
        ))).containsExactly(
            "8:qty=column SHOP.ORDER_LINE.QTY",
            "8:v=plsql INTO_TARGET",
            // A call: callBinding answers it.
            "9:count=Unavailable(NOT_A_COVERED_NAME)",
            "9:v=plsql INTO_TARGET",
            "9:l.qty=column SHOP.ORDER_LINE.QTY",
            "9:qty=column SHOP.ORDER_LINE.QTY",
            // Every source is known to lack the column, DUAL included: the PL/SQL name.
            "10:zz=plsql NO_SOURCE_HAS_IT",
            "10:v=plsql INTO_TARGET",
            "11:zz=plsql NO_SOURCE_HAS_IT",
            "11:v=plsql INTO_TARGET"
        )
    }

    @Test
    fun anInnerScopeComesBeforeAnOuterOne() {
        assertThat(answers(procedure(
            "select max(h.status) into status from order_head h where exists (select 1 from order_line l where status = 'H');",
            "select count(*) into v from order_head i where exists (select 1 from order_tail i where i.status = 'S');",
            "select (select max(zz) from order_line l) into v from order_head h;"
        )).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "8:h.status=column SHOP.ORDER_HEAD.STATUS",
            // ORDER_LINE lacks STATUS: the outer query's column, before the PL/SQL variable.
            "8:status=column SHOP.ORDER_HEAD.STATUS",
            // The inner alias i lacks the column: the outer alias i has it.
            "9:i.status=column SHOP.ORDER_HEAD.STATUS",
            "10:zz=plsql NO_SOURCE_HAS_IT"
        )
    }

    @Test
    fun anInnerAliasLackingTheColumnPassesItToTheOuterOne() {
        assertThat(answers(procedure(
            "select max(l.qty) into v from order_line l where exists (select 1 from order_head l where l.qty = 1);"
        )).filter { it.contains(".") }).containsExactly(
            "8:l.qty=column SHOP.ORDER_LINE.QTY",
            "8:l.qty=column SHOP.ORDER_LINE.QTY"
        )
    }

    @Test
    fun aColumnNamedLikeAPseudocolumn() {
        assertThat(answers(procedure(
            "select object_id, column_value, object_value, current_date into v, v, v, v from order_obj;",
            "select o.object_id, o.nextval, o.current_date into v, v, v from order_obj o;",
            "select ora_rowscn, connect_by_isleaf, s.connect_by_isleaf into v, v, v from order_scn s;",
            "select count(*) into v from order_scn x where exists (select 1 from order_head where ora_rowscn = 42);",
            "select object_id, h.object_value, rowid, h.rowid, h.ora_rowscn into v, v, v, v, v from order_head h;",
            "select order_seq.nextval into v from dual;"
        )).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "8:object_id=column SHOP.ORDER_OBJ.OBJECT_ID",
            "8:column_value=column SHOP.ORDER_OBJ.COLUMN_VALUE",
            "8:object_value=column SHOP.ORDER_OBJ.OBJECT_VALUE",
            // Unqualified, the SQL function comes before a column of that name.
            "8:current_date=plsql BUILT_IN_FUNCTION",
            "9:o.object_id=column SHOP.ORDER_OBJ.OBJECT_ID",
            "9:o.nextval=column SHOP.ORDER_OBJ.NEXTVAL",
            "9:o.current_date=column SHOP.ORDER_OBJ.CURRENT_DATE",
            "10:ora_rowscn=column SHOP.ORDER_SCN.ORA_ROWSCN",
            // Unqualified, the hierarchical pseudocolumn comes before a column of that name; qualified, the column.
            "10:connect_by_isleaf=Pseudocolumn",
            "10:s.connect_by_isleaf=column SHOP.ORDER_SCN.CONNECT_BY_ISLEAF",
            // The inner table's own ORA_ROWSCN, not the outer table's column.
            "11:ora_rowscn=Pseudocolumn",
            // An object table has OBJECT_ID and OBJECT_VALUE without such columns: the metadata does not say which tables are.
            "12:object_id=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column SHOP.ORDER_HEAD.OBJECT_ID missing",
            "12:h.object_value=column SHOP.ORDER_HEAD.OBJECT_VALUE unverified",
            "12:rowid=Pseudocolumn",
            "12:h.rowid=Pseudocolumn",
            "12:h.ora_rowscn=Pseudocolumn",
            "13:order_seq.nextval=Pseudocolumn"
        )
    }

    @Test
    fun anAliasLackingTheColumnAndAPackageOfThatName() {
        val text = "create package pk_q as\n  g number := 1;\n  function col return number;\nend;\n/\n" + procedure(
            "select pk_q.col into v from order_head pk_q;",
            "select pk_q.g into v from order_head pk_q;",
            "select pk_ext.col into v from order_head pk_ext;",
            "select pk_none.col into v from order_head pk_none;",
            "select shop.pk_q.g into v from order_head shop;",
            "select shop.order_head.no_col into v from shop.order_head;"
        )
        assertThat(answers(text).filter { it.contains(".") }).containsExactly(
            // The package's function does not take the name: Oracle rejects the statement.
            "13:pk_q.col=column SHOP.ORDER_HEAD.COL missing",
            // A package variable does.
            "14:pk_q.g=plsql NO_SOURCE_HAS_IT",
            // A package outside the project may have a variable of that name.
            // Else Oracle reads the missing column.
            "15:pk_ext.col=Unknown[PLSQL_NAME_UNDECIDED] maybe column SHOP.ORDER_HEAD.COL missing",
            "16:pk_none.col=column SHOP.ORDER_HEAD.COL missing",
            "17:shop.pk_q.g=plsql NO_SOURCE_HAS_IT",
            "18:shop.order_head.no_col=column SHOP.ORDER_HEAD.NO_COL missing"
        )
    }

    @Test
    fun anAliasNamedLikeTheEnclosingPackage() {
        val text = "create package pk_q as\n  function col return number;\n  g number;\nend;\n/\n" +
            "create package body pk_q as\n  b_var number;\n  function col return number is begin return 1; end;\n" +
            "  procedure p_in is\n    v number;\n  begin\n" +
            "    select pk_q.b_var into v from order_head pk_q;\n" +
            "    select pk_q.g into v from order_head pk_q;\n" +
            "    select pk_q.col into v from order_head pk_q;\n" +
            "  end;\n  function fn_x(p number) return number is\n    v number;\n  begin\n" +
            "    select fn_x.p into v from order_head fn_x;\n    return v;\n  end;\nend;\n/\n"
        assertThat(answers(text).filter { it.contains(".") }).containsExactly(
            // The alias lacks the column: a variable of the package takes the name, its body's own included.
            "12:pk_q.b_var=plsql NO_SOURCE_HAS_IT",
            "13:pk_q.g=plsql NO_SOURCE_HAS_IT",
            // A function of the package does not, as outside it: Oracle rejects the statement.
            "14:pk_q.col=column SHOP.ORDER_HEAD.COL missing",
            // A parameter named through its subprogram does.
            "19:fn_x.p=plsql NO_SOURCE_HAS_IT"
        )
    }

    @Test
    fun aRecordNamedLikeAnAlias() {
        val text = procedure(
            "for l in (select 7 qty from dual) loop select l.qty into v from order_head h; end loop;",
            "for l in (select 7 qty from dual) loop select l.qty into v from order_head l; end loop;",
            "for l in (select 7 qty from dual) loop select l.qty into v from order_line l; end loop;",
            "for l in (select 7 qty from dual) loop select l into v from order_mark h; end loop;",
            "for l in (select 7 qty from dual) loop select l.qty into v from order_mark h; end loop;",
            "select c.no_col into v from order_line c;"
        )
        assertThat(answers(text).filter { it.contains(".") || it.startsWith("11:l=") }).containsExactly(
            // No source is named l: the loop record.
            "8:l.qty=plsql QUALIFIER_NOT_A_SOURCE",
            // The alias l lacks QTY: the loop record.
            "9:l.qty=plsql NO_SOURCE_HAS_IT",
            "10:l.qty=column SHOP.ORDER_LINE.QTY",
            // A column named l comes before the record; l.qty is still the record's field.
            "11:l=column SHOP.ORDER_MARK.L",
            "12:l.qty=plsql QUALIFIER_NOT_A_SOURCE",
            // Nothing else takes c: the missing column, which Oracle rejects.
            "13:c.no_col=column SHOP.ORDER_LINE.NO_COL missing"
        )
        // nameBinding agrees: a relation takes the first name before the record only when it has the column.
        val first = answers(text) { context, name ->
            if (name.type === PlSqlGrammar.MEMBER_EXPRESSION) context.nameBinding(name.firstChild).toString() else "-"
        }.filter { !it.endsWith("=-") }
        assertThat(first).containsExactly(
            "8:l.qty=Local(LOOP_INDEX@8:6)",
            "9:l.qty=ColumnOrLocal(LOOP_INDEX@9:6)",
            "10:l.qty=ColumnOrLocal(LOOP_INDEX@10:6)",
            "12:l.qty=Local(LOOP_INDEX@12:6)",
            "13:c.no_col=NothingLocal(column possible)"
        )
    }

    @Test
    fun selectItemsWithQueriesAndInlineViews() {
        assertThat(answers(procedure(
            "select qty into v from (select qty, -qty as amount from order_line order by amount) where rownum = 1;",
            "with order_line as (select 99 qty from dual) select qty into v from order_line;",
            "select iv.n, n, iv.order_id into v, v, v from (select order_id, qty n from order_line) iv;",
            "select qty into v from (select * from order_line) iv;",
            "select qty into v from (select * from order_note) iv;",
            "with w (a, b) as (select order_id, qty from order_line) select w.b into v from w;",
            "for r in (select qty, amount qty from order_line order by qty) loop null; end loop;"
        )).filter { !it.contains("INTO_TARGET") }).containsExactly(
            "8:qty=query INLINE_VIEW.QTY from 8:33",
            "8:qty=column SHOP.ORDER_LINE.QTY",
            "8:qty=column SHOP.ORDER_LINE.QTY",
            // An alias comes before a column of the same name in ORDER BY.
            "8:amount=item 8:38",
            "8:rownum=Pseudocolumn",
            // A WITH name hides the table of the same name.
            "9:qty=query order_line.QTY from 9:29",
            "10:iv.n=query IV.N from 10:66",
            "10:n=query IV.N from 10:66",
            "10:iv.order_id=query IV.ORDER_ID from 10:56",
            "10:order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "10:qty=column SHOP.ORDER_LINE.QTY",
            "11:qty=query IV.QTY from 11:33",
            // A star over a relation whose columns are not all known.
            "12:qty=Unknown[STAR_NOT_COVERED] maybe query IV.QTY",
            "13:order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "13:qty=column SHOP.ORDER_LINE.QTY",
            "13:w.b=query w.B from 13:37",
            "14:qty=column SHOP.ORDER_LINE.QTY",
            "14:amount=column SHOP.ORDER_LINE.AMOUNT",
            // Two select items named qty (ORA-00960).
            "14:qty=ambiguous item 14:19, item 14:24"
        )
    }

    @Test
    fun joinsSynonymsAndQualifiers() {
        assertThat(answers(procedure(
            "select id into v from order_line l, order_head h where l.order_id = h.id;",
            "select count(order_id) into v from order_line join order_tail using (order_id);",
            "select count(*) into v from order_line a, order_tail b where order_id = 1;",
            "select qty into v from lines;",
            "select shop.order_line.qty into v from shop.order_line;",
            "select order_line.qty into v from order_line ol;",
            "select ol.*, rowid, sysdate into v, v, v from order_line ol;",
            "select status into v from order_v;",
            "select ref(ol), ol.amount.to_text(1) into v, v from order_line ol;"
        )).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "8:id=column SHOP.ORDER_HEAD.ID",
            "8:l.order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "8:h.id=column SHOP.ORDER_HEAD.ID",
            "9:order_id=joined column SHOP.ORDER_LINE.ORDER_ID, column SHOP.ORDER_TAIL.ORDER_ID",
            "10:order_id=ambiguous column SHOP.ORDER_LINE.ORDER_ID, column SHOP.ORDER_TAIL.ORDER_ID",
            // Through the synonym, to its table.
            "11:qty=column SHOP.ORDER_LINE.QTY",
            "12:shop.order_line.qty=column SHOP.ORDER_LINE.QTY",
            // A relation that has an alias is reachable only through it (ORA-00904).
            "13:order_line.qty=plsql QUALIFIER_NOT_A_SOURCE",
            "14:ol=source OL",
            "14:rowid=Pseudocolumn",
            "14:sysdate=plsql BUILT_IN_FUNCTION",
            "15:status=column SHOP.ORDER_V.STATUS",
            // REF takes an alias; a method called on a column reads that column.
            "16:ol=source OL",
            "16:ol.amount.to_text=column SHOP.ORDER_LINE.AMOUNT"
        )
    }

    @Test
    fun dmlStatements() {
        assertThat(answers(procedure(
            "insert into order_line (qty, amount) values (qty, 0) returning qty into v;",
            "update order_line set qty = amount where qty = 5;",
            "merge into order_line d using order_tail s on (d.order_id = s.order_id) when matched then update set qty = 1 " +
                "when not matched then insert (order_id, qty) values (s.order_id, note);"
        ))).containsExactly(
            "8:qty=column SHOP.ORDER_LINE.QTY",
            "8:amount=column SHOP.ORDER_LINE.AMOUNT",
            // The target of INSERT takes a name of RETURNING only.
            "8:qty=plsql NO_SOURCE_HAS_IT",
            "8:qty=column SHOP.ORDER_LINE.QTY",
            "8:v=plsql INTO_TARGET",
            "9:qty=column SHOP.ORDER_LINE.QTY",
            "9:amount=column SHOP.ORDER_LINE.AMOUNT",
            "9:qty=column SHOP.ORDER_LINE.QTY",
            "10:d.order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "10:s.order_id=column SHOP.ORDER_TAIL.ORDER_ID",
            "10:qty=column SHOP.ORDER_LINE.QTY",
            "10:order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "10:qty=column SHOP.ORDER_LINE.QTY",
            "10:s.order_id=column SHOP.ORDER_TAIL.ORDER_ID",
            "10:note=column SHOP.ORDER_TAIL.NOTE"
        )
    }

    @Test
    fun undecidedNames() {
        assertThat(answers(procedure(
            "select x into v from missing_tab;",
            "select m.x into v from missing_tab m;",
            "select qty into v from order_line\n  \$if \$\$extra \$then where amount > 0 \$end\n  ;"
        )).filter { !it.contains("INTO_TARGET") }).containsExactly(
            // The relation is not in the metadata: its columns are not known.
            "8:x=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column missing_tab.X unverified",
            "9:m.x=column M.X unverified",
            // Conditional text inside the statement.
            "10:qty=Unknown[CONDITIONAL_COMPILATION]",
            "11:amount=Unknown[CONDITIONAL_COMPILATION]"
        )
        // Without metadata no source's columns are known.
        assertThat(answers(procedure("select qty into v from order_line;"), withMetadata = false))
            .containsExactly("8:qty=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column order_line.QTY unverified", "8:v=plsql INTO_TARGET")
    }

    @Test
    fun theCandidatesOfAnUndecidedName() {
        assertThat(answers(procedure(
            "select x_col into v from missing_tab;",
            "select qty into v from order_line, missing_tab m;",
            "select qty into v from (select * from missing_tab) iv;",
            "select column_value into v from table(sys.odcinumberlist(1)) t;",
            "select max(n) into v from order_head h, (select id n from order_line) iv, missing_tab m where nvl(id, 0) = 1;"
        )).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            // The column the only source would give, its columns not known.
            "8:x_col=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column missing_tab.X_COL unverified",
            // The known match, and the column of the source that may have one too (ORA-00918).
            "9:qty=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column SHOP.ORDER_LINE.QTY maybe column M.QTY unverified",
            // The inline view's column, its `*` over a relation whose columns are not known.
            "10:qty=Unknown[STAR_NOT_COVERED] maybe query IV.QTY",
            "11:column_value=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column T.COLUMN_VALUE unverified",
            // The inline view's known column beside missing_tab, and none of a source known to lack it (ORDER_HEAD has no N).
            "12:n=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe query IV.N from 12:50 maybe column M.N unverified",
            "12:id=plsql NO_SOURCE_HAS_IT",
            "12:id=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column SHOP.ORDER_HEAD.ID maybe column M.ID unverified"
        )
    }

    @Test
    fun aNameMarkedForAnOuterJoinIsTheNameWithoutTheMarker() {
        val text = procedure(
            "select count(*) into v from order_line l, order_head h where l.order_id = h.id(+) and status(+) = 'X';",
            "select count(*) into v from order_line l, missing_tab m where l.order_id = m.x(+) and y(+) = 1;"
        )
        assertThat(answers(text).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "8:l.order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "8:h.id=column SHOP.ORDER_HEAD.ID",
            "8:status=column SHOP.ORDER_HEAD.STATUS",
            "9:l.order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "9:m.x=column M.X unverified",
            "9:y=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column M.Y unverified"
        )
        // nameBinding agrees: the alias takes the first name, the marker or not.
        assertThat(answers(text) { context, name ->
            if (name.type === PlSqlGrammar.MEMBER_EXPRESSION) context.nameBinding(name.firstChild).toString() else "-"
        }.filter { !it.endsWith("=-") }).containsExactly(
            "8:l.order_id=NothingLocal(column possible)", "8:h.id=NothingLocal(column possible)",
            "9:l.order_id=NothingLocal(column possible)", "9:m.x=NothingLocal(column possible)"
        )
    }

    @Test
    fun bindVariablesAndAStarOfNoSource() {
        assertThat(answers(
            "create trigger order_line_biu before insert on order_line for each row\ndeclare\n  v number;\nbegin\n" +
                "  select count(*) into v from order_head where id = :new.order_id;\n" +
                "  for r in (select z.* from order_line l) loop null; end loop;\nend;\n/\n"
        ).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "5:id=column SHOP.ORDER_HEAD.ID",
            "5::new.order_id=plsql BIND_VARIABLE",
            "6:z=plsql QUALIFIER_NOT_A_SOURCE"
        )
    }

    /**
     * `line:name=column binding / name binding` for each name of each dynamic text of [text]: the text of every
     * `EXECUTE IMMEDIATE`, and the second argument of every `DBMS_SQL.PARSE`.
     */
    private fun dynamicAnswers(text: String): List<String> {
        val external = ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("SHOP"), ProjectSchemaPaths.none())
        val context = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), external).prepare(listOf(ProjectSource(FileId("u.sql")) { text }), concurrent = false)
        )
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                val expressions = node.getDescendants(PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT).map { it.children[2] } +
                    node.getDescendants(PlSqlGrammar.METHOD_CALL).filter { it.firstChild.tokens.joinToString("") { t -> t.value }.equals("DBMS_SQL.PARSE", ignoreCase = true) }
                        .map { it.getFirstChild(PlSqlGrammar.ARGUMENTS).getChildren(PlSqlGrammar.ARGUMENT)[1].firstChild }
                for (expression in expressions) {
                    val dynamic = this.context.dynamicSql(expression) ?: continue
                    dynamic.root.getDescendants(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION)
                        .filter { !(it.type === PlSqlGrammar.VARIABLE_NAME && it.parent.type === PlSqlGrammar.MEMBER_EXPRESSION) }
                        .forEach { name ->
                            val written = name.tokens.joinToString("") { it.originalValue }
                            rows += "${name.token.line}:$written=${render(this.context.columnBinding(name))} / ${this.context.nameBinding(if (name.type === PlSqlGrammar.MEMBER_EXPRESSION) name.firstChild else name)}"
                        }
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source("u.sql", text), listOf(check), FileId("u.sql"))
        return rows
    }

    @Test
    fun theNamesOfADynamicText() {
        // The text sees no declaration of the procedure: zz there is no local variable, l.amount2 is the missing column.
        assertThat(dynamicAnswers(procedure(
            "execute immediate 'select qty, zz, l.amount2 from order_line l where order_id = :id' into v using 1;",
            "execute immediate 'select qty from stock_x';"
        ))).containsExactly(
            "8:qty=column SHOP.ORDER_LINE.QTY / NothingLocal(column possible)",
            "8:zz=plsql NO_SOURCE_HAS_IT / NothingLocal(column possible)",
            "8:l.amount2=column SHOP.ORDER_LINE.AMOUNT2 missing / NothingLocal(column possible)",
            "8:order_id=column SHOP.ORDER_LINE.ORDER_ID / NothingLocal(column possible)",
            "8::id=plsql BIND_VARIABLE / Unavailable(NOT_A_COVERED_NAME)",
            "9:qty=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column stock_x.QTY unverified / NothingLocal(column possible)"
        )
    }

    @Test
    fun aPlSqlBlockOfADynamicTextSeesItsOwnDeclarations() {
        // order_head has no qty: Oracle reads the field of the loop record l, as in static SQL; it has status.
        assertThat(dynamicAnswers(procedure(
            "execute immediate 'begin for l in (select 7 qty from dual) loop select l.qty into :x from order_head l; end loop; end;' using out v;",
            "execute immediate 'begin for l in (select 7 qty from dual) loop select l.status into :x from order_head l; end loop; end;' using out v;"
        )).filter { "l." in it }).containsExactly(
            "8:l.qty=plsql NO_SOURCE_HAS_IT / ColumnOrLocal(LOOP_INDEX@8:31)",
            "9:l.status=column SHOP.ORDER_HEAD.STATUS / ColumnOrLocal(LOOP_INDEX@9:31)"
        )
    }

    @Test
    fun aHoleOfADynamicText() {
        assertThat(dynamicAnswers(procedure(
            // A number standing as an operand, or a value inside a string literal of the text: only a value.
            "execute immediate 'select qty from order_line where order_id = ' || v into v;",
            "execute immediate 'select qty from order_line where amount = ''' || status || '''' into v;",
            // Any other value may write a condition, a source or an alias.
            "execute immediate 'select qty from order_line where ' || status into v;"
        )).map { it.substringBefore(" / ") }).containsExactly(
            "8:qty=column SHOP.ORDER_LINE.QTY",
            "8:order_id=column SHOP.ORDER_LINE.ORDER_ID",
            "8:ZPA$1=Unknown[DYNAMIC_SQL_HOLE]",
            "9:qty=column SHOP.ORDER_LINE.QTY",
            "9:amount=column SHOP.ORDER_LINE.AMOUNT",
            "10:qty=Unknown[DYNAMIC_SQL_HOLE]",
            "10:ZPA$1=Unknown[DYNAMIC_SQL_HOLE]"
        )
    }

    @Test
    fun aTextOfDbmsSqlParse() {
        // status of the text is the column: the procedure's variable is not visible there.
        assertThat(dynamicAnswers(procedure(
            "dbms_sql.parse(v, 'delete from order_head where status = ''X''', dbms_sql.native);"
        )).map { it.substringBefore(" / ") }).containsExactly("8:status=column SHOP.ORDER_HEAD.STATUS")
    }

    @Test
    fun theKnownMatchesOfOuterScopesAreCandidates() {
        // When stock_x lacks status, Oracle reads the one of order_head.
        assertThat(answers(procedure(
            "select count(*) into v from order_head h where exists (select 1 from stock_x where flag = status);",
            "select count(*) into v from order_line l where exists (select 1 from order_head h where exists (select 1 from stock_x where qty = 1));"
        )).filter { !it.contains("INTO_TARGET") && !it.contains("Unavailable") }).containsExactly(
            "8:flag=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column stock_x.FLAG unverified",
            "8:status=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column stock_x.STATUS unverified maybe column SHOP.ORDER_HEAD.STATUS",
            // order_head lacks qty: the next scope out has it.
            "9:qty=Unknown[SOURCE_COLUMNS_UNKNOWN] maybe column stock_x.QTY unverified maybe column SHOP.ORDER_LINE.QTY"
        )
    }

    @Test
    fun namesOutsideStaticSql() {
        assertThat(answers(procedure("v := qty;", "select fn_one(qty) into v from order_line;"))).containsExactly(
            "8:v=Unavailable(NOT_A_COVERED_NAME)",
            "8:qty=Unavailable(NOT_A_COVERED_NAME)",
            // A call: callBinding answers it.
            "9:fn_one=Unavailable(NOT_A_COVERED_NAME)",
            "9:qty=column SHOP.ORDER_LINE.QTY",
            "9:v=plsql INTO_TARGET"
        )
    }
}
