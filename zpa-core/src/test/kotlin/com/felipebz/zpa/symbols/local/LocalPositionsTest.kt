//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocalPositionsTest {

    /** Positions in no order, each put three times with the values 3p, 3p+1 and 3p+2, interleaved with the others. */
    private val positions = (0 until 500).map { (it * 7919) % 500 }.map { (1 + it / 40) to (it % 40) * 1000 }

    private fun ints(duplicates: LocalPositionDuplicates): LocalPositionInts {
        val builder = LocalPositionInts.Builder()
        for (round in 0 until 3) positions.forEachIndexed { p, (line, column) -> builder.put(line, column, 3 * p + round) }
        return builder.build(duplicates)
    }

    @Test
    fun aRepeatedPositionKeepsTheFirstTheLastOrNoValue() {
        val first = ints(LocalPositionDuplicates.FIRST)
        val last = ints(LocalPositionDuplicates.LAST)
        val none = ints(LocalPositionDuplicates.NONE)
        positions.forEachIndexed { p, (line, column) ->
            assertThat(first[line, column]).isEqualTo(3 * p)
            assertThat(last[line, column]).isEqualTo(3 * p + 2)
            assertThat(none[line, column]).isEqualTo(-1)
        }
    }

    @Test
    fun aPositionPutOnceIsFoundAndOthersAreNot() {
        val builder = LocalPositionMap.Builder<String>()
        positions.forEach { (line, column) -> builder.put(line, column, "$line:$column") }
        builder.put(Int.MAX_VALUE, Int.MAX_VALUE, "far")
        val map = builder.build(LocalPositionDuplicates.NONE)
        positions.forEach { (line, column) -> assertThat(map[line, column]).isEqualTo("$line:$column") }
        assertThat(map[Int.MAX_VALUE, Int.MAX_VALUE]).isEqualTo("far")
        // The same column on another line, a column between two others, and the column of one line on the next.
        assertThat(map[100, 0]).isNull()
        assertThat(map[1, 500]).isNull()
        assertThat(map[2, 39_000 + 1]).isNull()
        assertThat(LocalPositionInts.Builder().build(LocalPositionDuplicates.LAST)[1, 0]).isEqualTo(-1)
    }
}
