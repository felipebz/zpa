/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.symbols

import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.matchers.MethodMatcher.Companion.semantic
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.CharacterDatatype
import com.felipebz.zpa.api.symbols.datatype.NumericDatatype
import com.felipebz.zpa.api.symbols.datatype.RecordDatatype
import org.assertj.core.api.Assertions.*
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SymbolVisitorTest {

    @field:TempDir
    lateinit var tempFolder: File

    private val visitor = SymbolVisitor(DefaultTypeSolver(), ScopeImpl())

    @Test
    fun variableDeclaration() {
        val symbols = scan("""
declare
  variable number(5, 2);
  text varchar2(5);
begin
  variable := 1;
end;
""")
        assertThat(symbols).hasSize(2)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat((variable.datatype as NumericDatatype).length).isEqualTo(5)
        assertThat(variable.datatype.scale).isEqualTo(2)
        assertThat(variable.references).containsExactly(
            tuple(5, 3)
        )
        assertThat(variable.innerScope).isNull()

        val text = symbols.find("text", 3, 3)
        assertThat(text.type).isEqualTo(PlSqlType.CHARACTER)
        assertThat((text.datatype as CharacterDatatype).length).isEqualTo(5)
        assertThat(text.references).isEmpty()
        assertThat(text.innerScope).isNull()
    }

    @Test
    fun variableDeclarationWithSubtype() {
        val symbols = scan("""
declare
  subtype my_number is number;
  variable my_number;
begin
  variable := 1;
end;
""")
        assertThat(symbols).hasSize(2)

        val type = symbols.find("my_number", 2, 11)
        assertThat(type.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(type.innerScope).isNull()

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(
            tuple(5, 3))
        assertThat(variable.innerScope).isNull()
    }

    @Test
    fun associativeArray() {
        val symbols = scan("""
declare
  type my_array is table of number;
  variable my_array;
  n number;
begin
  variable := my_array();
  n := variable(1);
end;
""")
        assertThat(symbols).hasSize(3)

        val type = symbols.find("my_array", 2, 8)
        assertThat(type.type).isEqualTo(PlSqlType.ASSOCIATIVE_ARRAY)
        assertThat(type.innerScope).isNull()

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.ASSOCIATIVE_ARRAY)
        assertThat(variable.references).containsExactly(
            tuple(6, 3),
            tuple(7, 8))
        assertThat(variable.innerScope).isNull()

        val methodCall = variable.usages[1].getFirstAncestor(PlSqlGrammar.METHOD_CALL)
        assertThat(semantic(methodCall).plSqlType).isEqualTo(PlSqlType.NUMERIC)
        assertThat(semantic(methodCall.firstChild).plSqlType).isEqualTo(PlSqlType.ASSOCIATIVE_ARRAY)
    }

    @Test
    fun record() {
        val symbols = scan("""
declare
  type my_record is record (x number);
  variable my_record;
begin
  null;
end;
""")
        assertThat(symbols).hasSize(2)

        val type = symbols.find("my_record", 2, 8)
        assertThat(type.type).isEqualTo(PlSqlType.RECORD)
        assertThat(type.innerScope).isNull()

        val datatype = type.datatype as RecordDatatype
        assertThat(datatype.name).isEqualTo("MY_RECORD");

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.RECORD)
        assertThat(variable.references).isEmpty()
    }

    @Test
    fun recordInProcedure() {
        val symbols = scan("""
create or replace procedure my_proc is
  type my_record is record (x number);
begin
  null;
end;
""")
        assertThat(symbols).hasSize(2) // TODO

        val type = symbols.find("my_record", 2, 8)
        assertThat(type.type).isEqualTo(PlSqlType.RECORD)
        assertThat(type.innerScope).isNull()

        val datatype = type.datatype as RecordDatatype
        assertThat(datatype.name).isEqualTo("MY_RECORD");
    }

    @Test
    fun recordInPackage() {
        val symbols = scan("""
create or replace package my_pack is
  type my_record is record (x number);
end;
""")
        assertThat(symbols).hasSize(2) // TODO

        val type = symbols.find("my_record", 2, 8)
        assertThat(type.type).isEqualTo(PlSqlType.RECORD)
        assertThat(type.innerScope).isNull()

        val datatype = type.datatype as RecordDatatype
        assertThat(datatype.name).isEqualTo("MY_PACK.MY_RECORD");
    }

    @Test
    fun forLoop() {
        val symbols = scan("""
begin
  for i in 1..2 loop
    null;
  end loop;
end;
""")
        assertThat(symbols).hasSize(1)

        val i = symbols.find("i", 2, 7)
        assertThat(i.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(i.references).isEmpty()
        assertThat(i.innerScope).isNull()
    }

    @Test
    fun forLoopRedeclaringVariable() {
        val symbols = scan("""
declare
  i number;
begin
  for i in 1..2 loop
    foo(i);
  end loop;
  foo(i);
end;
""")
        assertThat(symbols).hasSize(2)

        val i = symbols.find("i", 2, 3)
        assertThat(i.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(i.references).containsExactly(
            tuple(7, 7))
        assertThat(i.innerScope).isNull()

        val i2 = symbols.find("i", 4, 7)
        assertThat(i2.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(i2.references).containsExactly(
            tuple(5, 9))
        assertThat(i2.innerScope).isNull()
    }

    @Test
    fun forLoopWithExplicitDatatype() {
        val symbols = scan("""
begin
  for i number(5) in 1..2 loop
    null;
  end loop;
end;
""")
        assertThat(symbols).hasSize(1)

        val i = symbols.find("i", 2, 7)
        assertThat(i.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat((i.datatype as NumericDatatype).length).isEqualTo(5)
        assertThat(i.references).isEmpty()
        assertThat(i.innerScope).isNull()
    }

    @Test
    fun forLoopPairsOf() {
        val symbols = scan("""
begin
  for i, v in pairs of x loop
    null;
  end loop;
end;
""")
        assertThat(symbols).hasSize(2)

        val i = symbols.find("i", 2, 7)
        assertThat(i.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(i.references).isEmpty()
        assertThat(i.innerScope).isNull()

        val v = symbols.find("v", 2, 10)
        assertThat(v.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(v.references).isEmpty()
        assertThat(v.innerScope).isNull()
    }

    @Test
    fun forAllLoop() {
        val symbols = scan("""
begin
  forall foo in 1 .. 2
  insert into tab values (var(foo).value);
end;
""")
        assertThat(symbols).hasSize(1)

        val foo = symbols.find("foo", 2, 10)
        assertThat(foo.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(foo.references).containsExactly(
            tuple(3, 31))
        assertThat(foo.innerScope).isNull()
    }

    @Test
    fun procedureArgument() {
        val symbols = scan("""
create procedure foo(x number) is
begin
  null;
end;
""")
        assertThat(symbols).hasSize(2)

        val foo = symbols.find("foo", 1, 18)
        assertThat(foo.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(foo.references).isEmpty()
        assertThat(foo.innerScope?.symbols).hasSize(1)

        val x = symbols.find("x", 1, 22)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).isEmpty()
        assertThat(x.innerScope).isNull()
    }

    @Test
    fun functionArgument() {
        val symbols = scan("""
create function foo(x number) return number is
begin
  null;
end;
""")
        assertThat(symbols).hasSize(2)

        val foo = symbols.find("foo", 1, 17)
        assertThat(foo.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(foo.references).isEmpty()
        assertThat(foo.innerScope?.symbols).hasSize(1)

        val x = symbols.find("x", 1, 21)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).isEmpty()
        assertThat(x.innerScope).isNull()
    }

    @Test
    fun packageSpecificationVariable() {
        val symbols = scan("""
create package pkg as
  variable number;
end;
""")
        assertThat(symbols).hasSize(2)

        val pkg = symbols.find("pkg", 1, 16)
        assertThat(pkg.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(pkg.references).isEmpty()
        assertThat(pkg.innerScope?.symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).isEmpty()
        assertThat(variable.innerScope).isNull()
    }

    @Test
    fun packageBodyVariable() {
        val symbols = scan("""
create package body pkg as
  variable number;
end;
""")
        assertThat(symbols).hasSize(2)

        val pkg = symbols.find("pkg", 1, 21)
        assertThat(pkg.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(pkg.references).isEmpty()
        assertThat(pkg.innerScope?.symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).isEmpty()
        assertThat(variable.innerScope).isNull()
    }

    @Test
    fun packageSpecAndBodyOnSameFile() {
        val symbols = scan("""
create package pkg as
  variable number;
end;
create package body pkg as
  variable2 number;
  
begin
  variable := 0;
  variable2 := 0;
end;
""")
        assertThat(symbols).hasSize(3)

        val pkg = symbols.find("pkg", 1, 16)
        assertThat(pkg.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(pkg.references).containsExactly(tuple(4, 21))
        assertThat(pkg.innerScope?.symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(tuple(8, 3))
        assertThat(variable.innerScope).isNull()

        val variable2 = symbols.find("variable2", 5, 3)
        assertThat(variable2.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable2.references).containsExactly(tuple(9, 3))
        assertThat(variable2.innerScope).isNull()

        assertThat(variable2.scope.outer).isEqualTo(variable.scope)
    }

    @Test
    fun cursor() {
        val symbols = scan("""
declare
  cursor cur(c number) is
    select 1 from dual;

  variable cur%rowtype;
begin
  open cur(1);
  fetch cur into variable;
  close cur;
end;
""")
        assertThat(symbols).hasSize(3)

        val cur = symbols.find("cur", 2, 10)
        assertThat(cur.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(cur.references).containsExactly(
            tuple(5, 12),
            tuple(7, 8),
            tuple(8, 9),
            tuple(9, 9))
        assertThat(cur.innerScope?.symbols).hasSize(1)

        val c = symbols.find("c", 2, 14)
        assertThat(c.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(c.references).isEmpty()
        assertThat(c.innerScope).isNull()

        val variable = symbols.find("variable", 5, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.ROWTYPE)
        assertThat(variable.references).containsExactly(
            tuple(8, 18))
        assertThat(variable.innerScope).isNull()
    }

    @Test
    fun exception() {
        val symbols = scan("""
declare
  ex exception;
begin
  null;
exception
  when ex then
    null;
end;
""")
        assertThat(symbols).hasSize(1)

        val ex = symbols.find("ex", 2, 3)
        assertThat(ex.type).isEqualTo(PlSqlType.EXCEPTION)
        assertThat(ex.references).containsExactly(
            tuple(6, 8))
        assertThat(ex.innerScope).isNull()
    }

    @Test
    fun trigger() {
        val symbols = scan("""
create trigger baz before insert or delete on tab for each row
declare
  variable number;
begin
  variable := :old.id;
end;
""")
        assertThat(symbols).hasSize(2)

        val baz = symbols.find("baz", 1, 16)
        assertThat(baz.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(baz.references).isEmpty()
        assertThat(baz.innerScope?.symbols).hasSize(1)

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(
            tuple(5, 3))
        assertThat(variable.innerScope).isNull()
        assertThat(variable.scope.identifier).isEqualTo("BAZ")
        assertThat(variable.scope.type).isEqualTo(PlSqlGrammar.CREATE_TRIGGER)
    }

    @Test
    fun fullVariableReference() {
        val symbols = scan("""
create procedure foo is
  x number;
begin
  x := 1;
  foo.x := 1;
  bar.x := 1;
end;
""")
        assertThat(symbols).hasSize(2)

        val foo = symbols.find("foo", 1, 18)
        assertThat(foo.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(foo.references).containsExactly(
            tuple(5, 3))
        assertThat(foo.innerScope?.symbols).hasSize(1)

        val x = symbols.find("x", 2, 3)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(4, 3),
            tuple(5, 7))
        assertThat(x.innerScope).isNull()
    }

    @Test
    fun fullVariableReferenceForOuterScope() {
        val symbols = scan("""
create procedure foo is
  x number;
  
  procedure bar is
  begin
    foo.x := 1;
  end;
begin
  null;
end;
""")
        assertThat(symbols).hasSize(3)

        val foo = symbols.find("foo", 1, 18)
        assertThat(foo.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(foo.references).containsExactly(
            tuple(6, 5))
        assertThat(foo.innerScope?.symbols).hasSize(2)

        val x = symbols.find("x", 2, 3)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(6, 9))
        assertThat(x.innerScope).isNull()

        val bar = symbols.find("bar", 4, 13)
        assertThat(bar.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(bar.references).isEmpty()
        assertThat(bar.innerScope?.symbols).isEmpty()
    }

    @Test
    fun variableReferenceWithPath() {
        val symbols = scan("""
procedure foo is
  cursor cur(x number) is
  select cur.x from dual;
begin
  null;
end;
/
""")
        assertThat(symbols).hasSize(3)

        val foo = symbols.find("foo", 1, 11)
        assertThat(foo.type).isEqualTo(PlSqlType.UNKNOWN)
        assertThat(foo.references).isEmpty()
        assertThat(foo.innerScope?.symbols).hasSize(1)

        val x = symbols.find("x", 2, 14)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(3, 14))
        assertThat(x.innerScope).isNull()
    }

    @Test
    fun symbolsWithQuotedIdentifiers() {
        val symbols = scan(
            """
declare
  "VAR" number;
  "Var" number;
  "var" number;
begin
  var := 1;
  "VAR" := 1;
  "Var" := 1;
  "var" := 1;
end;
"""
        )
        assertThat(symbols).hasSize(3)

        val var1 = symbols.find("var", 2, 3)
        assertThat(var1.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(var1.references).containsExactly(
            tuple(6, 3),
            tuple(7, 3),
        )
        assertThat(var1.innerScope).isNull()

        val var2 = symbols.find("\"Var\"", 3, 3)
        assertThat(var2.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(var2.references).containsExactly(
            tuple(8, 3),
        )
        assertThat(var2.innerScope).isNull()

        val var3 = symbols.find("\"var\"", 4, 3)
        assertThat(var3.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(var3.references).containsExactly(
            tuple(9, 3),
        )
        assertThat(var3.innerScope).isNull()
    }

    @Test
    fun variableDeclarationWithConstantVariableForLength() {
        val symbols = scan(
            """
declare
  const_length constant number := 20;
  const_scale constant number := 5;
  variable number(const_length, const_scale);
  text varchar2(const_length);
begin
  variable := 1;
end;
"""
        )
        assertThat(symbols).hasSize(4)

        val variable = symbols.find("variable", 4, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat((variable.datatype as NumericDatatype).length).isNull()
        assertThat(variable.datatype.scale).isNull()
        assertThat(variable.references).containsExactly(
            tuple(7, 3)
        )
        assertThat(variable.innerScope).isNull()

        val text = symbols.find("text", 5, 3)
        assertThat(text.type).isEqualTo(PlSqlType.CHARACTER)
        assertThat((text.datatype as CharacterDatatype).length).isNull()
        assertThat(text.references).isEmpty()
        assertThat(text.innerScope).isNull()
    }

    @Test
    fun selectWithFunctionDeclaration() {
        val symbols = scan(
            """
with 
  function func return number is 
  begin
    return 1; 
  end;
select func
"""
        )
        assertThat(symbols).hasSize(1)

        val func = symbols.find("func", 2, 12)
        assertThat(func.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(func.references).containsExactly(
            tuple(6, 8)
        )
        assertThat(func.scope.type).isEqualTo(DmlGrammar.SELECT_EXPRESSION)
        assertThat(func.innerScope).isNotNull()
    }

    private fun scan(contents: String): List<Symbol> {
        val file = tempFolder.resolve("test.sql")
        file.writeText(contents.trim())

        TestPlSqlVisitorRunner.scanFile(file, null, visitor)
        return visitor.symbols
    }

    private fun List<Symbol>.find(name: String, line: Int, column: Int): Symbol {
        val symbol = this.firstOrNull {
            it.name.equals(name, ignoreCase = true) &&
                it.declaration.token.line == line &&
                it.declaration.token.column == offset(column)
        }
        return symbol ?: fail("No symbol named $name was found at line $line and column ${offset(column)}")
    }

    private val Symbol.references: List<Tuple>
        get() = this.usages.map { tuple(it.token.line, it.token.column + 1) }

    private fun offset(offset: Int): Int {
        return offset - 1
    }

}
