//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import java.util.*

/** Which value a position put more than once keeps. */
internal enum class LocalPositionDuplicates {
    /** The first value put, as `putIfAbsent` does. */
    FIRST,

    /** The last value put, as `associateBy` and `toMap` do. */
    LAST,

    /** None: a position put more than once has no value. */
    NONE
}

/**
 * A source position (one-based line, zero-based column) as one long; longs compare as the positions do.
 */
private fun positionKey(line: Int, column: Int): Long = (line.toLong() shl 32) or (column.toLong() and 0xFFFFFFFFL)

/**
 * Int values by source position, in two sorted arrays: a lookup is a binary search that allocates nothing. Built
 * once with [LocalPositionInts.Builder].
 */
internal class LocalPositionInts private constructor(private val keys: LongArray, private val values: IntArray) {
    /** The value at [line], [column], or -1 when there is none. */
    operator fun get(line: Int, column: Int): Int {
        val slot = Arrays.binarySearch(keys, positionKey(line, column))
        return if (slot >= 0) values[slot] else -1
    }

    /** Collects values (not negative) in any order. */
    class Builder {
        private var keys = LongArray(16)
        private var values = IntArray(16)
        private var size = 0

        fun put(line: Int, column: Int, value: Int) {
            require(value >= 0) { "A value cannot be negative" }
            if (size == keys.size) {
                keys = keys.copyOf(size * 2)
                values = values.copyOf(size * 2)
            }
            keys[size] = positionKey(line, column)
            values[size] = value
            size++
        }

        fun build(duplicates: LocalPositionDuplicates): LocalPositionInts {
            val kept = keptSlots(keys, size, duplicates)
            return LocalPositionInts(LongArray(kept.size) { keys[kept[it]] }, IntArray(kept.size) { values[kept[it]] })
        }
    }
}

/**
 * Values by source position, in two sorted arrays: a lookup is a binary search that allocates nothing. Built once
 * with [LocalPositionMap.Builder].
 */
internal class LocalPositionMap<T : Any> private constructor(private val keys: LongArray, private val values: Array<Any?>) {
    /** The value at [line], [column], or null when there is none. */
    operator fun get(line: Int, column: Int): T? {
        val slot = Arrays.binarySearch(keys, positionKey(line, column))
        @Suppress("UNCHECKED_CAST")
        return if (slot >= 0) values[slot] as T else null
    }

    /** Collects values in any order. */
    class Builder<T : Any> {
        private var keys = LongArray(16)
        private var values = arrayOfNulls<Any>(16)
        private var size = 0

        fun put(line: Int, column: Int, value: T) {
            if (size == keys.size) {
                keys = keys.copyOf(size * 2)
                values = values.copyOf(size * 2)
            }
            keys[size] = positionKey(line, column)
            values[size] = value
            size++
        }

        fun build(duplicates: LocalPositionDuplicates): LocalPositionMap<T> {
            val kept = keptSlots(keys, size, duplicates)
            return LocalPositionMap(LongArray(kept.size) { keys[kept[it]] }, Array(kept.size) { values[kept[it]] })
        }
    }
}

/** The index of each reference [include] keeps, by the start of its range; a start kept twice keeps the last one. */
internal inline fun referenceStarts(references: List<LocalReference>, include: (LocalReference) -> Boolean): LocalPositionInts {
    val builder = LocalPositionInts.Builder()
    for (index in references.indices) {
        val reference = references[index]
        if (include(reference)) builder.put(reference.range.startLine, reference.range.startColumn, index)
    }
    return builder.build(LocalPositionDuplicates.LAST)
}

/**
 * The index of each reference [include] keeps, by the start of each of its segments after the first; a start kept
 * twice keeps the last one. [segmentPosition] tells which segment of the reference starts there.
 */
internal inline fun segmentStarts(references: List<LocalReference>, include: (LocalReference) -> Boolean): LocalPositionInts {
    val builder = LocalPositionInts.Builder()
    for (index in references.indices) {
        val reference = references[index]
        if (!include(reference)) continue
        for (segment in reference.segments) builder.put(segment.range.startLine, segment.range.startColumn, index)
    }
    return builder.build(LocalPositionDuplicates.LAST)
}

/** The position in [reference] of its segment starting at [line], [column]: 1 for the second name, as it was found by [segmentStarts]. */
internal fun segmentPosition(reference: LocalReference, line: Int, column: Int): Int =
    reference.segments.indexOfFirst { it.range.startLine == line && it.range.startColumn == column } + 1

/** The index in [LocalSemanticModel.declarations] of each declaration, by the start of its name; a start kept twice keeps the last one. */
internal fun declarationStarts(model: LocalSemanticModel): LocalPositionInts {
    val builder = LocalPositionInts.Builder()
    for ((index, declaration) in model.declarations.withIndex()) {
        builder.put(declaration.nameRange.startLine, declaration.nameRange.startColumn, index)
    }
    return builder.build(LocalPositionDuplicates.LAST)
}

/**
 * The slots of the first [size] keys to keep, in key order: the slots are sorted by key, ties in the order they were
 * put, and of each run of equal keys [duplicates] keeps the first, the last or none.
 */
private fun keptSlots(keys: LongArray, size: Int, duplicates: LocalPositionDuplicates): IntArray {
    val order = IntArray(size) { it }
    mergeSort(order, IntArray(size), keys, 0, size)
    val kept = IntArray(size)
    var count = 0
    var start = 0
    while (start < size) {
        var end = start + 1
        while (end < size && keys[order[end]] == keys[order[start]]) end++
        when (duplicates) {
            LocalPositionDuplicates.FIRST -> kept[count++] = order[start]
            LocalPositionDuplicates.LAST -> kept[count++] = order[end - 1]
            LocalPositionDuplicates.NONE -> if (end - start == 1) kept[count++] = order[start]
        }
        start = end
    }
    return kept.copyOf(count)
}

/** Sorts `order[from, to)` by the keys of its slots, keeping the order of slots with equal keys. */
private fun mergeSort(order: IntArray, buffer: IntArray, keys: LongArray, from: Int, to: Int) {
    if (to - from < 2) return
    val middle = (from + to) ushr 1
    mergeSort(order, buffer, keys, from, middle)
    mergeSort(order, buffer, keys, middle, to)
    if (keys[order[middle - 1]] <= keys[order[middle]]) return
    var left = from
    var right = middle
    var out = from
    while (left < middle && right < to) {
        buffer[out++] = if (keys[order[right]] < keys[order[left]]) order[right++] else order[left++]
    }
    while (left < middle) buffer[out++] = order[left++]
    while (right < to) buffer[out++] = order[right++]
    System.arraycopy(buffer, from, order, from, to - from)
}
