//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * One overload of a subprogram of the dictionary, as `ALL_ARGUMENTS` describes it: a subprogram of the package
 * [packageName] of [owner], or a standalone one when there is no package. A function has a [result]; a procedure has
 * none. Names are exact dictionary names.
 */
@ZpaExperimentalApi
class ProjectExternalSubprogram private constructor(
    val owner: ProjectIdentifier,
    packageName: ProjectIdentifier?,
    val name: ProjectIdentifier,
    overload: Int?,
    /** The subprogram is a function: it has a result, even one whose datatype is not supplied. */
    val isFunction: Boolean,
    parameters: List<ProjectExternalSubprogramParameter>,
    result: ProjectExternalDatatype?,
    resultPackageType: ProjectIdentifier?
) {
    /** `PACKAGE_NAME`; empty for a standalone subprogram. */
    val packageName: Optional<ProjectIdentifier> = Optional.ofNullable(packageName)

    /** `OVERLOAD`; empty for a subprogram that is not overloaded. */
    val overload: OptionalInt = overload?.let { OptionalInt.of(it) } ?: OptionalInt.empty()

    /** The parameters, in `POSITION` order. Runtime-unmodifiable. */
    val parameters: List<ProjectExternalSubprogramParameter> = Collections.unmodifiableList(ArrayList(parameters))

    /** The result datatype of a function; empty for a procedure, or when the provider did not supply it. */
    val result: Optional<ProjectExternalDatatype> = Optional.ofNullable(result)

    /**
     * `TYPE_SUBNAME` of the result: the type of the package [result] names, when the function returns a type a package
     * declares; empty for any other result. Two results are the same type only when both [result] and this are equal.
     */
    val resultPackageType: Optional<ProjectIdentifier> = Optional.ofNullable(resultPackageType)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalSubprogram && owner == other.owner && packageName == other.packageName && name == other.name &&
            overload == other.overload && isFunction == other.isFunction && parameters == other.parameters && result == other.result &&
            resultPackageType == other.resultPackageType

    override fun hashCode(): Int = Objects.hash(owner, packageName, name, overload, isFunction, parameters, result, resultPackageType)

    override fun toString(): String =
        "$owner.${packageName.map { "$it." }.orElse("")}$name${if (overload.isPresent) "#${overload.asInt}" else ""}" +
            "(${parameters.joinToString()})${result.map { " return $it" }.orElse("")}${resultPackageType.map { ".$it" }.orElse("")}"

    companion object {
        /**
         * A function. @param owner `OWNER`; @param packageName `PACKAGE_NAME`, null for a standalone function;
         * @param name `OBJECT_NAME`; @param overload `OVERLOAD`, null when not overloaded; @param result the datatype of
         * the row at position 0, or null when unknown; @param resultPackageType `TYPE_SUBNAME` of that row, or null
         * @throws IllegalArgumentException for an invalid name or two parameters with the same name
         */
        @JvmStatic
        @JvmOverloads
        fun function(
            owner: String,
            packageName: String?,
            name: String,
            overload: Int?,
            parameters: List<ProjectExternalSubprogramParameter>,
            result: ProjectExternalDatatype?,
            resultPackageType: String? = null
        ): ProjectExternalSubprogram = ProjectExternalSubprogram(
            dictionaryIdentifier(owner, "schema"),
            packageName?.let { dictionaryIdentifier(it, "package") },
            dictionaryIdentifier(name, "subprogram"),
            overload,
            true,
            distinct(owner, name, parameters),
            result,
            resultPackageType?.let { dictionaryIdentifier(it, "type") })

        /** A procedure; the arguments as for [function]. */
        @JvmStatic
        fun procedure(
            owner: String,
            packageName: String?,
            name: String,
            overload: Int?,
            parameters: List<ProjectExternalSubprogramParameter>
        ) =
            ProjectExternalSubprogram(
                dictionaryIdentifier(owner, "schema"),
                packageName?.let { dictionaryIdentifier(it, "package") },
                dictionaryIdentifier(name, "subprogram"),
                overload,
                false,
                distinct(owner, name, parameters),
                null,
                null
            )

        private fun distinct(owner: String, name: String, parameters: List<ProjectExternalSubprogramParameter>): List<ProjectExternalSubprogramParameter> {
            val list = parameters.toList()
            require(list.map { it.name }.toSet().size == list.size) { "Duplicate parameter in $owner.$name" }
            return list
        }
    }
}

/**
 * An `ALL_ARGUMENTS` row of a parameter (not the result, nor a part of a composite parameter). A parameter of a type
 * a package declares (a record, a table, a ref cursor) has the package as its datatype's name and owner, and the
 * type's own name as [packageType].
 */
@ZpaExperimentalApi
class ProjectExternalSubprogramParameter private constructor(
    val name: ProjectIdentifier,
    val mode: ProjectExternalMethodParameter.Mode,
    datatype: ProjectExternalDatatype?,
    packageType: ProjectIdentifier?,
    defaulted: Boolean?
) {
    /** Empty when the provider did not supply the datatype (a `%ROWTYPE`, for example). */
    val datatype: Optional<ProjectExternalDatatype> = Optional.ofNullable(datatype)

    /** `TYPE_SUBNAME`: the type of the package [datatype] names; empty for any other datatype. */
    val packageType: Optional<ProjectIdentifier> = Optional.ofNullable(packageType)

    /**
     * `DEFAULTED`: whether the parameter has a default; empty when the dictionary does not say. Omitting a parameter
     * whose default is unknown neither rejects nor proves an overload.
     */
    val defaulted: Optional<Boolean> = Optional.ofNullable(defaulted)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalSubprogramParameter && name == other.name && mode == other.mode && datatype == other.datatype &&
            packageType == other.packageType && defaulted == other.defaulted

    override fun hashCode(): Int = Objects.hash(name, mode, datatype, packageType, defaulted)

    override fun toString(): String =
        "${name.originalSpelling} $mode ${datatype.map { it.toString() }.orElse("?")}${packageType.map { ".$it" }.orElse("")}" +
            if (defaulted.orElse(false)) " default" else ""

    companion object {
        /**
         * @param dictionaryName `ARGUMENT_NAME`
         * @param datatype the datatype, or null when unknown
         * @param packageType `TYPE_SUBNAME`, or null
         * @param defaulted `DEFAULTED` as a boolean, or null when the dictionary does not say
         */
        @JvmStatic
        fun of(
            dictionaryName: String,
            mode: ProjectExternalMethodParameter.Mode,
            datatype: ProjectExternalDatatype?,
            packageType: String?,
            defaulted: Boolean?
        ): ProjectExternalSubprogramParameter = ProjectExternalSubprogramParameter(
            dictionaryIdentifier(dictionaryName, "parameter"), mode, datatype, packageType?.let { dictionaryIdentifier(it, "type") }, defaulted
        )
    }
}
