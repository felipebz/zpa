//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.util.*

/**
 * An `ALL_TYPES` row with what the type itself declares: its attributes (`ALL_TYPE_ATTRS`) and methods
 * (`ALL_TYPE_METHODS`, `ALL_METHOD_PARAMS`, `ALL_METHOD_RESULTS`), neither inherited ones. What a subtype inherits is
 * read from its [supertype], whose own declarations follow the same rule. Names are exact dictionary names.
 */
@ZpaExperimentalApi
class ProjectExternalObjectType private constructor(
    val owner: ProjectIdentifier,
    val name: ProjectIdentifier,
    val kind: Kind,
    supertypeOwner: ProjectIdentifier?,
    supertypeName: ProjectIdentifier?,
    attributes: List<ProjectExternalTypeAttribute>,
    methods: List<ProjectExternalTypeMethod>
) {
    /** What `TYPECODE` says the type is. */
    enum class Kind {
        /** `OBJECT`: attributes and methods, possibly under a supertype. */
        OBJECT,

        /** `COLLECTION`: a nested table or a varray. */
        COLLECTION,

        /** Any other code (`ANYDATA`, `ANYTYPE`...): a type whose representation the dictionary does not show. */
        OPAQUE
    }

    /** `SUPERTYPE_OWNER`; empty for a type under no type. */
    val supertypeOwner: Optional<ProjectIdentifier> = Optional.ofNullable(supertypeOwner)

    /** `SUPERTYPE_NAME`; empty for a type under no type. */
    val supertypeName: Optional<ProjectIdentifier> = Optional.ofNullable(supertypeName)

    /** The attributes the type declares, in `ATTR_NO` order, inherited ones excluded. Runtime-unmodifiable. */
    val attributes: List<ProjectExternalTypeAttribute> = Collections.unmodifiableList(ArrayList(attributes))

    /** The methods the type declares, overloads included, inherited ones excluded. Runtime-unmodifiable. */
    val methods: List<ProjectExternalTypeMethod> = Collections.unmodifiableList(ArrayList(methods))

    /** The attribute [name] the type declares, when it declares one; an inherited attribute is its supertype's. */
    fun findAttribute(name: ProjectIdentifier): Optional<ProjectExternalTypeAttribute> =
        Optional.ofNullable(attributes.firstOrNull { it.name == name })

    /** The methods named [name] the type declares, every overload; empty when it declares none. Runtime-unmodifiable. */
    fun findMethods(name: ProjectIdentifier): List<ProjectExternalTypeMethod> =
        Collections.unmodifiableList(methods.filter { it.name == name })

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalObjectType && owner == other.owner && name == other.name && kind == other.kind &&
            supertypeOwner == other.supertypeOwner && supertypeName == other.supertypeName && attributes == other.attributes &&
            methods == other.methods

    override fun hashCode(): Int = Objects.hash(owner, name, kind, supertypeOwner, supertypeName, attributes, methods)

    override fun toString(): String =
        "$owner.$name $kind${supertypeName.map { " under ${supertypeOwner.map { o -> "$o." }.orElse("")}$it" }.orElse("")}"

    companion object {
        /**
         * @param owner `OWNER`
         * @param name `TYPE_NAME`
         * @param supertypeOwner `SUPERTYPE_OWNER`, null for a type under no type
         * @param supertypeName `SUPERTYPE_NAME`, null for a type under no type
         * @throws IllegalArgumentException for an invalid name, a supertype owner without its name or the reverse, or
         * two attributes with the same name
         */
        @JvmStatic
        fun of(
            owner: String,
            name: String,
            kind: Kind,
            supertypeOwner: String?,
            supertypeName: String?,
            attributes: List<ProjectExternalTypeAttribute>,
            methods: List<ProjectExternalTypeMethod>
        ): ProjectExternalObjectType {
            require((supertypeOwner == null) == (supertypeName == null)) { "A supertype needs both its owner and its name: $owner.$name" }
            val attributeList = attributes.toList()
            val methodList = methods.toList()
            require(attributeList.map { it.name }.toSet().size == attributeList.size) { "Duplicate attribute in $owner.$name" }
            return ProjectExternalObjectType(
                dictionaryIdentifier(owner, "schema"), dictionaryIdentifier(name, "type"), kind,
                supertypeOwner?.let { dictionaryIdentifier(it, "schema") }, supertypeName?.let { dictionaryIdentifier(it, "type") },
                attributeList, methodList
            )
        }
    }
}

/** An `ALL_TYPE_ATTRS` row: an attribute a type declares, with its datatype. */
@ZpaExperimentalApi
class ProjectExternalTypeAttribute private constructor(val name: ProjectIdentifier, datatype: ProjectExternalDatatype?) {
    /** Empty when the provider did not supply the datatype (a `REF` attribute, for example). */
    val datatype: Optional<ProjectExternalDatatype> = Optional.ofNullable(datatype)

    override fun equals(other: Any?): Boolean = other is ProjectExternalTypeAttribute && name == other.name && datatype == other.datatype

    override fun hashCode(): Int = Objects.hash(name, datatype)

    override fun toString(): String = "${name.originalSpelling} ${datatype.map { it.toString() }.orElse("?")}"

    companion object {
        /** @param dictionaryName `ATTR_NAME`; @param datatype the datatype, or null when unknown */
        @JvmStatic
        fun of(dictionaryName: String, datatype: ProjectExternalDatatype?): ProjectExternalTypeAttribute =
            ProjectExternalTypeAttribute(dictionaryIdentifier(dictionaryName, "attribute"), datatype)
    }
}

/**
 * An `ALL_TYPE_METHODS` row with its parameters and result. A member method has the parameter `SELF` first, which
 * [parameters] leaves out; a static method has none. A function has a result, a procedure none. A constructor is a
 * function named like its type that returns it.
 */
@ZpaExperimentalApi
class ProjectExternalTypeMethod private constructor(
    val name: ProjectIdentifier,
    /** The method is called on the type (`STATIC`), not on a value of it. */
    val isStatic: Boolean,
    parameters: List<ProjectExternalMethodParameter>,
    result: ProjectExternalDatatype?,
    /** The method is a function: it has a result, even one whose datatype is not supplied. */
    val isFunction: Boolean
) {
    /** The parameters after `SELF`, in `PARAM_NO` order. Runtime-unmodifiable. */
    val parameters: List<ProjectExternalMethodParameter> = Collections.unmodifiableList(ArrayList(parameters))

    /** The result datatype of a function; empty for a procedure, or when the provider did not supply it. */
    val result: Optional<ProjectExternalDatatype> = Optional.ofNullable(result)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalTypeMethod && name == other.name && isStatic == other.isStatic && parameters == other.parameters &&
            result == other.result && isFunction == other.isFunction

    override fun hashCode(): Int = Objects.hash(name, isStatic, parameters, result, isFunction)

    override fun toString(): String =
        "${if (isStatic) "static" else "member"} ${if (isFunction) "function" else "procedure"} ${name.originalSpelling}" +
            "(${parameters.joinToString()})${result.map { " return $it" }.orElse("")}"

    companion object {
        /** A function. @param dictionaryName `METHOD_NAME`; @param result the result datatype, or null when unknown */
        @JvmStatic
        fun function(dictionaryName: String, isStatic: Boolean, parameters: List<ProjectExternalMethodParameter>, result: ProjectExternalDatatype?) =
            ProjectExternalTypeMethod(dictionaryIdentifier(dictionaryName, "method"), isStatic, parameters.toList(), result, true)

        /** A procedure. @param dictionaryName `METHOD_NAME` */
        @JvmStatic
        fun procedure(dictionaryName: String, isStatic: Boolean, parameters: List<ProjectExternalMethodParameter>) =
            ProjectExternalTypeMethod(dictionaryIdentifier(dictionaryName, "method"), isStatic, parameters.toList(), null, false)
    }
}

/** An `ALL_METHOD_PARAMS` row other than `SELF`. */
@ZpaExperimentalApi
class ProjectExternalMethodParameter private constructor(val name: ProjectIdentifier, val mode: Mode, datatype: ProjectExternalDatatype?) {
    /** `PARAM_MODE`. */
    enum class Mode { IN, OUT, IN_OUT }

    /** Empty when the provider did not supply the datatype. */
    val datatype: Optional<ProjectExternalDatatype> = Optional.ofNullable(datatype)

    override fun equals(other: Any?): Boolean =
        other is ProjectExternalMethodParameter && name == other.name && mode == other.mode && datatype == other.datatype

    override fun hashCode(): Int = Objects.hash(name, mode, datatype)

    override fun toString(): String = "${name.originalSpelling} $mode ${datatype.map { it.toString() }.orElse("?")}"

    companion object {
        /** @param dictionaryName `PARAM_NAME`; @param datatype the datatype, or null when unknown */
        @JvmStatic
        fun of(dictionaryName: String, mode: Mode, datatype: ProjectExternalDatatype?): ProjectExternalMethodParameter =
            ProjectExternalMethodParameter(dictionaryIdentifier(dictionaryName, "parameter"), mode, datatype)
    }
}
