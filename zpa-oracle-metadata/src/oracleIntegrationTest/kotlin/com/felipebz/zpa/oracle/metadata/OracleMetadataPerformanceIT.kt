//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle.metadata

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/** Timings and retained memory, printed only; enabled with `ZPA_ORACLE_MEASURE=true`. Not a correctness test. */
@OptIn(ZpaExperimentalApi::class)
class OracleMetadataPerformanceIT {
    @Test
    fun measure() {
        assumeTrue(System.getenv("ZPA_ORACLE_MEASURE") == "true", "ZPA_ORACLE_MEASURE not set")
        OracleDatabase.connect().use { db ->
            val bulk = db.createUser("ZPAMD_BULK", "CREATE TABLE", "CREATE VIEW")
            bulk.execute(
                """
                DECLARE cols VARCHAR2(4000);
                BEGIN
                  FOR c IN 1 .. 20 LOOP cols := cols || CASE WHEN c > 1 THEN ', ' END || 'c' || c || ' VARCHAR2(30)'; END LOOP;
                  FOR t IN 1 .. 1000 LOOP EXECUTE IMMEDIATE 'CREATE TABLE t' || t || ' (' || cols || ')'; END LOOP;
                  FOR v IN 1 .. 200 LOOP EXECUTE IMMEDIATE 'CREATE VIEW v' || v || ' AS SELECT * FROM t' || v; END LOOP;
                END;
                """.trimIndent()
            )
            fun time(label: String, request: OracleMetadataRequest): OracleMetadataResult {
                OracleMetadataAcquisition.acquire(bulk.dataSource, request)
                val runs = (1..3).map {
                    val start = System.nanoTime()
                    val result = OracleMetadataAcquisition.acquire(bulk.dataSource, request)
                    (System.nanoTime() - start) / 1_000_000 to result
                }
                println("MEASURE $label: ${runs.map { it.first }} ms, ${runs.last().second.relationCount} relations, ${runs.last().second.columnCount} columns")
                return runs.last().second
            }
            time("one relation", OracleMetadataRequest.builder().relation(bulk.name, "T1").build())
            val fifty = OracleMetadataRequest.builder().apply { (1..50).forEach { relation(bulk.name, "T$it") } }.build()
            time("50 targeted relations", fifty)
            val schema = OracleMetadataRequest.builder().schema(bulk.name).build()
            time("whole schema", schema)
            val runtime = Runtime.getRuntime()
            System.gc()
            val before = runtime.totalMemory() - runtime.freeMemory()
            val retained = OracleMetadataAcquisition.acquire(bulk.dataSource, schema)
            System.gc()
            val after = runtime.totalMemory() - runtime.freeMemory()
            println("MEASURE whole schema retained ≈ ${(after - before) / 1024} KiB (approximate)")
            assertThat(retained.relationCount).isEqualTo(1200)
        }
    }
}
