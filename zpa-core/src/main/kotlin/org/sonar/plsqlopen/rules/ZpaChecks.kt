/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plsqlopen.rules

import org.sonar.plsqlopen.utils.getAnnotation
import org.sonar.plsqlopen.utils.getFields
import org.sonar.plugins.plsqlopen.api.annotations.Rule
import org.sonar.plugins.plsqlopen.api.annotations.RuleProperty
import java.lang.reflect.Field
import java.util.*

open class ZpaChecks<C> constructor(private val activeRules: ZpaActiveRules, private val repository: String) {
    private val checkByRule = HashMap<ZpaRuleKey, C>()
    private val ruleByCheck = IdentityHashMap<C, ZpaRuleKey>()

    fun of(ruleKey: ZpaRuleKey): C? {
        return checkByRule[ruleKey]
    }

    fun all(): Collection<C> {
        return checkByRule.values
    }

    fun ruleKey(check: C): ZpaRuleKey? {
        return ruleByCheck[check]
    }

    open fun getRuleAnnotation(annotatedClassOrObject: Any) : RuleData? =
        RuleData.from(getAnnotation(annotatedClassOrObject, Rule::class.java))

    open fun getRulePropertyAnnotation(field: Field) : RulePropertyData? =
        RulePropertyData.from(field.getAnnotation(RuleProperty::class.java))

    private fun add(ruleKey: ZpaRuleKey, obj: C) {
        checkByRule[ruleKey] = obj
        ruleByCheck[obj] = ruleKey
    }

    fun addAnnotatedChecks(checkClassesOrObjects: Iterable<Any>): ZpaChecks<C> {
        val checksByEngineKey = HashMap<String, Any>()
        for (checkClassesOrObject in checkClassesOrObjects) {
            val engineKey = annotatedEngineKey(checkClassesOrObject)
            if (engineKey != null) {
                checksByEngineKey[engineKey] = checkClassesOrObject
            }
        }

        for (activeRule in activeRules.findByRepository(repository)) {
            val engineKey = if (activeRule.templateRuleKey().isNullOrBlank())
                activeRule.ruleKey().rule()
            else
                activeRule.templateRuleKey()

            val checkClassesOrObject = checksByEngineKey[engineKey]
            if (checkClassesOrObject != null) {
                val obj = instantiate(activeRule, checkClassesOrObject)
                add(activeRule.ruleKey(), obj)
            }
        }
        return this
    }

    private fun annotatedEngineKey(annotatedClassOrObject: Any): String? {
        var key: String? = null
        val ruleAnnotation = getRuleAnnotation(annotatedClassOrObject)
        if (ruleAnnotation != null) {
            key = ruleAnnotation.key
        }
        var clazz: Class<*> = annotatedClassOrObject.javaClass
        if (annotatedClassOrObject is Class<*>) {
            clazz = annotatedClassOrObject
        }
        return if (key.isNullOrBlank()) clazz.canonicalName else key
    }

    private fun instantiate(activeRule: ZpaActiveRule, checkClassOrInstance: Any): C {
        try {
            var check = checkClassOrInstance
            if (check is Class<*>) {
                check = (checkClassOrInstance as Class<*>).getDeclaredConstructor().newInstance()
            }
            configureFields(activeRule, check)
            return check as C
        } catch (e: InstantiationException) {
            throw failToInstantiateCheck(activeRule, checkClassOrInstance, e)
        } catch (e: IllegalAccessException) {
            throw failToInstantiateCheck(activeRule, checkClassOrInstance, e)
        }

    }

    private fun failToInstantiateCheck(activeRule: ZpaActiveRule, checkClassOrInstance: Any, e: Exception): RuntimeException {
        throw IllegalStateException(String.format("Fail to instantiate class %s for rules %s", checkClassOrInstance, activeRule.ruleKey()), e)
    }

    private fun configureFields(activeRule: ZpaActiveRule, check: Any) {
        for ((key, value) in activeRule.params()) {
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
            val propertyAnnotation = getRulePropertyAnnotation(field)
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
