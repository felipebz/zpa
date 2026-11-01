//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
