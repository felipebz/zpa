//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class OracleDatabaseVersionIT {
    @Test
    fun theAcquiredVersionIsWhatDbmsDbVersionDeclares() {
        OracleDatabase.connect().use { db ->
            val user = db.createUser("ZPAMD_VER")
            val expected = user.call("BEGIN ? := DBMS_DB_VERSION.VERSION || '.' || DBMS_DB_VERSION.RELEASE; END;")
            val result = OracleMetadataAcquisition.acquire(user.dataSource, OracleMetadataRequest.builder().schema(user.name).build())
            println("DBMS_DB_VERSION $expected, acquired ${result.databaseVersion}")
            assertThat(result.databaseVersionFailure).isNull()
            assertThat(result.databaseVersion!!.let { "${it.version}.${it.release}" }).isEqualTo(expected)
        }
    }
}
