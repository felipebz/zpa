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
package org.sonar.plsqlopen.util.log;

import org.jetbrains.annotations.NotNull;
import org.sonar.plsqlopen.utils.log.Logger;

public class SonarQubeLogger implements Logger {
    private org.sonar.api.utils.log.Logger logger;

    SonarQubeLogger(org.sonar.api.utils.log.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(@NotNull String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(@NotNull String msg, Object... args) {
        logger.trace(msg, args);
    }

    @Override
    public void info(@NotNull String msg) {
        logger.info(msg);
    }

    @Override
    public void info(@NotNull String msg, Object... args) {
        logger.info(msg, args);
    }

    @Override
    public void warn(@NotNull String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(@NotNull String msg, Object... args) {
        logger.warn(msg, args);
    }

    @Override
    public void error(@NotNull String msg) {
        logger.error(msg);
    }

    @Override
    public void error(@NotNull String msg, Object... args) {
        logger.error(msg, args);
    }
}
