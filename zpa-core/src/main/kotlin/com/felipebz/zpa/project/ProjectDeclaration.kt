package com.felipebz.zpa.project

enum class DeclarationRole {
    SPECIFICATION,
    BODY,
    STANDALONE
}

enum class ProjectDeclarationKind {
    PACKAGE,
    PACKAGE_BODY,
    STANDALONE_TYPE,
    PACKAGE_TYPE,
    PACKAGE_SUBTYPE,
    PACKAGE_PROCEDURE,
    PACKAGE_FUNCTION
}

enum class ProjectTypeShape {
    RECORD,
    OBJECT,
    COLLECTION,
    REF_CURSOR
}

enum class ParameterMode {
    IN,
    OUT,
    IN_OUT
}

data class ProjectParameter(
    val name: OracleIdentifier,
    val ordinal: Int,
    val mode: ParameterMode,
    val nocopy: Boolean,
    val typeRef: TypeRef,
    val defaultPresent: Boolean,
    val sourceRange: SourceRange
) {
    init {
        require(ordinal > 0) { "Parameter ordinals are one-based" }
    }
}

class ParameterOverloadIdentity(
    val name: OracleIdentifier,
    val typeKey: String
) {
    override fun equals(other: Any?): Boolean = other is ParameterOverloadIdentity &&
        name == other.name && typeKey == other.typeKey

    override fun hashCode(): Int = 31 * name.hashCode() + typeKey.hashCode()

    override fun toString(): String = "$name:$typeKey"
}

class ParameterHeaderIdentity(
    val name: OracleIdentifier,
    val mode: ParameterMode,
    val typeKey: String
) {
    override fun equals(other: Any?): Boolean = other is ParameterHeaderIdentity &&
        name == other.name && mode == other.mode && typeKey == other.typeKey

    override fun hashCode(): Int = 31 * (31 * name.hashCode() + mode.hashCode()) + typeKey.hashCode()

    override fun toString(): String = "$name:$mode:$typeKey"
}

/** Identity of a callable overload candidate. Oracle does not overload by mode or return type. */
class SubprogramOverloadIdentity(
    val kind: ProjectDeclarationKind,
    val name: QualifiedName,
    parameters: List<ParameterOverloadIdentity>
) {
    val parameters: List<ParameterOverloadIdentity> = immutableList(parameters)

    override fun equals(other: Any?): Boolean = other is SubprogramOverloadIdentity &&
        kind == other.kind && name == other.name && parameters == other.parameters

    override fun hashCode(): Int = 31 * (31 * kind.hashCode() + name.hashCode()) + parameters.hashCode()

    override fun toString(): String = "$kind:$name(${parameters.joinToString(",")})"
}

/** Identity used to correlate a specification declaration with its implementation header. */
class SubprogramHeaderIdentity(
    val overload: SubprogramOverloadIdentity,
    parameters: List<ParameterHeaderIdentity>,
    val returnTypeKey: String?
) {
    val parameters: List<ParameterHeaderIdentity> = immutableList(parameters)

    override fun equals(other: Any?): Boolean = other is SubprogramHeaderIdentity &&
        overload == other.overload && parameters == other.parameters && returnTypeKey == other.returnTypeKey

    override fun hashCode(): Int = 31 * (31 * overload.hashCode() + parameters.hashCode()) + (returnTypeKey?.hashCode() ?: 0)

    override fun toString(): String = "$overload[${parameters.joinToString(",")},return=$returnTypeKey]"
}

sealed interface ProjectDeclaration {
    val kind: ProjectDeclarationKind
    val role: DeclarationRole
    val fileId: FileId
    val sourceRange: SourceRange
}

data class PackageDeclaration(
    val name: QualifiedName,
    override val role: DeclarationRole,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    init {
        require(role == DeclarationRole.SPECIFICATION || role == DeclarationRole.BODY)
    }

    override val kind = if (role == DeclarationRole.SPECIFICATION) {
        ProjectDeclarationKind.PACKAGE
    } else {
        ProjectDeclarationKind.PACKAGE_BODY
    }
}

data class StandaloneTypeDeclaration(
    val name: QualifiedName,
    val shape: ProjectTypeShape?,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = ProjectDeclarationKind.STANDALONE_TYPE
    override val role = DeclarationRole.STANDALONE
}

data class PackageTypeDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val shape: ProjectTypeShape?,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = ProjectDeclarationKind.PACKAGE_TYPE
    override val role = DeclarationRole.SPECIFICATION
}

data class PackageSubtypeDeclaration(
    val owner: QualifiedName,
    val name: OracleIdentifier,
    val baseType: TypeRef,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = ProjectDeclarationKind.PACKAGE_SUBTYPE
    override val role = DeclarationRole.SPECIFICATION
}

sealed interface PackageSubprogramDeclaration : ProjectDeclaration {
    val owner: QualifiedName
    val name: OracleIdentifier
    val parameters: List<ProjectParameter>

    /** Stable overload identity; excludes mode, NOCOPY, defaults, and function return type. */
    fun overloadIdentity(): SubprogramOverloadIdentity = SubprogramOverloadIdentity(
        kind,
        owner.append(name),
        parameters.map { ParameterOverloadIdentity(it.name, it.typeRef.structuralKey()) }
    )

    /** Header-correlation identity; excludes NOCOPY and defaults but includes parameter mode. */
    fun headerIdentity(): SubprogramHeaderIdentity = SubprogramHeaderIdentity(
        overloadIdentity(),
        parameters.map { ParameterHeaderIdentity(it.name, it.mode, it.typeRef.structuralKey()) },
        (this as? PackageFunctionDeclaration)?.returnType?.structuralKey()
    )
}

class PackageProcedureDeclaration(
    override val owner: QualifiedName,
    override val name: OracleIdentifier,
    parameters: List<ProjectParameter>,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    override val role: DeclarationRole = DeclarationRole.SPECIFICATION
) : PackageSubprogramDeclaration {
    override val parameters: List<ProjectParameter> = immutableList(parameters)
    override val kind = ProjectDeclarationKind.PACKAGE_PROCEDURE
    init {
        require(role == DeclarationRole.SPECIFICATION || role == DeclarationRole.BODY)
    }

    override fun equals(other: Any?): Boolean = other is PackageProcedureDeclaration &&
        owner == other.owner && name == other.name && parameters == other.parameters &&
        fileId == other.fileId && sourceRange == other.sourceRange && role == other.role

    override fun hashCode(): Int = listOf(owner, name, parameters, fileId, sourceRange, role).hashCode()

    override fun toString(): String = "PackageProcedureDeclaration($owner.$name, $parameters, $fileId, $sourceRange)"
}

class PackageFunctionDeclaration(
    override val owner: QualifiedName,
    override val name: OracleIdentifier,
    parameters: List<ProjectParameter>,
    val returnType: TypeRef,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    override val role: DeclarationRole = DeclarationRole.SPECIFICATION
) : PackageSubprogramDeclaration {
    override val parameters: List<ProjectParameter> = immutableList(parameters)
    override val kind = ProjectDeclarationKind.PACKAGE_FUNCTION
    init {
        require(role == DeclarationRole.SPECIFICATION || role == DeclarationRole.BODY)
    }

    override fun equals(other: Any?): Boolean = other is PackageFunctionDeclaration &&
        owner == other.owner && name == other.name && parameters == other.parameters &&
        returnType == other.returnType && fileId == other.fileId && sourceRange == other.sourceRange && role == other.role

    override fun hashCode(): Int = listOf(owner, name, parameters, returnType, fileId, sourceRange, role).hashCode()

    override fun toString(): String = "PackageFunctionDeclaration($owner.$name, $parameters, $returnType, $fileId, $sourceRange)"
}
