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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class MleModuleTest : RuleTest() {

    private val js = "export function f(x) { return x; }"

    private fun create(vararg statements: String) {
        setRootRule(DdlGrammar.CREATE_MLE_MODULE)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notCreate(vararg statements: String) {
        setRootRule(DdlGrammar.CREATE_MLE_MODULE)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    private fun alter(vararg statements: String) {
        setRootRule(DdlGrammar.ALTER_MLE_MODULE)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notAlter(vararg statements: String) {
        setRootRule(DdlGrammar.ALTER_MLE_MODULE)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    private fun unit(vararg statements: String) {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        for (s in statements) assertThat(p).describedAs(s).matches(s)
    }

    private fun notUnit(vararg statements: String) {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        for (s in statements) assertThat(p).describedAs(s).notMatches(s)
    }

    @Test
    fun matchesCreateModuleWithText() {
        create(
            "create mle module m language javascript as $js",
            "create or replace mle module m language javascript as $js",
            "create mle module if not exists scott.m language javascript as $js",
            "create mle module m language javascript version '1.0' as $js",
            "create mle module \"m\" language scott.javascript as\n  $js\n  console.log(\"it's\");"
        )
        unit("create or replace mle module hello_mod\nlanguage javascript as\n  export function hello(who){\n    return `Hello, \${who}`;\n  }\n/\n")
    }

    @Test
    fun matchesCreateModuleUsing() {
        create(
            "create mle module m language javascript using bfile (dir, 'f.js')",
            "create mle module m language javascript version '1' using clob (select src from t)",
            "create mle module m language javascript using blob (select src from t)"
        )
    }

    @Test
    fun doesNotTreatOrdinarySqlShapedLikeMleAsOpaqueSource() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        val statements = listOf(
            "select mle language from t t;",
            "select mle language pure from t t;",
            "select mle language x pure a a from dual;",
            "select a mle language from t t;",
            "select * from mle language;",
            "select mle module m language javascript as x from t;",
            "select 1 from mle module m language javascript using clob (select 1 from dual);",
            "select mle module m language javascript as export const a = 1; from dual;",
            "begin mle language x ~y~ ; end;",
            "create table t (mle number, language number, module number); select mle language from t t;",
            "create or replace view v as select mle module m language javascript as x from t;",
            "create mle module_name language javascript;",
            "create table mle module m language javascript as x"
        )
        for (statement in statements) {
            val types = lexer.lex(statement).map { it.type }
            assertThatAst(types).describedAs(statement)
                .doesNotContain(PlSqlTokenType.MLE_INLINE_SOURCE, PlSqlTokenType.MLE_MODULE_SOURCE)
        }
        unit(
            "select mle language from t t;",
            "select mle, language, module from t;",
            "select mle language from mle language;"
        )
    }

    @Test
    fun activatesOnlyInTheMleSyntacticPrefix() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        fun types(source: String) = lexer.lex(source).map { it.type }
        assertThatAst(types("create function f return varchar2 as mle language javascript {{ return 1; }};"))
            .contains(PlSqlTokenType.MLE_INLINE_SOURCE)
        assertThatAst(types("create function f return varchar2 is mle language javascript {{ return 1; }};"))
            .contains(PlSqlTokenType.MLE_INLINE_SOURCE)
        assertThatAst(types("create function f return varchar2 mle language javascript {{ return 1; }};"))
            .doesNotContain(PlSqlTokenType.MLE_INLINE_SOURCE)
        assertThatAst(types("create mle module m language javascript as\n$js\n/\n"))
            .contains(PlSqlTokenType.MLE_MODULE_SOURCE)
        assertThatAst(types("create or replace /*x*/ mle module m language javascript as\n$js\n/\n"))
            .contains(PlSqlTokenType.MLE_MODULE_SOURCE)
        assertThatAst(types("create or mle module m language javascript as\n$js\n/\n"))
            .doesNotContain(PlSqlTokenType.MLE_MODULE_SOURCE)
        assertThatAst(types("alter mle module m language javascript as\n$js\n/\n"))
            .doesNotContain(PlSqlTokenType.MLE_MODULE_SOURCE)
    }

    @Test
    fun allowsCommentsBetweenHeaderTokens() {
        unit(
            "create mle /*x*/ module m language javascript as\n$js\n/\n",
            "create mle module /*x*/ m language javascript as\n$js\n/\n",
            "create mle module m language /*x*/ javascript as\n$js\n/\n",
            "create mle module m -- c\n language javascript /*x*/ as\n$js\n/\n",
            "create mle module m language javascript version /*x*/ '1' as\n$js\n/\n",
            "create mle module m /*x*/ language /*y*/ javascript using /*z*/ clob (select 'x' from dual)",
            "create function f return varchar2 as mle /*x*/ language javascript {{ return 1; }};",
            "create function f return varchar2 as mle language /*x*/ javascript {{ return 1; }};",
            "create function f return varchar2 as mle language javascript /*x*/ {{ return 1; }};",
            "create function f return varchar2 as mle language javascript pure /*x*/ {{ return 1; }};",
            "create function f return varchar2 as mle language javascript /*x*/ pure {{ return 1; }};",
            "create function f return varchar2 as mle -- c\n language javascript {{ return 1; }};"
        )
        notUnit(
            "create function f return varchar2 as mle language javascript pure/*x*/ ;",
            "create function f return varchar2 as mle language javascript {{ return 1; ;"
        )
    }

    @Test
    fun emitsHeaderCommentsAsTriviaAndKeepsBodyCommentsInTheSource() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        val module = lexer.lex("create mle /*a*/ module m language /*b*/ javascript as\n/* body */ export const a = 1; -- tail\n/\n")
        assertThatAst(module.flatMap { t -> t.trivia.map { it.token.originalValue } }).containsExactly("/*a*/", "/*b*/")
        assertThatAst(module.first { it.type == PlSqlTokenType.MLE_MODULE_SOURCE }.value)
            .isEqualTo("/* body */ export const a = 1; -- tail")

        val inline = lexer.lex("create function f return varchar2 as mle language javascript /*c*/ {{ /* d */ return 1; }};")
        assertThatAst(inline.flatMap { t -> t.trivia.map { it.token.originalValue } }).containsExactly("/*c*/")
        assertThatAst(inline.first { it.type == PlSqlTokenType.MLE_INLINE_SOURCE }.value)
            .isEqualTo("{{ /* d */ return 1; }}")
    }

    @Test
    fun matchesVersionStringLiteralForms() {
        create(
            "create mle module m language javascript version q'[1.0]' as $js",
            "create mle module m language javascript version Q'<a'b>' as $js",
            "create mle module m language javascript version N'1.0' as $js",
            "create mle module m language javascript version n'1.0' as $js",
            "create mle module m language javascript version nq'{1.0}' as $js",
            "create mle module m language javascript version q'!1.0!' as $js",
            "create mle module m language javascript version 'a''b' as $js"
        )
        notCreate(
            "create mle module m language javascript version q'[1.0] as $js",
            "create mle module m language javascript version 1 as $js",
            "create mle module m language javascript version as $js"
        )
    }

    @Test
    fun matchesUsingFormsAcceptedByOracle() {
        create(
            "create mle module m language javascript using clob select src from t",
            "create mle module m language javascript using blob select utl_raw.cast_to_raw('x') from dual",
            "create mle module m language javascript using clob (with a as (select 'x' c from dual) select c from a)",
            "create mle module m language javascript using clob with a as (select 'x' c from dual) select c from a",
            "create mle module m language javascript using bfile (\"DIR\", 'f.js')",
            "create mle module m language javascript using bfile select 1 from dual"
        )
        notCreate(
            "create mle module m language javascript using bfile (select 1 from dual)",
            "create mle module m language javascript using bfile (dir, 'f.js', 1)",
            "create mle module m language javascript using bfile (dir 'f.js')",
            "create mle module m language javascript using 'x'"
        )
    }

    @Test
    fun keepsModuleTextOpaqueUntilTheTerminator() {
        unit(
            "create mle module m language javascript as\n// it's a comment with an unmatched quote\nexport const s = 'don''t \"';\n/\n",
            "create mle module m language javascript as\nexport const s = \"unterminated '\n/* ' */ export const t = `a ' b`;\n/\ncreate table t (a number);\n",
            "create mle module m language javascript as\nexport const x = 1; -- not sql ' \"\n/\nselect 1 from dual;\n"
        )
        val tree = p.parse("create mle module m language javascript as\n// it's\nexport const a = 1;\n/\nselect 1 from dual;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_MLE_MODULE)).hasSize(1)
        val tokens = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
            .lex("create mle module m language javascript as\n// it's\nexport const a = 1;\n/\n")
        val source = tokens.filter { it.type == PlSqlTokenType.MLE_MODULE_SOURCE }
        assertThatAst(source).hasSize(1)
        assertThatAst(source[0].value).isEqualTo("// it's\nexport const a = 1;")
    }

    @Test
    fun rejectsMalformedCreateModule() {
        notCreate(
            "create mle module m as $js",
            "create mle module m language javascript",
            "create mle module m language javascript $js",
            "create mle module m language",
            "create mle module language javascript as $js",
            "create mle module m version '1' language javascript as $js",
            "create mle module m language javascript version 1 as $js",
            "create mle module m language javascript version v1 as $js",
            "create mle module m language javascript using",
            "create mle module m language javascript using text (select 1 from dual)",
            "create mle module m language javascript using bfile (dir)",
            "create mle module m language javascript using bfile ('dir', 'f.js')",
            "create mle module m language javascript using clob",
            "create mle module m language javascript clob (select src from t)",
            "create mle module m language javascript bfile (dir, 'f.js')",
            "create mle module a.b.c language javascript as $js",
            "create public mle module m language javascript as $js",
            "create mle m language javascript as $js"
        )
    }

    @Test
    fun matchesAlterModuleMetadata() {
        alter(
            "alter mle module myMLEModule set metadata using clob (select json('{\"name\": \"value\"}'))",
            "alter mle module if exists scott.m set metadata using clob (select src from t)",
            "alter mle module m set metadata using clob select src from t",
            "alter mle module m set metadata using clob (select src from t);"
        )
        notAlter(
            "alter mle module m",
            "alter mle module set metadata using clob (select 1 from dual)",
            "alter mle module m set using clob (select 1 from dual)",
            "alter mle module m set metadata clob (select 1 from dual)",
            "alter mle module m set metadata using (select 1 from dual)",
            "alter mle module m set metadata using blob (select 1 from dual)",
            "alter mle module m set metadata using bfile (select 1 from dual)",
            "alter mle module m set metadata using clob",
            "alter mle module m metadata using clob (select 1 from dual)",
            "alter mle module m compile",
            "alter mle module m rename to n",
            "alter mle module if exists",
            "alter mle module a.b.c set metadata using clob (select 1 from dual)"
        )
    }

    @Test
    fun matchesModuleBackedCallSpecs() {
        unit(
            "create or replace function hello(\"p_who\" varchar2) return varchar2 as mle module hello_mod signature 'hello';",
            "create function f(x varchar2) return varchar2 is mle module scott.m env scott.e signature 'f';",
            "create function f(x varchar2) return varchar2 authid current_user deterministic as mle module m signature 'f(x)';",
            "create or replace procedure hello(\"p_who\" varchar2) as mle module hello_mod signature 'hello';",
            "create procedure p as mle module m env e signature '';"
        )
        notUnit(
            "create function f return varchar2 as mle module m signature 'f' pure;",
            "create function f return varchar2 as mle module m env e signature 'f' pure;",
            "create function f return varchar2 as mle module m;",
            "create function f return varchar2 as mle module m signature;",
            "create function f return varchar2 as mle module m signature f;",
            "create function f return varchar2 as mle module m signature 'f' env e;",
            "create function f return varchar2 as mle module m pure signature 'f';",
            "create function f return varchar2 as mle signature 'f';",
            "create function f return varchar2 as mle env e signature 'f';",
            "create function f return varchar2 as mle module a.b.c signature 'f';",
            "create function f return varchar2 as mle module m signature 'f' x;",
            "create function f return varchar2 as mle module m signature 'f' || 'g';",
            "create function f return varchar2 as mle module m signature 'f'"
        )
    }

    @Test
    fun matchesInlineCallSpecsWithAnyDelimiter() {
        val delimiters = listOf(
            "{{" to "}}", "[[" to "]]", "<<" to ">>", "((" to "))", "{" to "}", "(" to ")", "<[" to "]>", "({" to "})",
            "{{{" to "}}}", "\$\$" to "\$\$", "~" to "~", "##" to "##", "abc" to "abc", "xx" to "xx", "'" to "'",
            "`" to "`", "@@" to "@@", "||" to "||", "==" to "==", "{}" to "{}", ">>" to ">>"
        )
        for ((open, close) in delimiters) {
            unit("create function f(x varchar2) return varchar2 as mle language javascript $open return x; $close;")
            unit("create procedure p(x varchar2) as mle language javascript pure $open log; $close;")
        }
        unit(
            "create or replace function hello_inline(\"who\" varchar2) return varchar2\nas mle language javascript\n{{\n  return `Hello, \${who}`;\n}};\n/\n",
            "create or replace procedure hello_inline(\"who\" varchar2)\nas mle language javascript\n{{\n  console.log(`Hello, \${who}`);\n}};\n/\n",
            "create function f return varchar2 as mle language scott.javascript {{ return 1; }};",
            "create function f return varchar2 as mle language javascript {{ return x;}};",
            "create function f return varchar2 as mle language javascript {{ if (x) { return \"}\"; } return 'a'; }};",
            "create function f return varchar2 as mle language javascript\n{{ return x; }};\ncreate procedure p as begin null; end;"
        )
    }

    @Test
    fun rejectsMalformedInlineCallSpecs() {
        notUnit(
            "create function f return varchar2 as mle language javascript;",
            "create function f return varchar2 as mle language javascript {{return x; }};",
            "create function f return varchar2 as mle language javascript {{ return x; }",
            "create function f return varchar2 as mle language javascript {{ return x; ]];",
            "create function f return varchar2 as mle language javascript <[ return x; >];",
            "create function f return varchar2 as mle language javascript ab return x; ba;",
            "create function f return varchar2 as mle language javascript {{ return x; }} extra;",
            "create function f return varchar2 as mle language javascript {{ return x; }}",
            "create function f return varchar2 as mle language javascript {{ return \"}}\"; }};",
            "create function f return varchar2 as mle language javascript {{ return x; }} pure;",
            "create function f return varchar2 as mle language javascript {{{{ return x; }};",
            "create function f return varchar2 as mle language javascript pure{{ return x; }};",
            "create function f return varchar2 as mle language javascript {{}};",
            "create function f return varchar2 as mle language javascript /* return x; */;",
            "create function f return varchar2 as mle language javascript -- {{ return x; }}\n;",
            "create function f return varchar2 as mle language {{ return x; }};"
        )
    }

    @Test
    fun readsTheBodyAsOneOpaqueToken() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        val tokens = lexer.lex("as mle language scott.javascript pure {{ it's `x` /* \n 'y }} ;")
        assertThatAst(tokens.map { it.type }).containsExactly(
            PlSqlKeyword.AS, PlSqlKeyword.MLE, PlSqlKeyword.LANGUAGE, GenericTokenType.IDENTIFIER,
            com.felipebz.zpa.api.PlSqlPunctuator.DOT, GenericTokenType.IDENTIFIER, PlSqlKeyword.PURE,
            PlSqlTokenType.MLE_INLINE_SOURCE, com.felipebz.zpa.api.PlSqlPunctuator.SEMICOLON, GenericTokenType.EOF)
        assertThatAst(tokens[7].originalValue).isEqualTo("{{ it's `x` /* \n 'y }}")
    }

    @Test
    fun leavesOrdinaryUsesOfTheWordsAlone() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, false))
        val tokens = lexer.lex("select mle language, mle module from t")
        assertThatAst(tokens.none { it.type == PlSqlTokenType.MLE_INLINE_SOURCE }).isTrue()
        unit("select mle language from t;", "select 1 from dual;")
    }

    @Test
    fun exposesStatementAndCallSpecNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse("create mle module m language javascript as $js\n/\n" +
            "create function f(x varchar2) return varchar2 as mle module m signature 'f';\n/\n" +
            "create procedure p(x varchar2) as mle language javascript {{ console.log(x); }};\n/\n" +
            "alter mle module m set metadata using clob (select 'x' from dual);\n")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_MLE_MODULE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.ALTER_MLE_MODULE)).hasSize(1)
        assertThatAst(tree.getDescendants(PlSqlGrammar.MLE_DECLARATION)).hasSize(2)
        assertThatAst(tree.getDescendants(PlSqlGrammar.CALL_SPECIFICATION)).hasSize(2)
    }
}
