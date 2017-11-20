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
