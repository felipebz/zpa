//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
    PACKAGE_FUNCTION,
    SEQUENCE,
    STANDALONE_PROCEDURE,
    STANDALONE_FUNCTION,
    PACKAGE_VARIABLE,
    PACKAGE_CONSTANT,
    PACKAGE_EXCEPTION,
    PACKAGE_CURSOR
}

sealed interface ProjectDeclaration {
    val kind: ProjectDeclarationKind
    val role: DeclarationRole
    val fileId: FileId
    val sourceRange: SourceRange
}

/** The full qualified name under which this fact is indexed for exact-name lookup. */
fun ProjectDeclaration.qualifiedName(): QualifiedName = when (this) {
    is PackageDeclaration -> name
    is StandaloneTypeDeclaration -> name
    is PackageTypeDeclaration -> owner.append(name)
    is PackageSubtypeDeclaration -> owner.append(name)
    is PackageSubprogramDeclaration -> owner.append(name)
    is SequenceDeclaration -> name
    is StandaloneProcedureDeclaration -> name
    is StandaloneFunctionDeclaration -> name
    is PackageValueDeclaration -> owner.append(name)
    is PackageExceptionDeclaration -> owner.append(name)
    is PackageCursorDeclaration -> owner.append(name)
}

data class PackageDeclaration(
    val name: QualifiedName,
    override val role: DeclarationRole,
    override val fileId: FileId,
    override val sourceRange: SourceRange,
    /** Callable coverage of this occurrence's declaration region; always supplied by the extractor, never assumed. */
    val callableCoverage: PackageCallableCoverage,
    /**
     * Member-name coverage of a specification's declaration region (names of declarations without facts, and gaps);
     * null when not recorded (package bodies), which never counts as complete.
     */
    val memberNames: PackageMemberNameCoverage? = null
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

data class SequenceDeclaration(
    val name: QualifiedName,
    override val fileId: FileId,
    override val sourceRange: SourceRange
) : ProjectDeclaration {
    override val kind = ProjectDeclarationKind.SEQUENCE
    override val role = DeclarationRole.STANDALONE
}
