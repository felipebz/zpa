/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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
package org.sonar.plugins.plsqlopen.api.ddl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateSynonymTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.CREATE_SYNONYM);
    }
    
    @Test
    public void matchesSimpleCreateSynonym() {
        assertThat(p).matches("create synonym foo for bar;");
    }
    
    @Test
    public void matchesCreatePublicSynonym() {
        assertThat(p).matches("create public synonym foo for bar;");
    }

    @Test
    public void matchesEditionableSynonym() {
        assertThat(p).matches("create editionable synonym foo for bar;");
    }

    @Test
    public void matchesNonEditionableSynonym() {
        assertThat(p).matches("create noneditionable synonym foo for bar;");
    }

    @Test
    public void matchesSynonymWithSharingMetada() {
        assertThat(p).matches("create synonym foo sharing = metadata for bar;");
    }

    @Test
    public void matchesSynonymWithSharingNone() {
        assertThat(p).matches("create synonym foo sharing = none for bar;");
    }
    
    @Test
    public void matchesCreateorReplaceSynonym() {
        assertThat(p).matches("create or replace synonym foo for bar;");
    }
    
    @Test
    public void matchesLongSynonym() {
        assertThat(p).matches("create or replace public synonym sch.foo for sch.bar@link;");
    }

}
