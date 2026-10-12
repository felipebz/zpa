package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.api.project.ProjectCallResolution.Status

/** Documentation example (Kotlin): a written two-segment name, resolved without picking an unsettled target. */
@OptIn(ZpaExperimentalApi::class)
fun describeWrittenCall(snapshot: ProjectSnapshot): List<String> {
    val arguments = ProjectCallArguments.of(listOf(
        ProjectCallArgument.positional(),
        ProjectCallArgument.named(ProjectIdentifier.fromSource("amount"))
    ))
    val resolution = snapshot.resolveCall(
        ProjectCallTarget.written(ProjectQualifiedName.of("billing", "charge")), ProjectCallKind.PROCEDURE, arguments
    )
    val report = mutableListOf<String>()
    report += when (resolution.status) {
        Status.RESOLVED -> "calls ${resolution.selected.get().declaration.qualifiedName}"
        Status.AMBIGUOUS -> "several overloads accept these arguments"
        Status.NO_APPLICABLE_CANDIDATE, Status.WRONG_KIND -> "no candidate accepts this call"
        Status.AMBIGUOUS_TARGET, Status.PARTIAL_DECLARATIONS, Status.INCOMPLETE_INDEX ->
            "not settled: ${resolution.uncertainties} ${resolution.failures.map { it.fileId }}"
        Status.UNKNOWN_PACKAGE, Status.NOT_FOUND_IN_PROJECT -> "not in the supplied sources"
        Status.INVALID_NOTATION -> "invalid notation at argument ${arguments.invalidArgumentOrdinal.asInt}"
        Status.NOT_PREPARED -> "no project was prepared"
    }
    resolution.candidates.forEach { candidate ->
        report += "${candidate.declaration.role} ${candidate.declaration.qualifiedName}: ${candidate.applicability}" +
            candidate.bindings.joinToString("") { " #${it.argumentOrdinal}->${it.parameter.name}" }
    }
    return report
}
