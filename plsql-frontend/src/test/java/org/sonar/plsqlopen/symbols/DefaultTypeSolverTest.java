/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2018 Felipe Zorzo
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType.Type;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class DefaultTypeSolverTest {

    private Parser<Grammar> p = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
    private DefaultTypeSolver typeSolver = new DefaultTypeSolver();

    @Before
    public void setUp() {
        p.setRootRule(p.getGrammar().rule(PlSqlGrammar.DATATYPE));
    }
    
    @Test
    public void identifyNumericType() {
        PlSqlType type = solveTypeFrom("number");
        assertThat(type.type()).isEqualTo(Type.NUMERIC);
        assertThat(type.isNumeric()).isTrue();
    }
    
    @Test
    public void identifyCharacterType() {
        PlSqlType type = solveTypeFrom("varchar2(100)");
        assertThat(type.type()).isEqualTo(Type.CHARACTER);
        assertThat(type.isCharacter()).isTrue();
    }
    
    @Test
    public void identifyDateType() {
        PlSqlType type = solveTypeFrom("date");
        assertThat(type.type()).isEqualTo(Type.DATE);
    }
    
    @Test
    public void identifyLobType() {
        PlSqlType type = solveTypeFrom("clob");
        assertThat(type.type()).isEqualTo(Type.LOB);
    }

    @Test
    public void identifyBooleanType() {
        PlSqlType type = solveTypeFrom("boolean");
        assertThat(type.type()).isEqualTo(Type.BOOLEAN);
    }

    @Test
    public void identifyRowtype() {
        PlSqlType type = solveTypeFrom("tab%rowtype");
        assertThat(type.type()).isEqualTo(Type.ROWTYPE);
    }
    
    @Test
    public void unknownType() {
        PlSqlType type = solveTypeFrom("tab.col%type");
        assertThat(type.type()).isEqualTo(Type.UNKNOWN);
        assertThat(type.isUnknown()).isTrue();
    }
    
    @Test
    public void ifNodeIsNullReturnsUnknownType() {
        PlSqlType type = typeSolver.solve(null);
        assertThat(type.type()).isEqualTo(Type.UNKNOWN);
        assertThat(type.isUnknown()).isTrue();
    }

    private PlSqlType solveTypeFrom(String code) {
        return typeSolver.solve(p.parse(code));
    }
    
}
