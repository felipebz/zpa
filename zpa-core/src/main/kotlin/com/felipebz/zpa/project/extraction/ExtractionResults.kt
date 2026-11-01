//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.project.ProjectDeclaration

/** Result of parsing one unit: its declarations (possibly none) and the index where scanning resumes. */
internal data class ParsedUnit(val declarations: List<ProjectDeclaration>, val nextIndex: Int)

/** Result of parsing one member declaration and the index after it. */
internal data class ParsedDeclaration(val declaration: ProjectDeclaration, val nextIndex: Int)
