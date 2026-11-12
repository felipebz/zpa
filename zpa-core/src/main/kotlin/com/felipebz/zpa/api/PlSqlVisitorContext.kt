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
     * valueCategory and typeCategory.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
        "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
        "regionUncertainty, valueCategory and typeCategory."
    )
    @Suppress("DEPRECATION")
    var symbolTable: SymbolTable = SymbolTableImpl()
    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use the read queries of
     * PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, regionUncertainty,
     * valueCategory and typeCategory.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use the read queries of " +
        "PlSqlVisitorContext instead: nameBinding, usageEvidence, hidingEvidence, exceptionBinding, " +
        "regionUncertainty, valueCategory and typeCategory."
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
    fun dynamicSql(expression: AstNode): DynamicSqlText? = dynamicSqlSupport().fromExpression(expression)

    /**
     * The SQL text built by joining [parts] in order, each a node of this file classified as in [dynamicSql]: for a
     * text gathered from several places, such as the assignments of a variable. Cached per list of nodes.
     */
    @ZpaExperimentalApi
    fun dynamicSql(parts: List<AstNode>): DynamicSqlText? = dynamicSqlSupport().fromParts(parts)

    /**
     * Whether [declaration] is used, from the file's local name resolution, with the project's coverage of package
     * specifications in other files when the project is prepared. [declaration] is a node of this file: a
     * `VARIABLE_DECLARATION`, an `EXCEPTION_DECLARATION`, a `CURSOR_DECLARATION`, a `PARAMETER_DECLARATION` of a
     * subprogram with a body, or a `CURSOR_PARAMETER_DECLARATION`. A covered node the analysis holds no declaration for (the parameters of a call
     * specification, which an external routine receives, or of a heading without a body) is
     * [UsageUnavailability.NOT_A_COVERED_DECLARATION].
     *
     * [UsageEvidence.Unused] is the only proven absence; [UsageEvidence.Unknown] never is one. When the local analysis of
     * the file did not complete, the answer is [UsageEvidence.Unavailable] for every declaration: report nothing then.
     */
    @ZpaExperimentalApi
    fun usageEvidence(declaration: AstNode): UsageEvidence =
        localSemanticFacts?.usages?.evidence(declaration) ?: UsageEvidence.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

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
        localSemanticFacts?.hiding?.evidence(declaration) ?: HidingEvidence.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * The exception that [name] names: a `VARIABLE_NAME` or `MEMBER_EXPRESSION` written in a `RAISE` statement or as a
     * handler's exception in `WHEN`. User-defined exceptions are compared with [ExceptionBinding.Declared.exception];
     * a predefined exception and a local exception that redeclares its name are told apart.
     * [ExceptionBinding.Unknown] is never a proof.
     */
    @ZpaExperimentalApi
    fun exceptionBinding(name: AstNode): ExceptionBinding =
        localSemanticFacts?.exceptions?.binding(name) ?: ExceptionBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * What the analysis cannot see in the region that holds [node] (for a block, subprogram or trigger, the region of
     * its body): empty when that region, and every region inside it, is complete. Null when the file was not analyzed.
     */
    @ZpaExperimentalApi
    fun regionUncertainty(node: AstNode): Set<UsageUncertainty>? = localSemanticFacts?.exceptions?.regionUncertainty(node)

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
     *   `pkg.v`). In SQL, a table, view or alias of the statement that takes it gives [NameBinding.NothingLocal];
     * - a later one, an `IDENTIFIER_NAME`: the declaration it binds when every segment before it names a unit or a block
     *   (`g_rec` of `pkg.g_rec.f`, `p` of `fn.p`). A record field, a collection method or an object member after a
     *   value, an attribute after `%`, and the procedure a call statement names are [NameBinding.Unavailable].
     */
    @ZpaExperimentalApi
    fun nameBinding(name: AstNode): NameBinding =
        localSemanticFacts?.names?.binding(name) ?: NameBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

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
        localSemanticFacts?.names?.typeBinding(name) ?: NameBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

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
     *   `SET` of an `UPDATE`), the first child of its `METHOD_CALL`.
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
     * [CallBinding.Unavailable] answers:
     * - an unqualified name a SQL built-in function may take, which wins over the file's declarations in SQL, as
     *   [nameBinding] answers it;
     * - a call in the text of dynamic SQL;
     * - a method called after another call or an element (`f(1).m(x)`, `v(i).m(x)`), bound only while typing a value;
     * - a node that names no call (a variable, a qualifier segment, a datatype), and a file that was not analyzed.
     *
     * A method called through a value (`o.m(x)`) is answered for the written name as it was decided, which binds no
     * method: [CallBinding.KeptLocal] for a value of the file, the project's answer for the written name otherwise.
     */
    @ZpaExperimentalApi
    fun callBinding(name: AstNode): CallBinding =
        localSemanticFacts?.callBindings?.binding(name) ?: CallBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * The explicit cursor the name [name] denotes and, for `OPEN` and a cursor `FOR` loop, how its written actuals bind
     * to the cursor's parameters. [name] is the `VARIABLE_NAME`, or for a qualified name the `MEMBER_EXPRESSION`
     * (`pk.c`), written after `OPEN`, `FETCH` or `CLOSE`, as the control of a cursor `FOR` loop (the first child of its
     * `METHOD_CALL` when it has actuals), or before `%ROWTYPE`.
     *
     * A cursor declared in the file gives [CursorBinding.Local]. A cursor of a package specification in another project
     * file gives [CursorBinding.Project] with that cursor: named through its package (`pk.c`, `schema.pk.c`, as the
     * project reads the name from the unit's schema), or alone in the body of that package. A name the project names no
     * single cursor for gives [CursorBinding.Unknown], as does text the file may hold differently; a name proven to be
     * something else (a cursor variable, a package variable, a relation) gives [CursorBinding.Unavailable].
     *
     * The actuals of `OPEN` and of the loop are associated as the actuals of a call: positional ones in order, then
     * named ones, a parameter with a default left out ([CursorArguments.omittedParameters]); a list Oracle rejects says
     * why ([CursorArguments.Status]). No written list binds no actual. `FETCH`, `CLOSE` and `%ROWTYPE` pass none:
     * their [CursorBinding.Local.arguments] and [CursorBinding.Project.arguments] are null.
     */
    @ZpaExperimentalApi
    fun cursorBinding(name: AstNode): CursorBinding =
        cursors?.takeIf { localSemanticFacts != null }?.binding(name) ?: CursorBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * Which field [segment] names: a segment written after a value, as the `IDENTIFIER_NAME` of a qualified name
     * (`code` of `g_rec.code`, `inner` and `code` of `l_out.inner.code`, `code` of `pkg.v.code`) or the `VARIABLE_NAME`
     * of a member written after an element or a call result (`code` of `f().code`, `items` of `g_rows(1).items`).
     * A field of a record type or an attribute of an object type gives [FieldBinding.Field]; the first name of a chain,
     * a method, a name the value's type does not have and a column of a table's or view's `%ROWTYPE` give
     * [FieldBinding.Unavailable]; a value whose type is not decided gives [FieldBinding.Unknown].
     *
     * The row of a cursor's query (`v c%ROWTYPE`, `v pk.c%ROWTYPE`, the record of `FOR r IN c` or of a loop over a query)
     * has the fields its select list names: the item of that name is [FieldBinding.Field] with
     * [FieldDeclarationKind.SELECT_ITEM]. A name no item has is [FieldBinding.Unavailable] when every item has a known
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
     * [FieldBinding.Unknown] with [UsageUncertainty.DECLARATION_OUTSIDE_FILE], and text this file may hold differently
     * (a `${'$'}IF`, a substitution) leaves the member undecided.
     */
    @ZpaExperimentalApi
    fun fieldBinding(segment: AstNode): FieldBinding =
        fields?.takeIf { localSemanticFacts != null }?.binding(segment) ?: FieldBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * The fields of the record type whose name is [typeName] (the `IDENTIFIER_NAME` of a `TYPE … IS RECORD` of this
     * file, at any depth), in written order, each with the declaration [fieldBinding] answers for it. Null when
     * [typeName] names no record type the file's local analysis holds, or the analysis did not complete.
     */
    @ZpaExperimentalApi
    fun recordFields(typeName: AstNode): List<DeclaredField>? =
        fields?.takeIf { localSemanticFacts != null }?.recordFields(typeName)

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
     */
    @ZpaExperimentalApi
    fun valueCategory(name: AstNode): ValueCategory =
        if (localSemanticFacts == null) ValueCategory.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)
        else valueCategories?.category(name) ?: ValueCategory.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * The category of the type a `DATATYPE` node writes: the type of a variable, parameter or field declaration, or a
     * function's RETURN type, followed as [valueCategory] follows a value's type.
     */
    @ZpaExperimentalApi
    fun typeCategory(datatype: AstNode): ValueCategory =
        if (localSemanticFacts == null) ValueCategory.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)
        else valueCategories?.typeCategory(datatype) ?: ValueCategory.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * What the `DATATYPE` node [datatype] declares (the type of a variable, constant, parameter, record field or
     * collection element, or a function's RETURN type), from the same walk as [typeCategory]: subtypes and `%TYPE`
     * anchors are followed, a local declaration hides a name declared elsewhere, and a name declared in another file is
     * described by the prepared project, a `%TYPE` of a field of a package record value (`pk.g_rec.f%TYPE`, or
     * `g_rec.f%TYPE` in the package) included.
     *
     * A predefined type gives its normalized name and written constraints. A constraint written as an expression is
     * evaluated from constants the analysis decides: a constant of this file whose default is such an expression, or a
     * package constant of another file whose default is an integer literal. Any other expression gives
     * [com.felipebz.zpa.api.semantics.WrittenTypeConstraint.Unknown]. A record or collection type of this file gives
     * its fields, or its kind and element's `DATATYPE` node to ask again. A `%ROWTYPE` or a column's `%TYPE` gives the
     * anchor, described with the scan's external metadata when it has any; a field of a package value declared with a
     * relation's `%ROWTYPE` gives that column's `%TYPE`. [DeclaredType.Unknown] is never a proof.
     */
    @ZpaExperimentalApi
    fun declaredType(datatype: AstNode): DeclaredType =
        if (localSemanticFacts == null) DeclaredType.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)
        else valueCategories?.declaredType(datatype, fields) ?: DeclaredType.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

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
