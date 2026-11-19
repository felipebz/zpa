//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.api.project.ProjectTypeReference
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.dynamicsql.DynamicSqlSupport
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.symbols.SymbolTableImpl
import com.felipebz.zpa.symbols.local.LocalCursors
import com.felipebz.zpa.symbols.local.LocalFields
import com.felipebz.zpa.symbols.local.LocalSemanticFacts
import com.felipebz.zpa.symbols.local.LocalValueCategories

open class PlSqlVisitorContext private constructor(private val rootTree: AstNode?,
                                                   private val plSqlFile: PlSqlFile?,
                                                   private val parsingException: RecognitionException?,
                                                   var formsMetadata: FormsMetadata?) {
    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use the read queries of
     * PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty,
     * valueCategory and declaredType.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
        "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
        "regionUncertainty, valueCategory and declaredType."
    )
    @Suppress("DEPRECATION")
    var symbolTable: SymbolTable = SymbolTableImpl()
    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use the read queries of
     * PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty,
     * valueCategory and declaredType.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
        "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
        "regionUncertainty, valueCategory and declaredType."
    )
    @Suppress("DEPRECATION")
    var currentScope: Scope? = null

    constructor(rootTree: AstNode,
                plsqlFile: PlSqlFile?,
                metadata: FormsMetadata?) :
        this(rootTree, plsqlFile, null, metadata)

    constructor(plsqlFile: PlSqlFile?,
                parsingException: RecognitionException,
                metadata: FormsMetadata?) :
        this(null, plsqlFile, parsingException, metadata)

    fun rootTree() = rootTree

    fun plSqlFile() = plSqlFile

    fun parsingException() = parsingException

    /** The file's local semantic resolution, left by the pipeline's pre-pass; null when it did not run or failed. */
    internal var localSemanticFacts: LocalSemanticFacts? = null

    /** The categories of the file's named values, left by the pre-pass with the scan's project; null when it did not run. */
    internal var valueCategories: LocalValueCategories? = null

    /** The fields of the file's member segments, left by the pre-pass with the scan's project; null when it did not run. */
    internal var fields: LocalFields? = null

    /** The cursors of the file's cursor positions, left by the pre-pass with the scan's project; null when it did not run. */
    internal var cursors: LocalCursors? = null

    /** Installed by the scanner with the file's configuration and project; created on first use otherwise. */
    internal var dynamicSqlSupport: DynamicSqlSupport? = null

    /** For a dynamic SQL text built from known parts only: its tokens and their trivia are all of its text. */
    internal var tokensAreWholeText: Boolean = false

    private fun dynamicSqlSupport(): DynamicSqlSupport =
        dynamicSqlSupport ?: DynamicSqlSupport.standalone(plSqlFile).also { dynamicSqlSupport = it }

    /**
     * The SQL text that [expression] builds, parsed as one statement, or null when the expression has no string
     * literal, or the text does not parse as exactly one statement (with placeholders, or with every hole left out).
     *
     * [expression] is a node of this file, such as the text of `EXECUTE IMMEDIATE`, `OPEN … FOR` or `DBMS_SQL.PARSE`.
     * The operands of a concatenation are its parts; a string literal or `CHR(n)` gives known text, anything else is a
     * hole. Parsed only when asked, at most once per expression in a scan.
     */
    @ZpaExperimentalApi
    fun dynamicSql(expression: AstNode): DynamicSqlText? = dynamicSqlSupport().fromExpression(expression, ::isNumberValue)

    /**
     * The SQL text built by joining [parts] in order, each a node of this file classified as in [dynamicSql]: for a
     * text gathered from several places, such as the assignments of a variable. Cached per list of nodes.
     */
    @ZpaExperimentalApi
    fun dynamicSql(parts: List<AstNode>): DynamicSqlText? = dynamicSqlSupport().fromParts(parts, ::isNumberValue)

    /** Whether [expression], a hole of a dynamic text, is a number: such a value writes no name into the text. */
    private fun isNumberValue(expression: AstNode): Boolean = valueCategories?.isNumber(expression) == true

    /**
     * Whether [declaration] is used, from the file's local name resolution, with the project's coverage of package
     * specifications in other files when the project is prepared. [declaration] is a node of this file: a
     * `VARIABLE_DECLARATION`, an `EXCEPTION_DECLARATION`, a `CURSOR_DECLARATION`, a `PARAMETER_DECLARATION` of a
     * subprogram with a body, or a `CURSOR_PARAMETER_DECLARATION`. A covered node the analysis holds no declaration for (the parameters of a call
     * specification, which an external routine receives, or of a heading without a body) is
     * [Unavailability.NOT_A_COVERED_DECLARATION].
     *
     * [UsageEvidence.Unused] is the only proven absence; [UsageEvidence.Unknown] never is one. When the local analysis of
     * the file did not complete, the answer is [UsageEvidence.Unavailable] for every declaration: report nothing then.
     */
    @ZpaExperimentalApi
    fun usageEvidence(declaration: AstNode): UsageEvidence =
        localSemanticFacts?.usages?.evidence(declaration) ?: UsageEvidence.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * Which declaration the variable or exception [declaration] (a `VARIABLE_DECLARATION` or `EXCEPTION_DECLARATION` node
     * of this file) hides: the outermost variable, constant, exception, parameter, cursor parameter or loop index of the
     * same name visible where it is written, from the file's local name resolution and, when the project is prepared,
     * the package specification in another file. A declaration of the same block is a duplicate, not a hidden one.
     *
     * [HidingEvidence.HidesNothing] is the only proven absence; [HidingEvidence.Unknown] never is one. When the local
     * analysis of the file did not complete, the answer is [HidingEvidence.Unavailable]: report nothing then.
     */
    @ZpaExperimentalApi
    fun hidingEvidence(declaration: AstNode): HidingEvidence =
        localSemanticFacts?.hiding?.evidence(declaration) ?: HidingEvidence.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * The exception that [name] names: a `VARIABLE_NAME` or `MEMBER_EXPRESSION` written in a `RAISE` statement or as a
     * handler's exception in `WHEN`. User-defined exceptions are compared with [ExceptionBinding.Declared.exception];
     * a predefined exception and a local exception that redeclares its name are told apart.
     * [ExceptionBinding.Unknown] is never a proof.
     */
    @ZpaExperimentalApi
    fun exceptionBinding(name: AstNode): ExceptionBinding =
        localSemanticFacts?.exceptions?.binding(name) ?: ExceptionBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * What the analysis cannot see in the region that holds [node] (for a block, subprogram or trigger, the region of
     * its body): empty when that region, and every region inside it, is complete. Null when the file was not analyzed.
     */
    @ZpaExperimentalApi
    fun regionUncertainty(node: AstNode): Set<Uncertainty>? = localSemanticFacts?.exceptions?.regionUncertainty(node)

    /**
     * What [name], a `VARIABLE_NAME` read in an expression (in PL/SQL, or in SQL inside PL/SQL), naming a call in a
     * PL/SQL expression, naming the cursor of `OPEN`, `FETCH` or `CLOSE`, or naming what a cursor `FOR` loop iterates
     * over (`c` of `for r in c` and of `for r in c(10)`, whose actuals are read as any other name), binds among the
     * file's declarations. A qualified name there (a `MEMBER_EXPRESSION` such as `fn.v` or `block.v`) binds the
     * declaration of its last segment. In a package body whose specification is in another project file, a member of
     * that specification is [NameBinding.Local] with the specification's file when the project names one declaration of
     * it. Only [NameBinding.NothingLocal] proves that no declaration of the file takes the name; in SQL a column
     * may still take it ([NameBinding.NothingLocal.sqlColumnPossible]). [NameBinding.Unknown] is never a proof.
     *
     * Each written segment of such a qualified name, and of a method call statement (`v.delete;`), is answered on its
     * own node, whatever follows it:
     * - the first, a `VARIABLE_NAME`: what that name binds (`g_rec` of `g_rec.f`, `g_tab` of `g_tab.delete;`, `pkg` of
     *   `pkg.v`). In SQL, a table, view or alias of the statement that takes it gives [NameBinding.NothingLocal], or
     *   [NameBinding.ColumnOrLocal] when a declaration of the file also has the name: Oracle reads the declaration
     *   (`l.qty` of a loop record `l`) when that relation has no column of the next segment ([columnBinding] decides it);
     * - a later one, an `IDENTIFIER_NAME`: the declaration it binds when every segment before it names a unit or a block
     *   (`g_rec` of `pkg.g_rec.f`, `p` of `fn.p`). A record field, a collection method or an object member after a
     *   value, an attribute after `%`, and the procedure a call statement names are [NameBinding.Unavailable].
     *
     * For a call name and for the cursor of `OPEN`, `FETCH`, `CLOSE` or a cursor `FOR` loop, this answers among the
     * file's declarations only: [callBinding] and [cursorBinding] give the full answer, the project's declarations,
     * overloads and built-ins included.
     *
     * A name of a dynamic SQL text this context parsed ([dynamicSql]) is answered among the text's own declarations, as
     * Oracle compiles the text on its own: no declaration of the file is visible there.
     */
    @ZpaExperimentalApi
    fun nameBinding(name: AstNode): NameBinding {
        val facts = localSemanticFacts ?: return NameBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
        if (name.parentOrNull != null && rootTree != null) {
            val root = generateSequence(name) { it.parentOrNull }.last()
            if (root !== rootTree) {
                val text = dynamicSqlSupport?.textOf(root) ?: return NameBinding.Unavailable(Unavailability.NOT_A_COVERED_NAME)
                return text.context?.localSemanticFacts?.names?.binding(name) ?: NameBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
            }
        }
        return facts.names.binding(name)
    }

    /**
     * What the datatype name [name] binds among the file's declarations: the `VARIABLE_NAME`, or the `MEMBER_EXPRESSION`
     * such as `pkg.t_rec`, of the `CUSTOM_DATATYPE` of a declaration (`l_x t_rec;`, a parameter's, a return's or a field's
     * type, a collection's element type). A record, collection or ref cursor type, a subtype or an object type of the
     * file is [NameBinding.Local] with [NameDeclarationKind.TYPE], qualified or not (`pkg.t_rec` in the body of `pkg`).
     * In a package body whose specification is in another project file, a type or subtype of that specification, written
     * alone or qualified with the package's name, is [NameBinding.Local] with [NameDeclarationKind.TYPE] and the
     * specification's file and position; a type of an inner block or subprogram of the body hides it, and it is
     * [NameBinding.Unknown] when the project does not hold the specification or cannot decide the name.
     * [NameBinding.NothingLocal] proves that no declaration of the file takes the name (`other_pkg.t_rec`), and
     * [NameBinding.Unknown] is never a proof. A datatype written in an expression (`TREAT(x AS t)`, `SELF AS t`,
     * `RETURNING t`) and any other node are [NameBinding.Unavailable].
     */
    @ZpaExperimentalApi
    fun typeBinding(name: AstNode): NameBinding =
        localSemanticFacts?.names?.typeBinding(name) ?: NameBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * What [name], a `VARIABLE_NAME` or `MEMBER_EXPRESSION` written in static SQL of this file, reads, as Oracle resolves
     * it: an inner query before an outer one, a column of a source before a PL/SQL name, and a qualified name through the
     * source its qualifier names (an alias, else the relation's name or `schema.relation`). A source known to lack the
     * column passes the name to the enclosing query, then to PL/SQL. The scopes are those [nameBinding] uses.
     *
     * The answers:
     * - [ColumnBinding.Column]: a column of a relation of the statement. Its source carries the `TABLE_REFERENCE`, the
     *   alias and the project's reading of the relation through its synonyms and the scan's external metadata; whether
     *   the column exists is known when the metadata covers the relation. A name the metadata proves missing is still
     *   this column when no PL/SQL name takes it (Oracle rejects it, ORA-00904);
     * - [ColumnBinding.QueryColumn]: a column of an inline view or a `WITH` query, with the select item that gives it;
     * - [ColumnBinding.SelectItem]: an `ORDER BY` name that names a select item, which Oracle prefers to a column;
     * - [ColumnBinding.Source]: the qualifier of `t.*`, or the argument of `REF(t)` or `VALUE(t)`;
     * - [ColumnBinding.Pseudocolumn], [ColumnBinding.Joined] (a `USING` or `NATURAL` join column),
     *   [ColumnBinding.Ambiguous] (ORA-00918, ORA-00960);
     * - [ColumnBinding.PlSqlName]: no column takes it, for example when every source is known to lack it or the only
     *   relation is `DUAL`, or it is a bind variable (`:x`, `:new.col`); [nameBinding] answers what it binds;
     * - [ColumnBinding.Unknown]: a source whose columns are not known may take it, a `*` covers a relation the metadata
     *   does not cover, or hidden text lies inside the statement. Its candidates are the columns the name would read if
     *   such a source has it, then the columns of enclosing queries known to have it, never a proof. A qualified name
     *   whose source's columns are not known is undecided when a PL/SQL name (a record, a package variable) would take
     *   it were the source to lack the column, as for a source known to lack it; else it is that source's column.
     *
     * A name of a dynamic SQL text this context parsed ([dynamicSql]) is answered by the same rules, with the text's
     * statement as the outermost scope and its relations read in the compilation schema of the unit that holds the
     * text. Oracle compiles the text on its own: no declaration of the file takes a name. In a text that is a SQL
     * statement no PL/SQL declaration exists: a name no column takes is [ColumnBinding.PlSqlName], and [nameBinding]
     * answers [NameBinding.NothingLocal] for it there (at run time a function called without parentheses, or Oracle
     * rejects the statement, ORA-00904), and a qualified name whose source lacks the column is that missing column. In
     * a text that is a PL/SQL block, the block's own declarations are visible to its static SQL, and its names follow
     * the rules of static SQL with the text's [nameBinding]. A placeholder of a concatenated value zpa could not read, and
     * every name of the text when such a value may write more than a value (it is not inside a string literal of the
     * text, or not a number standing as an operand), is [ColumnBinding.Unknown] with [ColumnUncertainty.DYNAMIC_SQL_HOLE].
     * [ColumnUncertainty.DYNAMIC_SQL] remains for a text whose analysis is not available.
     *
     * A column written by `INSERT (…)`, the left side of `SET` or a `MERGE` insert list is the target's column. A name
     * marked for an outer join (`t.col(+)`, `col(+)`) is the name without the marker. A call (`f(x)`, `q.f(x)`) is
     * [ColumnBinding.Unavailable]: [callBinding] answers it; `q.col.method(x)` is the column `q.col`. A member of an
     * expression (`(t.obj).attr`, `f(x).attr`) is not a name: the names inside it are answered. Any other node, and a
     * name outside static SQL, is [ColumnBinding.Unavailable].
     */
    @ZpaExperimentalApi
    fun columnBinding(name: AstNode): ColumnBinding {
        if (localSemanticFacts == null) return ColumnBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
        val root = generateSequence(name) { it.parentOrNull }.last()
        if (root !== rootTree) {
            val text = dynamicSqlSupport?.textOf(root) ?: return ColumnBinding.Unavailable(Unavailability.NOT_A_COVERED_NAME)
            return valueCategories?.columnBinding(text, name) ?: ColumnBinding.Unknown(setOf(ColumnUncertainty.DYNAMIC_SQL))
        }
        return valueCategories?.columnBinding(name) ?: ColumnBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
    }

    /**
     * Whether an outer join can give nulls for the columns of [source], a source [columnBinding] answered, in the query
     * or statement that reads it: [source] is on the right side of an ANSI `LEFT` join or of `OUTER APPLY`, on the left
     * side of a `RIGHT` join (every source joined before it in the same join), on either side of a `FULL` join, a lateral
     * inline view marked `(+)`, or a source a `(+)` marks in its query's `WHERE` (`t.col(+) = …`). An outer join nested
     * in a parenthesized join counts for the sources inside it. True also when a name marked `(+)` in that `WHERE` is
     * not decided, or when the file was not analyzed. Only `false` is a proof: no outer join gives nulls for [source].
     */
    @ZpaExperimentalApi
    fun mayBeOuterJoined(source: ColumnSource): Boolean {
        if (localSemanticFacts == null) return true
        val root = generateSequence(source.node) { it.parentOrNull }.last()
        if (root !== rootTree) {
            val text = dynamicSqlSupport?.textOf(root) ?: return true
            return valueCategories?.mayBeOuterJoined(text, source) ?: true
        }
        return valueCategories?.mayBeOuterJoined(source) ?: true
    }

    /**
     * Whether an outer join is proven to give nulls for the columns of [source], in the places [mayBeOuterJoined]
     * lists. Only `true` is a proof; `false` also when a name marked `(+)` in that `WHERE` is not decided, or when the
     * file was not analyzed.
     */
    @ZpaExperimentalApi
    fun isOuterJoined(source: ColumnSource): Boolean {
        if (localSemanticFacts == null) return false
        val root = generateSequence(source.node) { it.parentOrNull }.last()
        if (root !== rootTree) {
            val text = dynamicSqlSupport?.textOf(root) ?: return false
            return valueCategories?.isOuterJoined(text, source) ?: false
        }
        return valueCategories?.isOuterJoined(source) ?: false
    }

    @OptIn(ZpaExperimentalApi::class)
    internal fun columnSource(reference: AstNode): ColumnSource? = valueCategories?.columnSource(reference)

    /**
     * What the file's local analysis decided for the call whose name is [name], computed for every call of the file
     * before any visitor runs: this is a lookup, nothing is resolved again.
     *
     * [name] is the `VARIABLE_NAME`, or for a qualified name the `MEMBER_EXPRESSION` (`pkg.p`, `schema.pkg.p`), that
     * names the callee:
     * - of a call statement, with or without arguments (`p;`, `pkg.p(1);`);
     * - of a call in a PL/SQL expression, the first child of its `METHOD_CALL` (`f(x)`, `pkg.f(x)`, the `f` of
     *   `f(1)(2)`); the same written form is also an element access (`v(1)`) or a constructor (`t(1)`);
     * - of a function named without parentheses, read as a value: a function of the file, or a qualified name the
     *   project knows as a function and not as a package variable (`pkg.f`);
     * - of a call written with arguments inside static SQL (a `SELECT` list, a `WHERE`, the `VALUES` of an `INSERT`, the
     *   `SET` of an `UPDATE`), the first child of its `METHOD_CALL`;
     * - of a function named without parentheses inside static SQL that no source of the statement takes
     *   ([columnBinding] answers it as a PL/SQL name): a qualified name (`pk.f`, `schema.f`, `schema.pk.f`) or an
     *   unqualified name the project knows as a function and not as a package variable, which Oracle calls there.
     *   When a source whose columns are not known may have a column of that name, the call is undecided
     *   ([CallKeptLocalReason.INCOMPLETE]).
     *
     * A name of the file's own subprograms gives [CallBinding.Local], with the subprogram the call binds or the reason
     * it is undecided. A name whose first segment no declaration of the file takes gives [CallBinding.Project], the
     * project's resolution narrowed by the types of the actuals. Any other call keeps [CallBinding.KeptLocal] with the
     * reason. The cursor of `OPEN c(1)` and of `FOR r IN c(1)` is no call: [cursorBinding] answers it.
     *
     * Inside SQL, SQL's name resolution applies. A column never takes the first segment of a call, so an actual a column
     * could take has no known type and narrows nothing; one no column can take (over `DUAL` alone, or in the `VALUES` of
     * a single-table `INSERT`) has its local declaration's type, as in PL/SQL. A relation of the statement takes the
     * first segment before a member and a method (`alias.column.method(x)`, [CallKeptLocalReason.SQL_RELATION]).
     * `alias.m(x)` is the method of the rows of an object table, else the function `m` of the package of that name:
     * [CallKeptLocalReason.SQL_RELATION] when the project has no such function, and
     * [CallKeptLocalReason.SQL_RELATION_OR_PROJECT] when it has one, since the relation's kind decides. A function of the file that is not stored (declared in a subprogram or block, or
     * private to a package body) is [CallKeptLocalReason.NOT_CALLABLE_IN_SQL]; one a package body defines for its
     * specification in another file is the project's call of that specification's function.
     *
     * A name of a dynamic SQL text this context parsed ([dynamicSql]) is answered by the same rules among the text's own
     * declarations, as Oracle compiles the text on its own: a text that is a SQL statement sees no PL/SQL declaration,
     * and calls a schema function or a function of a package.
     *
     * [CallBinding.Unavailable] answers:
     * - an unqualified name a SQL built-in function may take (with arguments, or without parentheses such as `SYSDATE`
     *   and `USER`), which wins over the file's declarations in SQL, as [nameBinding] answers it;
     * - a method called after another call or an element (`f(1).m(x)`, `v(i).m(x)`), bound only while typing a value;
     * - a node that names no call (a variable, a qualifier segment, a datatype), and a file that was not analyzed.
     *
     * A method called through a value (`o.m(x)`) is answered for the written name as it was decided, which binds no
     * method: [CallBinding.KeptLocal] for a value of the file, the project's answer for the written name otherwise.
     */
    @ZpaExperimentalApi
    fun callBinding(name: AstNode): CallBinding {
        val facts = localSemanticFacts ?: return CallBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
        if (name.parentOrNull != null && rootTree != null) {
            val root = generateSequence(name) { it.parentOrNull }.last()
            if (root !== rootTree) {
                val text = dynamicSqlSupport?.textOf(root) ?: return CallBinding.Unavailable(Unavailability.NOT_A_COVERED_NAME)
                return text.context?.localSemanticFacts?.callBindings?.binding(name) { columnBinding(it) }
                    ?: CallBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)
            }
        }
        return facts.callBindings.binding(name) { columnBinding(it) }
    }

    /**
     * The explicit cursor the name [name] denotes and, for `OPEN` and a cursor `FOR` loop, how its written actuals bind
     * to the cursor's parameters. [name] is the `VARIABLE_NAME`, or for a qualified name the `MEMBER_EXPRESSION`
     * (`pk.c`), written after `OPEN`, `FETCH` or `CLOSE`, as the control of a cursor `FOR` loop (the first child of its
     * `METHOD_CALL` when it has actuals), or before `%ROWTYPE`.
     *
     * A cursor declared in the file gives [CursorBinding.Local]. A cursor of a package specification in another project
     * file gives [CursorBinding.Project] with that cursor: named through its package (`pk.c`, `schema.pk.c`, as the
     * project reads the name from the unit's schema), from another unit as inside the body of that package, or alone in
     * that body. A cursor that the specification in another file declares with a RETURN clause alone, defined in the
     * body with its query and the same parameters, gives [CursorBinding.Local] with that definition inside the body,
     * named alone or through the package, as with the specification in the file. A name the project names no single
     * cursor for gives [CursorBinding.Unknown], as does text the file may hold differently (a cursor of the specification
     * under conditional compilation); a name proven to be something else (a cursor variable, a package variable, a
     * relation) gives [CursorBinding.Unavailable].
     *
     * The actuals of `OPEN` and of the loop are associated as the actuals of a call: positional ones in order, then
     * named ones, a parameter with a default left out ([CursorArguments.omittedParameters]); a list Oracle rejects says
     * why ([CursorArguments.Status]). No written list binds no actual. `FETCH`, `CLOSE` and `%ROWTYPE` pass none:
     * their [CursorBinding.Local.arguments] and [CursorBinding.Project.arguments] are null.
     */
    @ZpaExperimentalApi
    fun cursorBinding(name: AstNode): CursorBinding =
        cursors?.takeIf { localSemanticFacts != null }?.binding(name) ?: CursorBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * Which field [segment] names: a segment written after a value, as the `IDENTIFIER_NAME` of a qualified name
     * (`code` of `g_rec.code`, `inner` and `code` of `l_out.inner.code`, `code` of `pkg.v.code`) or the `VARIABLE_NAME`
     * of a member written after an element or a call result (`code` of `f().code`, `items` of `g_rows(1).items`).
     * A field of a record type or an attribute of an object type gives [FieldBinding.Field]. A name the value's type
     * is known completely not to have gives [FieldBinding.NoSuchField]: a record type of this file, a cursor's row whose
     * select list names every item, or an object type whose whole supertype chain is known. The first name of a chain,
     * a method and a column of a table's or view's `%ROWTYPE` give [FieldBinding.Unavailable]; a value whose type is
     * not decided, or whose type may have the name (a supertype not known), gives [FieldBinding.Unknown].
     *
     * The row of a cursor's query (`v c%ROWTYPE`, `v pk.c%ROWTYPE`, the record of `FOR r IN c` or of a loop over a query)
     * has the fields its select list names: the item of that name is [FieldBinding.Field] with
     * [FieldDeclarationKind.SELECT_ITEM]. A name no item has is [FieldBinding.NoSuchField] when every item has a known
     * name, and [FieldBinding.Unknown] when a `*`, a `t.*` or an expression without an alias may give it; a select list
     * that repeats a name gives a record Oracle rejects (PLS-00402), whose segments are all [FieldBinding.Unavailable].
     * A loop over a cursor with a `RETURN` type, and the `%ROWTYPE` of a package cursor declared with a `RETURN` type
     * and no query, have the `RETURN` type's fields.
     *
     * In a member method or a constructor of a type body, a name written alone or first in a chain (`items` of
     * `items(1) := 2` or `items.extend`) that is an attribute of the type, its own or a supertype's, is the instance's:
     * it is answered as `SELF.items` would be there, with the same declaration and no more certainty. A parameter or a
     * local of that name takes it first, a static method has no instance, and in SQL a column may take it first.
     *
     * After a value whose type is declared in another file (a package variable of another file, an element, a call
     * result, a parameter of a project type), the member is the one the project declares for that type, as the local
     * type walk of [valueCategory] reaches the value: a value of a type the project does not hold gives
     * [FieldBinding.Unknown] with [Uncertainty.DECLARATION_OUTSIDE_FILE], and text this file may hold differently
     * (a `${'$'}IF`, a substitution) leaves the member undecided.
     */
    @ZpaExperimentalApi
    fun fieldBinding(segment: AstNode): FieldBinding =
        fields?.takeIf { localSemanticFacts != null }?.binding(segment) ?: FieldBinding.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * The category of the declared type of the value [name] denotes: a `VARIABLE_NAME`, or a `MEMBER_EXPRESSION` such as
     * `pkg.v`, `r.f` or `block.v`, read as a value in PL/SQL or SQL, or called (`v(i)` names the collection `v`). In
     * PL/SQL, and in the `VALUES` of a single-table `INSERT`, also an `OBJECT_REFERENCE`, a member written after an
     * element or a call result (`v(i).f`, `f(1).g.h`, `o.m().a`), and each of its member items after the first child:
     * the value of the chain up to that item. The element is typed by its collection's element type, one step per
     * argument list; a call must bind one function or method, and a call nothing else takes that names an object type
     * is its constructor, whose result is that type. A call that binds no single candidate is unknown: Oracle rejects
     * it, or the candidates differ only by argument types, which this analysis does not compare.
     * Subtypes and `%TYPE` anchors are followed; a declaration in another file is described by the prepared project.
     * [ValueCategory.Unknown] is never a proof of any category.
     *
     * With Oracle Forms metadata ([formsMetadata]), a host variable written `:qualifier.name` (the `MEMBER_EXPRESSION`)
     * is a [ValueCategory.Scalar]: an item of a block or a parameter of the form by the data type the metadata states
     * for it (CHAR and LONG character, NUMBER numeric, DATE and DATETIME datetime), a global always a character string,
     * and a system variable a character string when Forms documents it as one. The same type is the evidence of such a
     * host variable written as a call actual, for [callBinding]. Without metadata, or without a data type Forms
     * documents for a reference, the host variable is not typed.
     */
    @ZpaExperimentalApi
    fun valueCategory(name: AstNode): ValueCategory =
        if (localSemanticFacts == null) ValueCategory.Unavailable(Unavailability.FILE_NOT_ANALYZED)
        else valueCategories?.category(name) ?: ValueCategory.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * What a type declares, from one of two starting points:
     * - a `DATATYPE` node (the type of a variable, constant, parameter, record field or collection element, or a
     *   function's RETURN type, or a type written in an expression or a SQL column definition);
     * - the `IDENTIFIER_NAME` of a `TYPE … IS RECORD`, `IS TABLE OF`, `IS VARRAY` or `IS REF CURSOR` of this file, at any
     *   depth, answered as a `DATATYPE` naming that type is.
     *
     * Any other node gives [DeclaredType.Unavailable]. Subtypes and `%TYPE` anchors are followed, a local declaration
     * hides a name declared elsewhere, and a name declared in another file is described by the prepared project, a
     * `%TYPE` of a field of a package record value (`pk.g_rec.f%TYPE`, or `g_rec.f%TYPE` in the package) included.
     *
     * A predefined type gives its normalized name and written constraints. A constraint written as an expression is
     * evaluated from constants the analysis decides: a constant of this file whose default is such an expression, or a
     * package constant of another file whose default is an integer literal. Any other expression gives
     * [com.felipebz.zpa.api.semantics.WrittenTypeConstraint.Unknown]. A record type of this file gives its fields in
     * written order, each with the declaration [fieldBinding] gives for it; a collection type its kind and element's
     * `DATATYPE` node to ask again. A `%ROWTYPE` or a column's `%TYPE` gives the anchor, described with the scan's
     * external metadata when it has any; a field of a package value declared with a relation's `%ROWTYPE` gives that
     * column's `%TYPE`. [DeclaredType.Unknown] is never a proof.
     *
     * Every answer, [DeclaredType.Unknown] and [DeclaredType.Unavailable] included, has its [DeclaredType.category]:
     * the category of a value of the type, followed as [valueCategory] follows a value's type.
     */
    @ZpaExperimentalApi
    fun declaredType(node: AstNode): DeclaredType =
        if (localSemanticFacts == null) DeclaredType.Unavailable(Unavailability.FILE_NOT_ANALYZED)
        else valueCategories?.declaredType(node, fields) ?: DeclaredType.Unavailable(Unavailability.FILE_NOT_ANALYZED)

    /**
     * The type reference the prepared project retains for the declaration whose written type is [datatype], a
     * `DATATYPE` node of this file: a package specification's variable, constant, formal, record field or RETURN type,
     * or a standalone unit's formal or RETURN type. Null when the project does not index that declaration (a local
     * variable of a body, a project that is not prepared) or [datatype] is another node.
     */
    @ZpaExperimentalApi
    fun projectTypeReference(datatype: AstNode): ProjectTypeReference? =
        localSemanticFacts?.let { valueCategories?.projectTypeReference(datatype) }

}
