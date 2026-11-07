//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

/**
 * What the pre-pass leaves on the root of a tree: the local resolution, and the value categories walked over it (which
 * read the project snapshot, so the facts themselves stay detached from it). Readers holding only the tree reach them
 * here.
 */
internal class LocalFileAnalysis(val facts: LocalSemanticFacts, val categories: LocalValueCategories?)
