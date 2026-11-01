//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.flr.grammar.ContextKey

/**
 * Marks the explicit MODEL expression scopes owned by the ZPA grammar.
 */
internal val MODEL_EXPRESSION_CONTEXT: ContextKey<Boolean> = ContextKey()

/**
 * Set inside the static RETURNING list of INSERT, UPDATE and DELETE, where OLD and NEW may qualify an operand.
 */
internal val RETURNING_VALUE_CONTEXT: ContextKey<Boolean> = ContextKey()

/**
 * Set inside CONTAINER_MAP UPDATE (...), where SPLIT PARTITION takes no nested table part and no PARALLEL clause
 * before the index maintenance clause.
 */
internal val CONTAINER_MAP_CONTEXT: ContextKey<Boolean> = ContextKey()

/**
 * Set inside the SQL definition of a JSON relational duality view, where table and field annotations are legal.
 */
internal val DUALITY_VIEW_CONTEXT: ContextKey<Boolean> = ContextKey()
