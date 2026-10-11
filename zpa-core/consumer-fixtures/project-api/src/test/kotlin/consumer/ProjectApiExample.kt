package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationRole
import com.felipebz.zpa.api.project.ProjectFunctionView
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution.Status
import com.felipebz.zpa.api.project.ProjectPackageTypeView
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation

/** Documentation example (Kotlin): prepare, resolve a type, inspect fields and callables, correlate a body. */
@OptIn(ZpaExperimentalApi::class)
fun describeShop(sources: List<ProjectSourceInput>): List<String> {
    val report = mutableListOf<String>()
    val snapshot = ProjectSnapshots.prepare(sources)
    val pkg = ProjectQualifiedName.of("shop")

    // Named type resolution: every status is handled; nothing picks "the first" candidate.
    val type = snapshot.resolveNamedType(ProjectQualifiedName.of("order_t"), pkg)
    when (type.status) {
        Status.RESOLVED -> {
            val declaration = type.declaration.get()
            if (declaration is ProjectPackageTypeView) {
                declaration.recordFields.forEach { report += "field ${it.ordinal}: ${it.name.originalSpelling} ${it.typeRef.name}" }
            }
        }
        Status.AMBIGUOUS -> report += "order_t is declared ${type.candidates.size} times in the supplied sources"
        Status.INCOMPLETE_INDEX -> report += "unknown: ${type.failures.map { it.fileId }} could not be read"
        Status.NOT_FOUND_IN_PROJECT -> report += "order_t is not in the supplied sources (not proof it is missing in Oracle)"
        Status.NOT_PREPARED -> report += "no project was prepared"
    }

    // Unranked candidates: specifications and bodies are all returned.
    val candidates = snapshot.findPackageSubprograms(pkg, ProjectIdentifier.fromSource("total"))
    candidates.declarations.forEach { candidate ->
        val returns = (candidate as? ProjectFunctionView)?.returnType?.name?.toString() ?: "-"
        report += "${candidate.role} total(${candidate.parameters.joinToString { it.name.originalSpelling }}) returns $returns"
    }

    // Correlation takes a BODY occurrence ID from the same snapshot.
    candidates.declarations.filter { it.role == ProjectDeclarationRole.BODY }.forEach { body ->
        val correlation = snapshot.correlateSpecification(body.id)
        report += when (correlation.status) {
            ProjectSpecificationCorrelation.Status.RESOLVED -> "body matches ${correlation.specification.get().sourceRange.fileId}"
            ProjectSpecificationCorrelation.Status.NOT_FOUND -> "no matching specification was supplied"
            ProjectSpecificationCorrelation.Status.AMBIGUOUS -> "${correlation.candidates.size} matching specifications"
            ProjectSpecificationCorrelation.Status.INCOMPLETE -> "unknown: some sources failed"
        }
    }
    return report
}
