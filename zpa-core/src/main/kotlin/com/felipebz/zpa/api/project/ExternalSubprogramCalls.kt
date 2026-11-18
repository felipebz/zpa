//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.Outcome
import com.felipebz.zpa.api.project.ProjectBindingCompatibility.UnknownReason
import com.felipebz.zpa.api.project.ProjectCallNarrower.Evidence
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ParameterListBinding
import com.felipebz.zpa.project.ProjectCallArguments
import com.felipebz.zpa.project.ProjectCallCandidate
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.ProjectCallKind as InternalCallKind

/**
 * The result type of a call to a subprogram only the external metadata has: `pkg.f(…)`, `owner.pkg.f(…)` or a
 * standalone function, whose package or function the project does not declare. The name is resolved from the unit's
 * schema as Oracle resolves an object: an object of the schema, then (for a one-segment name the schema is known not
 * to hold) a public synonym; synonyms are followed to their target. STANDARD and DBMS_STANDARD are left to their own
 * rules.
 */
@OptIn(ZpaExperimentalApi::class)
internal class ExternalSubprogramCalls(private val snapshot: ProjectSnapshot, private val metadata: ProjectExternalMetadata) {
    private val objects = snapshot.objects(metadata)
    private val names = ObjectTypeNames(snapshot, metadata)
    private val hierarchy = ObjectTypeHierarchy(snapshot, metadata)
    private val shadowed = ProjectCallNarrower.shadowCheck(snapshot)

    /**
     * The datatype the call of the written name [segments] returns, with [actuals] written with [arguments]' notation:
     * the result every overload of that name shares, else the result of the one overload the actuals leave. Null when
     * the name reaches no subprogram of the metadata, or no single result.
     */
    fun result(segments: List<OracleIdentifier>, schema: ProjectIdentifier?, actuals: List<ProjectArgumentType>, arguments: ProjectCallArguments): ProjectExternalDatatype? {
        if (!metadata.origin.isPresent) return null
        val overloads = overloads(segments.map(ProjectIdentifier::wrap), schema).takeIf { it.isNotEmpty() } ?: return null
        if (overloads.any { !it.isFunction }) return null
        shared(overloads)?.let { return it }
        return narrowed(overloads, actuals, arguments)
    }

    /**
     * The result every one of [overloads] returns, when they agree on both the datatype and the package type. A result
     * of a type a package declares is never typed: null.
     */
    private fun shared(overloads: List<ProjectExternalSubprogram>): ProjectExternalDatatype? {
        val first = overloads.first()
        val same = overloads.all { it.result == first.result && it.resultPackageType == first.resultPackageType }
        return if (same) resultOf(first) else null
    }

    /** The result of [subprogram]; null for a type a package declares, which nothing here compares. */
    private fun resultOf(subprogram: ProjectExternalSubprogram): ProjectExternalDatatype? =
        if (subprogram.resultPackageType.isPresent) null else subprogram.result.orElse(null)

    private class Candidate(val subprogram: ProjectExternalSubprogram, val formals: List<Evidence>, val outcomes: List<Pair<Outcome, UnknownReason?>>, val proven: Boolean)

    /** As the project narrows its overloads: arity and notation, then the actuals' types, then the closest formal. */
    private fun narrowed(overloads: List<ProjectExternalSubprogram>, actuals: List<ProjectArgumentType>, arguments: ProjectCallArguments): ProjectExternalDatatype? {
        val evidence = actuals.map { ProjectCallNarrower.actual(it, shadowed) }
        val retained = overloads.mapNotNull { candidate(it, evidence, arguments) }
            .filter { c -> c.outcomes.none { it.first == Outcome.INCOMPATIBLE } }
        if (retained.size == 1) return resultOf(retained.single().subprogram)
        if (retained.isEmpty()) return null
        shared(retained.map { it.subprogram })?.let { return it }
        val remaining = ProjectCallNarrower.bindable(retained) { c -> c.outcomes.any { it.second == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (remaining.size == 1) return resultOf(remaining.single().subprogram)
        val proven = remaining.all { c -> c.proven && c.outcomes.all { it.first == Outcome.COMPATIBLE || it.second == UnknownReason.ENVIRONMENT_DEPENDENT } }
        if (!proven) return null
        val selected = ProjectCallNarrower.select(evidence, remaining.map { it.formals }, hierarchy) ?: return null
        return resultOf(remaining[selected].subprogram)
    }

    /**
     * The overload bound to the actuals, or null when they cannot bind to it. A parameter whose default is unknown is
     * taken to have one: omitting it does not reject the overload, but does not prove it either.
     */
    private fun candidate(subprogram: ProjectExternalSubprogram, actuals: List<Evidence>, arguments: ProjectCallArguments): Candidate? {
        val parameters = subprogram.parameters
        val declared = if (subprogram.isFunction) InternalCallKind.FUNCTION else InternalCallKind.PROCEDURE
        val name = { p: ProjectExternalSubprogramParameter -> p.name.identifier }
        val bound = ParameterListBinding.bind(declared, InternalCallKind.FUNCTION, parameters, name, { it.defaulted.orElse(true) }, arguments)
        if (bound.applicability != ProjectCallCandidate.Applicability.APPLICABLE) return null
        val certain = ParameterListBinding.bind(declared, InternalCallKind.FUNCTION, parameters, name, { it.defaulted.orElse(false) }, arguments)
        val formals = actuals.indices.map { formal(parameters[bound.formalIndices[it]]) }
        val outcomes = actuals.indices.map { ProjectCallNarrower.compare(actuals[it], formals[it], hierarchy) { outcome, reason -> outcome to reason } }
        return Candidate(subprogram, formals, outcomes, certain.applicability == ProjectCallCandidate.Applicability.APPLICABLE)
    }

    /**
     * The evidence of a parameter: a predefined type, or an object type the metadata or the project has. A type a
     * package of the dictionary declares is compared with nothing: the outcome is unknown, never incompatible.
     */
    private fun formal(parameter: ProjectExternalSubprogramParameter): Evidence {
        val datatype = parameter.datatype.orElse(null)
        if (datatype == null || parameter.packageType.isPresent) return Evidence(null, UnknownReason.FORMAL_INCONCLUSIVE)
        if (datatype.typeOwner.isPresent) {
            val type = names.ofDatatype(datatype) ?: return Evidence(null, UnknownReason.FORMAL_INCONCLUSIVE)
            return Evidence(ProjectDatatypeFamily.COMPOSITE, null, null, CompositeKind.SCHEMA_OBJECT, objectType = type)
        }
        val family = DatatypeFamilies.ofExternal(datatype) ?: return Evidence(null, UnknownReason.UNCLASSIFIED_TYPE)
        return Evidence(family, null, DatatypeFamilies.rankOfExternal(datatype))
    }

    /** The overloads the written name reaches, a package's subprogram or a standalone one. */
    private fun overloads(names: List<ProjectIdentifier>, schema: ProjectIdentifier?): List<ProjectExternalSubprogram> = when (names.size) {
        1 -> schema?.let { follow(it, names[0], unqualified = true) }?.takeIf { it.second == null }
            ?.let { metadata.findSubprograms(it.first, null, it.third) }.orEmpty()
        2 -> {
            // The first segment is an object of the schema or a public synonym before it is a schema, and a schema
            // only when nothing proves otherwise.
            val first = schema?.let { follow(it, names[0], unqualified = true) }
            when {
                first?.second != null -> inPackage(first, names[1])
                first != null || !schemaFirst(names[0], schema) -> emptyList()
                else -> follow(names[0], names[1], unqualified = false)?.takeIf { it.second == null }
                    ?.let { metadata.findSubprograms(it.first, null, it.third) }.orEmpty()
            }
        }
        3 -> if (!schemaFirst(names[0], schema)) emptyList()
        else follow(names[0], names[1], unqualified = false)?.takeIf { it.second != null }?.let { inPackage(it, names[2]) }.orEmpty()
        else -> emptyList()
    }

    /**
     * Whether the first segment [name] of a qualified name can be read as a schema from [schema]: Oracle reads it as an
     * object of the unit's schema, then as a public synonym, and as a schema only when neither takes it.
     */
    private fun schemaFirst(name: ProjectIdentifier, schema: ProjectIdentifier?): Boolean =
        snapshot.provesNoObjectNamed(name.identifier, schema, metadata)

    private fun inPackage(target: Triple<ProjectIdentifier, ProjectIdentifier?, ProjectIdentifier>, name: ProjectIdentifier): List<ProjectExternalSubprogram> {
        val owner = target.first
        val pkg = target.second!!
        if (owner == SYS && (pkg == STANDARD || pkg == DBMS_STANDARD)) return emptyList()
        return metadata.findSubprograms(owner, pkg, name)
    }

    /**
     * The package (owner, package, package) or standalone subprogram (owner, null, name) [name] reaches from
     * [schema]; null when it reaches something else, nothing, or an object the project declares.
     */
    private fun follow(schema: ProjectIdentifier, name: ProjectIdentifier, unqualified: Boolean): Triple<ProjectIdentifier, ProjectIdentifier?, ProjectIdentifier>? {
        val seen = HashSet<Pair<ProjectIdentifier, ProjectIdentifier>>()
        var owner = schema
        var current = name
        var public = unqualified
        while (seen.add(owner to current)) {
            // A package or a subprogram the project declares is never replaced by the dictionary's.
            if (snapshot.projectIndex.findInSchema(owner.identifier, QualifiedName(listOf(current.identifier))).isNotEmpty()) return null
            val found = objects.findObject(owner, current)
            val synonym = when {
                found == null && public && metadata.isSchemaComplete(owner) -> objects.findSynonym(ProjectExternalSynonym.PUBLIC, current) ?: return null
                found == null -> return null
                found.kind == ProjectExternalObject.Kind.SYNONYM -> objects.findSynonym(owner, current) ?: return null
                found.kind == ProjectExternalObject.Kind.PACKAGE -> return Triple(owner, current, current)
                found.kind == ProjectExternalObject.Kind.FUNCTION || found.kind == ProjectExternalObject.Kind.PROCEDURE -> return Triple(owner, null, current)
                else -> return null
            }
            if (synonym.dbLink.isPresent) return null
            owner = synonym.targetOwner.orElse(null) ?: return null
            current = synonym.targetName
            public = false
        }
        // Oracle rejects a synonym that resolves to itself (ORA-01775).
        return null
    }

    private companion object {
        val SYS: ProjectIdentifier = dictionaryIdentifier("SYS", "schema")
        val STANDARD: ProjectIdentifier = dictionaryIdentifier("STANDARD", "package")
        val DBMS_STANDARD: ProjectIdentifier = dictionaryIdentifier("DBMS_STANDARD", "package")
    }
}
