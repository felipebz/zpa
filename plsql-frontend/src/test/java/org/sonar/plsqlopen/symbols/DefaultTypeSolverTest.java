/*
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
package org.sonar.plsqlopen.symbols;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class DefaultTypeSolverTest {

    private Parser<Grammar> p = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
    private DefaultTypeSolver typeSolver = new DefaultTypeSolver();
    
    @Test
    public void identifyNumericType() {
        PlSqlType type = solveTypeFromDatatype("number");
        assertThat(type).isEqualTo(PlSqlType.NUMERIC);
        assertThat(type.isNumeric()).isTrue();
    }

    @Test
    public void identifyTypeNotNull() {
        PlSqlType type = solveTypeFromDatatype("number not null");
        assertThat(type).isEqualTo(PlSqlType.NUMERIC);
        assertThat(type.isNumeric()).isTrue();
    }
    
    @Test
    public void identifyCharacterType() {
        PlSqlType type = solveTypeFromDatatype("varchar2(100)");
        assertThat(type).isEqualTo(PlSqlType.CHARACTER);
        assertThat(type.isCharacter()).isTrue();
    }
    
    @Test
    public void identifyDateType() {
        PlSqlType type = solveTypeFromDatatype("date");
        assertThat(type).isEqualTo(PlSqlType.DATE);
    }
    
    @Test
    public void identifyLobType() {
        PlSqlType type = solveTypeFromDatatype("clob");
        assertThat(type).isEqualTo(PlSqlType.LOB);
    }

    @Test
    public void identifyBooleanType() {
        PlSqlType type = solveTypeFromDatatype("boolean");
        assertThat(type).isEqualTo(PlSqlType.BOOLEAN);
    }

    @Test
    public void identifyRowtype() {
        PlSqlType type = solveTypeFromDatatype("tab%rowtype");
        assertThat(type).isEqualTo(PlSqlType.ROWTYPE);
    }

    @Test
    public void identifyRowtypeNotNull() {
        PlSqlType type = solveTypeFromDatatype("tab%rowtype not null");
        assertThat(type).isEqualTo(PlSqlType.ROWTYPE);
    }
    
    @Test
    public void unknownType() {
        PlSqlType type = solveTypeFromDatatype("tab.col%type");
        assertThat(type).isEqualTo(PlSqlType.UNKNOWN);
        assertThat(type.isUnknown()).isTrue();
    }

    @Test
    public void unknownTypeNotNull() {
        PlSqlType type = solveTypeFromDatatype("tab.col%type not null");
        assertThat(type).isEqualTo(PlSqlType.UNKNOWN);
        assertThat(type.isUnknown()).isTrue();
    }
    
    @Test
    public void ifNodeIsNullReturnsUnknownType() {
        PlSqlType type = typeSolver.solve(null);
        assertThat(type).isEqualTo(PlSqlType.UNKNOWN);
        assertThat(type.isUnknown()).isTrue();
    }

    @Test
    public void identifyNumericLiteral() {
        PlSqlType type = solveTypeFromLiteral("1");
        assertThat(type).isEqualTo(PlSqlType.NUMERIC);
        assertThat(type.isNumeric()).isTrue();
    }

    @Test
    public void identifyCharacterLiteral() {
        PlSqlType type = solveTypeFromLiteral("'foo'");
        assertThat(type).isEqualTo(PlSqlType.CHARACTER);
        assertThat(type.isCharacter()).isTrue();
    }

    @Test
    public void identifyDateLiteral() {
        PlSqlType type = solveTypeFromLiteral("date '2000-01-01'");
        assertThat(type).isEqualTo(PlSqlType.DATE);
    }

    @Test
    public void identifyBooleanLiteral() {
        PlSqlType type = solveTypeFromLiteral("true");
        assertThat(type).isEqualTo(PlSqlType.BOOLEAN);
    }

    private PlSqlType solveTypeFromDatatype(String code) {
        p.setRootRule(p.getGrammar().rule(PlSqlGrammar.DATATYPE));
        return typeSolver.solve(p.parse(code));
    }

    private PlSqlType solveTypeFromLiteral(String code) {
        p.setRootRule(p.getGrammar().rule(PlSqlGrammar.LITERAL));
        return typeSolver.solve(p.parse(code));
    }
    
}
