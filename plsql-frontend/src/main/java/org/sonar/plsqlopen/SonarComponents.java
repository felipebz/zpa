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
package org.sonar.plsqlopen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.metadata.FormsMetadata;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

@BatchSide
public class SonarComponents {

    private static final Logger LOG = Loggers.get(SonarComponents.class);

    private final SensorContext context;
    private FormsMetadata formsMetadata;
    
    public SonarComponents(SensorContext context) {
        this.context = context;
    }
    
    public SensorContext getContext() {
        return context;
    }
    
    public FormsMetadata getFormsMetadata() {
        return this.formsMetadata;
    }
    
    @VisibleForTesting
    public void setFormsMetadata(FormsMetadata metadata) {
        this.formsMetadata = metadata;
    }

    public void loadMetadataFile(String metadataFile) {
        if (Strings.isNullOrEmpty(metadataFile)) {
            return;
        }
        
        try (JsonReader reader = new JsonReader(new FileReader(metadataFile))) {
            this.formsMetadata = new Gson().fromJson(reader, FormsMetadata.class);
        } catch (FileNotFoundException e) {
            LOG.warn("The metadata file {} was not found.", metadataFile);
        } catch (IOException e) {
            LOG.error("Error reading the metadata file at {}.", metadataFile, e);
        }
    }
}
