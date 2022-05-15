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
package com.felipebz.flr.toolkit

import com.felipebz.flr.internal.toolkit.SourceCodeModel
import com.felipebz.flr.internal.toolkit.ToolkitPresenter
import com.felipebz.flr.internal.toolkit.ToolkitViewImpl
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Creates a Toolkit with a title, and the given [ConfigurationModel].
 *
 * @param title
 * @param configurationModel
 *
 * @since 1.17
 */
public class Toolkit(private val title: String, private val configurationModel: ConfigurationModel) {
    public fun run() {
        SwingUtilities.invokeLater {
            try {
                for (info in UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus" == info.name) {
                        UIManager.setLookAndFeel(info.className)
                        break
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            val model = SourceCodeModel(configurationModel)
            val presenter = ToolkitPresenter(configurationModel, model)
            presenter.setView(ToolkitViewImpl(presenter))
            presenter.run(title)
        }
    }
}
