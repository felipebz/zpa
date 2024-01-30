/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.RuleTest

class RollbackStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.ROLLBACK_STATEMENT)
    }

    @Test
    fun matchesSimpleRollback() {
        assertThat(p).matches("rollback;")
    }

    @Test
    fun matchesRollbackWork() {
        assertThat(p).matches("rollback work;")
    }

    @Test
    fun matchesRollbackForce() {
        assertThat(p).matches("rollback force 'test';")
    }

    @Test
    fun matchesRollbackToSavepoint() {
        assertThat(p).matches("rollback to save;")
    }

    @Test
    fun matchesRollbackToSavepointAlternative() {
        assertThat(p).matches("rollback to savepoint save;")
    }

    @Test
    fun matchesLongRollbackStatement() {
        assertThat(p).matches("rollback work to savepoint save;")
    }

    @Test
    fun matchesLabeledRollback() {
        assertThat(p).matches("<<foo>> rollback;")
    }

}
