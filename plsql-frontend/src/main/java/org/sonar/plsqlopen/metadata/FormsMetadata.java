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
package org.sonar.plsqlopen.metadata;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.annotation.CheckForNull;

import org.sonar.plsqlopen.utils.log.Logger;
import org.sonar.plsqlopen.utils.log.Loggers;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class FormsMetadata {

    private static final Logger LOG = Loggers.get(FormsMetadata.class);

    private String[] alerts = new String[0];
    private Block[] blocks = new Block[0];
    private String[] lovs = new String[0];

    public String[] getAlerts() {
        return alerts;
    }

    public void setAlerts(String[] alerts) {
        this.alerts = alerts;
    }
    
    public Block[] getBlocks() {
        return blocks;
    }

    public void setBlocks(Block[] blocks) {
        this.blocks = blocks;
    }
    
    public String[] getLovs() {
        return lovs;
    }

    public void setLovs(String[] lovs) {
        this.lovs = lovs;
    }

    @CheckForNull
    public static FormsMetadata loadFromFile(String path) {
        if (!Strings.isNullOrEmpty(path)) {
            try (JsonReader reader = new JsonReader(new FileReader(path))) {
                FormsMetadata formsMetadata = new Gson().fromJson(reader, FormsMetadata.class);
                LOG.info("Loaded Oracle Forms metadata from {}.", path);
                return formsMetadata;
            } catch (FileNotFoundException e) {
                LOG.warn("The metadata file {} was not found.", path, e);
            } catch (IOException e) {
                LOG.error("Error reading the metadata file at {}.", path, e);
            }
        }
        return null;
    }
    
}
