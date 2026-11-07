//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.semantics.NameBinding
import com.felipebz.zpa.api.semantics.UsageUnavailability
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Runs [block] as an analysis outside zpa's own tests does: a failure of the local pass is logged, not thrown. For the
 * tests of that behavior, which zpa's test tasks otherwise turn into a failed scan.
 */
internal fun <T> withLocalPassFailuresLogged(block: () -> T): T = withLocalPassFailures("false", block)

private fun <T> withLocalPassFailures(value: String, block: () -> T): T {
    val original: String? = System.getProperty(LocalPassFailures.PROPERTY)
    System.setProperty(LocalPassFailures.PROPERTY, value)
    try {
        return block()
    } finally {
        if (original == null) System.clearProperty(LocalPassFailures.PROPERTY) else System.setProperty(LocalPassFailures.PROPERTY, original)
    }
}

/** A failure of the local pass fails zpa's own test runs, and is logged by any other analysis. */
class LocalPassFailuresTest {
    /** A file whose text can be read once, for the parser: the local pass, which reads it again, then throws. */
    private class ReadOnce(private val name: String, private val text: String, private val failing: Boolean) : PlSqlFile {
        private var reads = 0
        override fun contents(): String {
            if (failing && reads++ > 0) throw IllegalStateException("the text of $name cannot be read again")
            return text
        }
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val code = "declare\n  n number;\nbegin\n  n := 1;\nend;\n/\n"

    /** The binding of the first `n` assigned in each file, scanned in order with one scanner. */
    private fun scan(vararg files: ReadOnce): List<NameBinding> {
        val bindings = ArrayList<NameBinding>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                bindings += context.nameBinding(node.getFirstDescendant(PlSqlGrammar.ASSIGNMENT_STATEMENT)!!.firstChild)
            }
        }
        val scanner = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, ProjectAnalysisContext.NOT_PREPARED)
        files.forEach { scanner.scanFile(it, listOf(check), FileId(it.fileName())) }
        return bindings
    }

    @Test
    fun underZpasOwnTestsTheScanFailsNamingTheFileAndTheException() {
        withLocalPassFailures("true") {
            assertThatThrownBy { scan(ReadOnce("broken.sql", code, failing = true)) }
                .hasStackTraceContaining("Local semantic resolution failed while computing broken.sql")
                .rootCause().hasMessage("the text of broken.sql cannot be read again")
        }
    }

    @Test
    fun otherwiseTheFileIsNotAnalyzedAndTheOthersAre() {
        val bindings = withLocalPassFailuresLogged { scan(ReadOnce("good.sql", code, failing = false), ReadOnce("broken.sql", code, failing = true), ReadOnce("other.sql", code, failing = false)) }
        assertThat(bindings[1]).isEqualTo(NameBinding.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))
        assertThat(bindings[0]).isInstanceOf(NameBinding.Local::class.java)
        assertThat(bindings[2]).isInstanceOf(NameBinding.Local::class.java)
    }
}
