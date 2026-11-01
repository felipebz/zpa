//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.sql.SQLException

/** The dictionary returned metadata that cannot form a consistent snapshot; no result is produced. */
@ZpaExperimentalApi
class OracleMetadataException internal constructor(message: String, cause: Throwable?) : SQLException(message, cause)
