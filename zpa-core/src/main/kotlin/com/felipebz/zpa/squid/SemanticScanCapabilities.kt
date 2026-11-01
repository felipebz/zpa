//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectAnalysis

/** Capabilities installed for one walker execution before lifecycle callbacks begin. */
@OptIn(ZpaExperimentalApi::class)
internal class SemanticScanCapabilities(
    val projectAnalysis: ProjectAnalysis
)
