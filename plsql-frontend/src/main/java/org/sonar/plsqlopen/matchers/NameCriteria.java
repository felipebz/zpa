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
package org.sonar.plsqlopen.matchers;

import java.util.Arrays;

@FunctionalInterface
public interface NameCriteria {

    boolean matches(String name);

    static NameCriteria any() {
        return name -> true;
    }

    static NameCriteria is(String exactName) {
    	return exactName::equalsIgnoreCase;
    }

    static NameCriteria startsWith(String prefix) {
        return name -> name.toUpperCase().startsWith(prefix.toUpperCase());
    }
    
    static NameCriteria in(String... prefix) {
        return name -> Arrays.stream(prefix).anyMatch(name::equalsIgnoreCase);
    }
    
}
