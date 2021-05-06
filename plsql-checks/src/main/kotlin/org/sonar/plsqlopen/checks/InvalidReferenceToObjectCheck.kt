/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.checks

import com.sonar.sslr.api.AstNode
import org.sonar.plsqlopen.FormsMetadataAwareCheck
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.annotations.*
import org.sonar.plugins.plsqlopen.api.matchers.MethodMatcher
import java.util.*

@Rule(key = InvalidReferenceToObjectCheck.CHECK_KEY, priority = Priority.MAJOR, tags = [Tags.BUG])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.MAIN)
@ActivatedByDefault
class InvalidReferenceToObjectCheck : AbstractBaseCheck(), FormsMetadataAwareCheck {

    private val verifiers = listOf(
            Verifier(MethodMatcher.create().name("find_alert").addParameter(), ObjectType.ALERT),
            Verifier(MethodMatcher.create().name("set_alert_button_property").addParameters(4), ObjectType.ALERT),
            Verifier(MethodMatcher.create().name("set_alert_property").addParameters(3), ObjectType.ALERT),
            Verifier(MethodMatcher.create().name("show_alert").addParameter(), ObjectType.ALERT),

            Verifier(MethodMatcher.create().name("find_lov").addParameter(), ObjectType.LOV),
            Verifier(MethodMatcher.create().name("get_lov_property").addParameters(2), ObjectType.LOV),
            Verifier(MethodMatcher.create().name("set_lov_column_property").addParameters(4), ObjectType.LOV),
            Verifier(MethodMatcher.create().name("set_lov_property").addParameters(3), ObjectType.LOV),
            Verifier(MethodMatcher.create().name("set_lov_property").addParameters(4), ObjectType.LOV),
            Verifier(MethodMatcher.create().name("show_lov").addParameter(), ObjectType.LOV),

            Verifier(MethodMatcher.create().name("find_block").addParameter(), ObjectType.BLOCK),
            Verifier(MethodMatcher.create().name("get_block_property").addParameters(2), ObjectType.BLOCK),
            Verifier(MethodMatcher.create().name("go_block").addParameter(), ObjectType.BLOCK),
            Verifier(MethodMatcher.create().name("set_block_property").addParameters(3), ObjectType.BLOCK),
            Verifier(MethodMatcher.create().name("set_block_property").addParameters(4), ObjectType.BLOCK),

            Verifier(MethodMatcher.create().name("checkbox_checked").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("convert_other_value").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("display_item").addParameters(2), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("find_item").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("get_item_instance_property").addParameters(3), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("get_item_property").addParameters(2), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("get_radio_button_property").addParameters(3), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("go_item").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("image_scroll").addParameters(3), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("image_zoom").addParameters(2), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("image_zoom").addParameters(3), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("play_sound").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("read_image_file").addParameters(3), 3, ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("read_sound_file").addParameters(3), 3, ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("recalculate").addParameter(), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("set_item_instance_property").addParameters(4), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("set_item_property").addParameters(3), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("set_item_property").addParameters(4), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("set_radio_button_property").addParameters(4), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("set_radio_button_property").addParameters(5), ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("write_image_file").addParameters(5), 3, ObjectType.ITEM),
            Verifier(MethodMatcher.create().name("write_sound_file").addParameters(5), 3, ObjectType.ITEM)
    )

    override fun init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL)
    }

    override fun visitNode(node: AstNode) {
        val verifier = verifiers.firstOrNull { v -> v.matcher.matches(node) }

        if (verifier != null) {
            val argument = verifier.matcher.getArgumentsValues(node)[verifier.argumentToCheck]
            if (!isVarcharLiteral(argument)) {
                return
            }

            val value = argument.tokenOriginalValue.replace("'", "")

            var reportIssue = false
            when (verifier.type) {
                ObjectType.ALERT -> reportIssue = validateAlert(value)
                ObjectType.BLOCK -> reportIssue = validateBlock(value)
                ObjectType.ITEM -> reportIssue = validateItem(value)
                ObjectType.LOV -> reportIssue = validateLov(value)
            }

            if (reportIssue) {
                addIssue(argument, getLocalizedMessage(CHECK_KEY), value,
                    verifier.matcher.methodName.uppercase(Locale.getDefault())
                )

            }

        }
    }

    private fun validateAlert(value: String): Boolean {
        return context.formsMetadata?.alerts?.none { alert -> alert.equals(value, ignoreCase = true) } ?: false
    }

    private fun validateBlock(value: String): Boolean {
        return context.formsMetadata?.blocks?.none { block -> block.name.equals(value, ignoreCase = true) } ?: false
    }

    private fun validateItem(value: String): Boolean {
        val formsMetadata = context.formsMetadata ?: return false

        var reportIssue = true
        for (block in formsMetadata.blocks) {
            if (block.items.any { item ->
                        val fullName = "${block.name}.$item"
                        fullName.equals(value, ignoreCase = true)
                    }) {
                reportIssue = false
            }
        }
        return reportIssue
    }

    private fun validateLov(value: String): Boolean {
        return context.formsMetadata?.lovs?.none { lov -> lov.equals(value, ignoreCase = true) } ?: false
    }

    private enum class ObjectType {
        ALERT, BLOCK, ITEM, LOV
    }

    private inner class Verifier internal constructor(internal val matcher: MethodMatcher, argumentToCheck: Int, val type: ObjectType) {
        internal val argumentToCheck: Int = argumentToCheck - 1

        internal constructor(matcher: MethodMatcher, type: ObjectType) : this(matcher, 1, type)
    }

    private fun isVarcharLiteral(argument: AstNode): Boolean {
        return if (argument.typeIs(PlSqlGrammar.LITERAL)) {
            argument.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL)
        } else false
    }

    companion object {
        internal const val CHECK_KEY = "InvalidReferenceToObject"
    }

}
