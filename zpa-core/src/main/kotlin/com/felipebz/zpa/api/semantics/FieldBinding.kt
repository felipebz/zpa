//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.semantics

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi

/**
 * The field a segment written after a value names (`code` of `g_rec.code`, `items` of `g_rows(1).items`, `inner`
 * and `code` of `l_out.inner.code`): a field of a record type, or an attribute of an object type, as zpa resolves it
 * from the value's declared type. A guess is never a [Field].
 */
@ZpaExperimentalApi
sealed interface FieldBinding {
    /** The segment names [declaration]. */
    class Field(val declaration: FieldDeclaration) : FieldBinding {
        override fun equals(other: Any?) = other is Field && declaration == other.declaration

        override fun hashCode() = declaration.hashCode()

        override fun toString() = "Field($declaration)"
    }

    /** Not decided; [reasons] says what the analysis could not see. Never a proof that the segment is not a field. */
    class Unknown(reasons: Set<UsageUncertainty>) : FieldBinding {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /**
     * Not a field: the node is not a segment written after a value, or the segment is proven something else (a
     * collection or object method, a name the value's type does not have, a column of a `%ROWTYPE`), or the file was
     * not analyzed.
     */
    class Unavailable(val reason: UsageUnavailability) : FieldBinding {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/**
 * A field of a record type or an attribute of an object type, [ordinal] zero-based in written order (an attribute
 * among its own type's attributes). Declared in this file ([fileId] null), the position is the field name's, as
 * [NameDeclaration] places a name; declared in another project file [fileId], it is the start of the field as the
 * project's declaration views give it (`ProjectRecordFieldView.sourceRange`).
 */
@ZpaExperimentalApi
class FieldDeclaration internal constructor(
    val kind: FieldDeclarationKind,
    val ordinal: Int,
    val line: Int,
    val column: Int,
    val fileId: String? = null
) {
    override fun equals(other: Any?) = other is FieldDeclaration && kind == other.kind && ordinal == other.ordinal &&
        line == other.line && column == other.column && fileId == other.fileId

    override fun hashCode() = (((kind.hashCode() * 31 + ordinal) * 31 + line) * 31 + column) * 31 + fileId.hashCode()

    override fun toString() = "$kind#$ordinal@" + (fileId?.let { "$it:" } ?: "") + "$line:$column"
}

/** What a [FieldDeclaration] is a member of. */
@ZpaExperimentalApi
enum class FieldDeclarationKind {
    /** A field of a record type. */
    RECORD_FIELD,

    /** An attribute of an object type. */
    ATTRIBUTE
}
