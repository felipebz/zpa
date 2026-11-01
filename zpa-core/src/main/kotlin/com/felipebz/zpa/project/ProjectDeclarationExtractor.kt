//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.extraction.DiscardedSourceText
import com.felipebz.zpa.project.extraction.UnitDispatcher
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Extracts the small, project-relevant declaration slice without constructing an AST.
 * This scanner deliberately understands declaration boundaries only; it does not parse
 * PL/SQL statements or expressions.
 */
fun interface ProjectDeclarationSourceExtractor {
    fun extract(fileId: FileId, source: String): List<ProjectDeclaration>
}

class ProjectDeclarationExtractor(
    configuration: PlSqlConfiguration = PlSqlConfiguration(StandardCharsets.UTF_8, true)
) : ProjectDeclarationSourceExtractor {
    /** FLR lexers keep mutable cursor state, so a concurrent preparation needs one per worker. */
    private val lexers = ThreadLocal.withInitial { PlSqlLexer.create(configuration) }

    override fun extract(fileId: FileId, source: String): List<ProjectDeclaration> = extractFile(fileId, source).declarations

    internal fun extractFile(fileId: FileId, source: String): ProjectFileExtraction {
        val tokens = lexers.get().lex(source)
        return UnitDispatcher(fileId, tokens, DiscardedSourceText(source)).extract()
    }
}

/**
 * Facts of one file plus name-only standalone coverage: the qualified names of standalone procedure/function
 * headings that were identified strictly but produced no fact. Only namespace evidence; never a candidate.
 */
internal class ProjectFileExtraction(
    val declarations: List<ProjectDeclaration>,
    omittedStandaloneCallables: Set<QualifiedName>,
    val alteredTypes: AlteredTypes = AlteredTypes.NONE
) {
    val omittedStandaloneCallables: Set<QualifiedName> = Collections.unmodifiableSet(HashSet(omittedStandaloneCallables))
}
