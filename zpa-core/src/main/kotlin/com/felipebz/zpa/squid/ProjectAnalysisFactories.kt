//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectAnalysis
import com.felipebz.zpa.project.ProjectAnalysisContext

@OptIn(ZpaExperimentalApi::class)
@JvmSynthetic
internal fun createProjectAnalysis(projectAnalysisContext: ProjectAnalysisContext): ProjectAnalysis =
    ProjectAnalysis.create(projectAnalysisContext)

@OptIn(ZpaExperimentalApi::class)
@JvmSynthetic
internal fun createNotPreparedProjectAnalysis(): ProjectAnalysis =
    ProjectAnalysis.notPrepared()
