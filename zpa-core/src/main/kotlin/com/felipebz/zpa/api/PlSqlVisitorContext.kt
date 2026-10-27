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

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.RecognitionException
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.dynamicsql.DynamicSqlText
import com.felipebz.zpa.api.semantics.*
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.dynamicsql.DynamicSqlSupport
import com.felipebz.zpa.metadata.FormsMetadata
import com.felipebz.zpa.symbols.SymbolTableImpl
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
     * PL/SQL expression, or naming the cursor of `OPEN`, `FETCH` or `CLOSE`, binds among the file's declarations. A
     * qualified name there (a `MEMBER_EXPRESSION` such as `fn.v` or `block.v`) binds the declaration of its last segment. Only [NameBinding.NothingLocal] proves that no declaration of the file takes the name; in SQL a column
     * may still take it ([NameBinding.NothingLocal.sqlColumnPossible]). [NameBinding.Unknown] is never a proof.
     */
    @ZpaExperimentalApi
    fun nameBinding(name: AstNode): NameBinding =
        localSemanticFacts?.names?.binding(name) ?: NameBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED)

    /**
     * The category of the declared type of the value [name] denotes: a `VARIABLE_NAME`, or a `MEMBER_EXPRESSION` such as
     * `pkg.v`, `r.f` or `block.v`, read as a value in PL/SQL or SQL, or called (`v(i)` names the collection `v`).
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

}
