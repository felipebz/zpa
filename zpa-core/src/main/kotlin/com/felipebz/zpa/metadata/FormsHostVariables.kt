//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metadata

import java.util.*

/**
 * The PL/SQL type of a host variable of a form (`:block.item`, `:parameter.name`, `:global.name`, `:system.name`), as
 * Oracle Forms gives it to a reference, from what the [FormsMetadata] states. Null whenever that is not known.
 */
internal object FormsHostVariables {
    /**
     * The system variables Forms documents as character strings. The others have no documented type
     * (`SYSTEM.COORDINATION_OPERATION`, `SYSTEM.DATE_THRESHOLD`, `SYSTEM.TAB_NEW_PAGE`…), or another one:
     * `SYSTEM.TRIGGER_NODE` is a value of type NODE.
     */
    private val CHARACTER_SYSTEM_VARIABLES = setOf(
        "BLOCK_STATUS", "CURRENT_BLOCK", "CURRENT_DATETIME", "CURRENT_FORM", "CURRENT_ITEM", "CURRENT_VALUE",
        "CURSOR_BLOCK", "CURSOR_ITEM", "CURSOR_RECORD", "CURSOR_VALUE", "FORM_STATUS", "LAST_EVENT", "LAST_FORM", "LAST_QUERY",
        "LAST_RECORD", "MESSAGE_LEVEL", "MODE", "MOUSE_BUTTON_MODIFIERS", "MOUSE_BUTTON_PRESSED", "MOUSE_BUTTON_SHIFT_STATE",
        "MOUSE_CANVAS", "MOUSE_FORM", "MOUSE_ITEM", "MOUSE_RECORD", "MOUSE_RECORD_OFFSET", "MOUSE_X_POS", "MOUSE_Y_POS",
        "NOTIFICATION_VALUE", "RECORD_STATUS", "SUPPRESS_WORKING", "TRIGGER_BLOCK", "TRIGGER_ITEM", "TRIGGER_MENUOPTION",
        "TRIGGER_RECORD"
    )

    /**
     * The type of `:qualifier.name`: an item of a block or a parameter of the form by its Forms data type; a global is a
     * character string, as is each system variable Forms documents as one.
     */
    fun type(metadata: FormsMetadata, qualifier: String, name: String): String? = when (qualifier.uppercase(Locale.ROOT)) {
        "GLOBAL" -> CHARACTER
        "SYSTEM" -> CHARACTER.takeIf { name.uppercase(Locale.ROOT) in CHARACTER_SYSTEM_VARIABLES }
        "PARAMETER" -> metadata.parameters.singleOrNull { it.name.equals(name, ignoreCase = true) }?.dataType?.let(::of)
        else -> metadata.blocks.singleOrNull { it.name.equals(qualifier, ignoreCase = true) }
            ?.itemDefinitions?.singleOrNull { it.name.equals(name, ignoreCase = true) }?.dataType?.let(::of)
    }

    /**
     * The PL/SQL type of a value of the Forms data type [dataType]: CHAR supports VARCHAR2, and LONG holds variable-length
     * character strings; NCHAR corresponds to NCHAR and NVARCHAR2; NUMBER is processed as a true number; DATE and
     * DATETIME as true dates. The legacy types (ALPHA, INT, MONEY, RINT, RMONEY, RNUMBER, EDATE, JDATE, TIME) are
     * documented by their display and storage only, and an Object REF by no PL/SQL type: null.
     */
    fun of(dataType: String): String? = when (dataType.trim().uppercase(Locale.ROOT)) {
        "CHAR", "LONG" -> CHARACTER
        "NCHAR" -> "NVARCHAR2"
        "NUMBER" -> "NUMBER"
        "DATE", "DATETIME" -> "DATE"
        else -> null
    }

    private const val CHARACTER = "VARCHAR2"
}
