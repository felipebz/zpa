//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * An explicit cursor declared directly in a package specification; its [kind] is
 * [ProjectDeclarationKind.PACKAGE_CURSOR].
 *
 * [owner] is the explicit declaring package as written in the source; no owner or schema is inferred. [localName]
 * preserves the quoted/unquoted identifier semantics of [ProjectIdentifier]. [parameters] are the cursor's formal
 * parameters in declaration order (always `IN`), possibly empty. [returnType] is the written `RETURN` row type syntax,
 * retained unresolved, and is empty when the declaration has no `RETURN` clause; a `RETURN` clause gives the row's type.
 * The query itself is not retained; the names of its select list are, in [selectList].
 */
@ZpaExperimentalApi
interface ProjectPackageCursorView : ProjectDeclarationView {
    val owner: ProjectQualifiedName
    val localName: ProjectIdentifier
    val parameters: List<ProjectParameterView>
    val returnType: Optional<ProjectTypeReference>

    /**
     * The items of the select list of the cursor's query, in order; empty when the declaration writes no query (a
     * `RETURN` clause alone). A query zpa cannot read gives one item without a name covering the query.
     * Runtime-unmodifiable.
     */
    val selectList: Optional<List<ProjectSelectItemView>>
}

/**
 * One item of the select list of a cursor's query, as Oracle names the field of the cursor's row: a column by its name
 * (`l.line_no` names `LINE_NO`), an item with an alias by the alias (a quoted alias keeps its spelling). In a compound
 * query the names come from the first query block.
 */
@ZpaExperimentalApi
interface ProjectSelectItemView {
    /** Zero-based position of the item in the select list. */
    val ordinal: Int

    /**
     * The column name or alias as Oracle names the field; empty for `*`, `t.*` and an expression without an alias
     * (Oracle names such an expression by its text, which zpa does not reproduce).
     */
    val name: Optional<ProjectIdentifier>

    /** The range of the name, or of the whole item when it has none. */
    val sourceRange: ProjectSourceRange
}
