/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.rules

import com.felipebz.zpa.CustomAnnotationBasedRulesDefinition.Companion.getRuleKey
import com.felipebz.zpa.utils.getFields
import com.felipebz.zpa.api.checks.PlSqlVisitor
import java.lang.reflect.Field
import java.util.*

open class ZpaChecks constructor(private val activeRules: ZpaActiveRules,
                                 private val repository: String,
                                 private val ruleMetadataLoader: RuleMetadataLoader) {
    private val checkByRule = HashMap<ZpaRuleKey, PlSqlVisitor>()
    private val ruleByCheck = IdentityHashMap<PlSqlVisitor, ZpaRuleKey>()

    fun of(ruleKey: ZpaRuleKey): PlSqlVisitor? {
        return checkByRule[ruleKey]
    }

    fun all(): Collection<PlSqlVisitor> {
        return checkByRule.values
    }

    fun ruleKey(check: PlSqlVisitor): ZpaRuleKey? {
        return ruleByCheck[check]
    }

    private fun add(ruleKey: ZpaRuleKey, obj: PlSqlVisitor) {
        checkByRule[ruleKey] = obj
        ruleByCheck[obj] = ruleKey
    }

    fun addAnnotatedChecks(checkClassesOrObjects: Iterable<Class<*>>): ZpaChecks {
        val checksByEngineKey = HashMap<String, Class<*>>()
        for (checkClassesOrObject in checkClassesOrObjects) {
            val engineKey = getRuleKey(ruleMetadataLoader, checkClassesOrObject)
            checksByEngineKey[engineKey] = checkClassesOrObject
        }

        for (activeRule in activeRules.findByRepository(repository)) {
            val engineKey = if (activeRule.templateRuleKey.isNullOrBlank())
                activeRule.ruleKey.rule
            else
                activeRule.templateRuleKey

            val checkClassesOrObject = checksByEngineKey[engineKey]
            if (checkClassesOrObject != null) {
                val obj = instantiate(activeRule, checkClassesOrObject)
                obj.activeRule = activeRule
                add(activeRule.ruleKey, obj)
            }
        }
        return this
    }

    private fun instantiate(activeRule: ZpaActiveRule, checkClassOrInstance: Any): PlSqlVisitor {
        try {
            var check = checkClassOrInstance
            if (check is Class<*>) {
                check = (checkClassOrInstance as Class<*>).getDeclaredConstructor().newInstance()
            }
            configureFields(activeRule, check)
            return check as PlSqlVisitor
        } catch (e: InstantiationException) {
            throw failToInstantiateCheck(activeRule, checkClassOrInstance, e)
        } catch (e: IllegalAccessException) {
            throw failToInstantiateCheck(activeRule, checkClassOrInstance, e)
        }

    }

    private fun failToInstantiateCheck(activeRule: ZpaActiveRule, checkClassOrInstance: Any, e: Exception): RuntimeException {
        throw IllegalStateException(String.format("Fail to instantiate class %s for rules %s", checkClassOrInstance, activeRule.ruleKey), e)
    }

    private fun configureFields(activeRule: ZpaActiveRule, check: Any) {
        for ((key, value) in activeRule.params) {
            val field = getField(check, key) ?: throw IllegalStateException(
                String.format("The field '%s' does not exist or is not annotated with @RuleProperty in the class %s", key, check.javaClass.name))
            if (value.isNotBlank()) {
                configureField(check, field, value)
            }
        }
    }

    private fun getField(check: Any, key: String): Field? {
        val fields = getFields(check.javaClass, true)
        for (field in fields) {
            val propertyAnnotation = ruleMetadataLoader.getRulePropertyAnnotation(field)
            if (propertyAnnotation != null && ((key == field.name) || (key == propertyAnnotation.key))) {
                return field
            }
        }
        return null
    }

    private fun configureField(check: Any, field: Field, value: String) {
        try {
            field.isAccessible = true

            when(field.type) {
                String::class.java -> field.set(check, value)
                Int::class.javaPrimitiveType -> field.setInt(check, Integer.parseInt(value))
                Short::class.javaPrimitiveType -> field.setShort(check, java.lang.Short.parseShort(value))
                Long::class.javaPrimitiveType -> field.setLong(check, java.lang.Long.parseLong(value))
                Double::class.javaPrimitiveType -> field.setDouble(check, java.lang.Double.parseDouble(value))
                Boolean::class.javaPrimitiveType -> field.setBoolean(check, java.lang.Boolean.parseBoolean(value))
                Byte::class.javaPrimitiveType -> field.setByte(check, java.lang.Byte.parseByte(value))
                Int::class.java -> field.set(check, Integer.parseInt(value))
                Long::class.java -> field.set(check, java.lang.Long.parseLong(value))
                Double::class.java -> field.set(check, java.lang.Double.parseDouble(value))
                Boolean::class.java -> field.set(check, java.lang.Boolean.parseBoolean(value))
                else -> throw IllegalStateException("The type of the field " + field + " is not supported: " + field.type)
            }
        } catch (e: IllegalAccessException) {
            throw IllegalStateException("Can not set the value of the field " + field + " in the class: " + check.javaClass.name, e)
        }

    }
}
