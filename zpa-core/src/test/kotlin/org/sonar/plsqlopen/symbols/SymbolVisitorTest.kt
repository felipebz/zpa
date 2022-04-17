/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2022 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import org.assertj.core.api.Assertions.*
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SymbolVisitorTest {

    private val visitor = SymbolVisitor(DefaultTypeSolver())

    @Test
    fun variableDeclaration() {
        val symbols = scan("""
declare
  variable number;
begin
  variable := 1;
end;
""")
        assertThat(symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(
            tuple(4, 3))
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

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(
            tuple(5, 3))
    }

    @Test
    fun associativeArray() {
        val symbols = scan("""
declare
  type my_array is table of number;
  variable my_array;
begin
  variable := my_array();
end;
""")
        assertThat(symbols).hasSize(2)

        val type = symbols.find("my_array", 2, 8)
        assertThat(type.type).isEqualTo(PlSqlType.ASSOCIATIVE_ARRAY)

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.ASSOCIATIVE_ARRAY)
        assertThat(variable.references).containsExactly(
            tuple(5, 3))
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

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.RECORD)
        assertThat(variable.references).isEmpty()
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

        val i2 = symbols.find("i", 4, 7)
        assertThat(i2.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(i2.references).containsExactly(
            tuple(5, 9))
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
    }

    @Test
    fun procedureArgument() {
        val symbols = scan("""
create procedure foo(x number) is
begin
  null;
end;
""")
        assertThat(symbols).hasSize(1)

        val x = symbols.find("x", 1, 22)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).isEmpty()
    }

    @Test
    fun functionArgument() {
        val symbols = scan("""
create function foo(x number) return number is
begin
  null;
end;
""")
        assertThat(symbols).hasSize(1)

        val x = symbols.find("x", 1, 21)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).isEmpty()
    }

    @Test
    fun packageSpecificationVariable() {
        val symbols = scan("""
create package pkg as
  variable number;
end;
""")
        assertThat(symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).isEmpty()
    }

    @Test
    fun packageBodyVariable() {
        val symbols = scan("""
create package body pkg as
  variable number;
end;
""")
        assertThat(symbols).hasSize(1)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).isEmpty()
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
        assertThat(symbols).hasSize(2)

        val variable = symbols.find("variable", 2, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(tuple(8, 3))

        val variable2 = symbols.find("variable2", 5, 3)
        assertThat(variable2.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable2.references).containsExactly(tuple(9, 3))

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

        val c = symbols.find("c", 2, 14)
        assertThat(c.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(c.references).isEmpty()

        val variable = symbols.find("variable", 5, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.ROWTYPE)
        assertThat(variable.references).containsExactly(
            tuple(8, 18))
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
        assertThat(symbols).hasSize(1)

        val variable = symbols.find("variable", 3, 3)
        assertThat(variable.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(variable.references).containsExactly(
            tuple(5, 3))
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
        assertThat(symbols).hasSize(1)

        val x = symbols.find("x", 2, 3)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(4, 3),
            tuple(5, 7))
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
        assertThat(symbols).hasSize(1)

        val x = symbols.find("x", 2, 3)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(6, 9))
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
        assertThat(symbols).hasSize(2)

        val x = symbols.find("x", 2, 14)
        assertThat(x.type).isEqualTo(PlSqlType.NUMERIC)
        assertThat(x.references).containsExactly(
            tuple(3, 14))
    }

    private fun scan(contents: String): List<Symbol> {
        TestPlSqlVisitorRunner.scan(contents, null, visitor)
        return visitor.symbols
    }

    private fun List<Symbol>.find(name: String, line: Int, column: Int): Symbol {
        val symbol = this.firstOrNull {
            it.name == name &&
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
