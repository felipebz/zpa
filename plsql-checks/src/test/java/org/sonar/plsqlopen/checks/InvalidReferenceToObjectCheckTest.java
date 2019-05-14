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
package org.sonar.plsqlopen.checks;

import java.util.Collections;

import org.junit.Test;
import org.sonar.plsqlopen.checks.verifier.PlSqlCheckVerifier;
import org.sonar.plsqlopen.metadata.Block;
import org.sonar.plsqlopen.metadata.FormsMetadata;

public class InvalidReferenceToObjectCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        FormsMetadata metadata = new FormsMetadata();
        metadata.setAlerts(Collections.singletonList("foo"));
        metadata.setBlocks(Collections.singletonList(new Block("foo", new String[] {"item1"})));
        metadata.setLovs(Collections.singletonList("foo"));
        PlSqlCheckVerifier.verify(getPath("invalid_reference_to_object.sql"), new InvalidReferenceToObjectCheck(), metadata);
    }

}
