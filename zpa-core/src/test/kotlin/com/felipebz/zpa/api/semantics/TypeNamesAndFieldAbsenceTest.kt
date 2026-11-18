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
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.ConditionalCompilationSettings
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class TypeNamesAndFieldAbsenceTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private fun scan(file: String, vararg sources: Pair<String, String>, visit: (PlSqlVisitorContext, AstNode) -> Unit) {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, ConditionalCompilationSettings.NONE)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration)).prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false)
        )
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) = visit(context, node)
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, ConditionalCompilationSettings.NONE)
            .scanFile(Source(file, sources.toMap().getValue(file)), listOf(check), FileId(file))
    }

    private fun render(type: DeclaredType): String {
        val shape = when (type) {
            is DeclaredType.Record -> "$type ${type.fields}"
            is DeclaredType.Collection -> "$type element@${type.element?.token?.line}"
            else -> type.toString()
        }
        return "$shape ${type.category}"
    }

    private val stock = """
        create or replace procedure stock_check is
          procedure inner_step is
            type t_bin is record (shelf number, label varchar2(20));
            type t_bins is table of t_bin;
            type t_slots is varray(4) of number;
            type t_feed is ref cursor;
            v_bin t_bin;
            v_bins t_bins;
            v_slots t_slots;
            v_feed t_feed;
          begin
            null;
          end;
        begin
          null;
        end;
        /
        """.trimIndent() + "\n"

    @Test
    fun theNameOfALocalTypeAnswersAsADatatypeNamingIt() {
        val byName = LinkedHashMap<String, String>()
        val byDatatype = HashMap<String, String>()
        var variable: DeclaredType? = null
        scan("stock.sql", "stock.sql" to stock) { context, root ->
            for (declaration in root.getDescendants(
                PlSqlGrammar.RECORD_DECLARATION, PlSqlGrammar.TABLE_OF_DECLARATION, PlSqlGrammar.VARRAY_DECLARATION, PlSqlGrammar.REF_CURSOR_DECLARATION
            )) {
                val name = declaration.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
                byName[name.tokenValue] = render(context.declaredType(name))
            }
            for (declaration in root.getDescendants(PlSqlGrammar.VARIABLE_DECLARATION)) {
                val datatype = declaration.getFirstChild(PlSqlGrammar.DATATYPE)
                byDatatype[datatype.tokenValue] = render(context.declaredType(datatype))
            }
            variable = context.declaredType(root.getFirstDescendant(PlSqlGrammar.VARIABLE_DECLARATION).getFirstChild(PlSqlGrammar.IDENTIFIER_NAME))
        }
        assertThat(byName).containsExactly(
            entry("T_BIN", "Record(TYPE@3:9) [shelf=RECORD_FIELD#0@3:26, label=RECORD_FIELD#1@3:40] Record(RECORD_TYPE)"),
            entry("T_BINS", "Collection(TYPE@4:9, NESTED_TABLE) element@4 Collection(NESTED_TABLE of Record(RECORD_TYPE))"),
            entry("T_SLOTS", "Collection(TYPE@5:9, VARRAY) element@5 Collection(VARRAY of Scalar(NUMERIC))"),
            entry("T_FEED", "OtherLocal(TYPE@6:9) CursorVariable")
        )
        // The same answer, category included, as the datatype of a variable naming the type.
        assertThat(byDatatype).isEqualTo(byName)
        // The name of a variable is no type declaration.
        assertThat(variable).isEqualTo(DeclaredType.Unavailable(Unavailability.NOT_A_COVERED_NAME))
        assertThat(variable?.category).isEqualTo(ValueCategory.Unavailable(Unavailability.NOT_A_COVERED_NAME))
    }

    private val shape = "create or replace type t_shape as object (sides number) not final;\n/\n"
    private val square = "create or replace type t_square under t_shape (edge number);\n/\n"
    private val blob = "create or replace type t_blob under t_unseen_base (mass number);\n/\n"
    private val yard = """
        create or replace procedure yard_walk is
          type t_spot is record (row_no number);
          v_spot t_spot;
          v_sq t_square;
          v_bl t_blob;
          cursor c_full is select 1 lane, 2 depth from dual;
          cursor c_star is select d.*, 1 lane from dual d;
          n number;
        begin
          n := v_spot.row_no + v_spot.col_no;
          n := v_sq.edge + v_sq.sides + v_sq.corners;
          n := v_bl.mass + v_bl.corners;
          for r in c_full loop n := r.lane + r.width; end loop;
          for r in c_star loop n := r.lane + r.width; end loop;
        end;
        /
        """.trimIndent() + "\n"

    @Test
    fun aNameAWhollyKnownTypeLacksIsNoSuchField() {
        val rows = ArrayList<String>()
        scan("yard.sql", "shape.sql" to shape, "square.sql" to square, "blob.sql" to blob, "yard.sql" to yard) { context, root ->
            for (member in root.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION)) {
                val segment = member.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME).last()
                rows += "${segment.token.line}:${member.tokenValue}.${segment.tokenValue}=${answer(context.fieldBinding(segment))}"
            }
        }
        assertThat(rows).containsExactly(
            // A record type of the file.
            "10:V_SPOT.ROW_NO=Field", "10:V_SPOT.COL_NO=NoSuchField",
            // An object type whose whole supertype chain the project holds.
            "11:V_SQ.EDGE=Field", "11:V_SQ.SIDES=Field", "11:V_SQ.CORNERS=NoSuchField",
            // A supertype the project does not hold may have the name.
            "12:V_BL.MASS=Field", "12:V_BL.CORNERS=Unknown[DECLARATION_OUTSIDE_FILE]",
            // A cursor row whose select list names every item.
            "13:R.LANE=Field", "13:R.WIDTH=NoSuchField",
            // A `*` may give the name.
            "14:R.LANE=Field", "14:R.WIDTH=Unknown[UNSUPPORTED_REFERENCE]"
        )
    }

    private val bin = "create type bin_t as object (a number, member procedure p);\n/\n"
    private val binBody = """
        create type body bin_t as
          member procedure p is
            n number;
          begin
            ${'$'}if dbms_db_version.version >= 12 ${'$'}then
            n := qq_pkg.v;
            ${'$'}else
            n := qq_pkg.w;
            ${'$'}end
            select count(*) into n from dual where rownum = 1;
            n := self.a + self.zz;
          end;
        end;
        /
        """.trimIndent() + "\n"
    private val binUser = """
        create or replace procedure bin_use is
          o bin_t;
          n number;
        begin
          n := o.a + o.zz;
        end;
        /
        """.trimIndent() + "\n"

    @Test
    fun aBareNameInAMethodThatIsNoAttributeIsNotAMissingField() {
        fun answers(file: String): Map<String, String> {
            val rows = LinkedHashMap<String, String>()
            scan(file, "bin.sql" to bin, "bin_body.sql" to binBody, "bin_use.sql" to binUser) { context, root ->
                for (name in root.getDescendants(PlSqlGrammar.VARIABLE_NAME)) {
                    rows["${name.token.line}:${name.tokenValue}"] = answer(context.fieldBinding(name))
                }
                for (member in root.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION)) {
                    val segment = member.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME).last()
                    rows["${segment.token.line}:${member.tokenValue}.${segment.tokenValue}"] = answer(context.fieldBinding(segment))
                }
            }
            return rows
        }
        val body = answers("bin_body.sql")
        // Oracle reads these as a pseudocolumn and a package: not a member of the instance.
        assertThat(body["10:ROWNUM"]).isEqualTo("Unavailable(NOT_A_COVERED_NAME)")
        assertThat(body["6:QQ_PKG"]).isEqualTo("Unavailable(NOT_A_COVERED_NAME)")
        assertThat(body["11:SELF.A"]).isEqualTo("Field")
        assertThat(body["11:SELF.ZZ"]).isEqualTo("NoSuchField")
        val user = answers("bin_use.sql")
        assertThat(user["5:O.A"]).isEqualTo("Field")
        assertThat(user["5:O.ZZ"]).isEqualTo("NoSuchField")
    }

    private fun answer(binding: FieldBinding) = if (binding is FieldBinding.Field) "Field" else binding.toString()
}
