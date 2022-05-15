/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2010-2021 SonarSource SA
 * Copyright (C) 2021-2021 Felipe Zorzo
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
package com.felipebz.flr.internal.toolkit

import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.Serializable
import javax.swing.*

internal class ConfigurationPropertyPanel(name: String, description: String) : Serializable {
    val panel: JPanel = JPanel(GridBagLayout())
    val valueTextField: JTextField
    val errorMessageLabel: JLabel

    init {
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 0
        constraints.anchor = GridBagConstraints.NORTH
        constraints.insets = Insets(3, 10, 3, 10)
        panel.border = BorderFactory.createTitledBorder(name)
        val descriptionLabel = JLabel(description)
        panel.add(descriptionLabel, constraints)
        valueTextField = JTextField()
        panel.add(valueTextField, constraints)
        errorMessageLabel = JLabel()
        errorMessageLabel.foreground = Color.RED
        panel.add(errorMessageLabel, constraints)
        val constraints2 = GridBagConstraints()
        constraints2.gridx = 0
        constraints2.weighty = 1.0
        panel.add(Box.createGlue(), constraints2)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
