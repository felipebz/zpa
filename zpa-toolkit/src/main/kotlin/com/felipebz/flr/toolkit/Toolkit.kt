//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.toolkit

import com.felipebz.flr.internal.toolkit.SourceCodeModel
import com.felipebz.flr.internal.toolkit.ToolkitPresenter
import com.felipebz.flr.internal.toolkit.ToolkitViewImpl
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import com.formdev.flatlaf.FlatIntelliJLaf
import javax.swing.SwingUtilities

/**
 * Creates a Toolkit with a title, and the given [ZpaConfigurationModel].
 *
 * @param title
 * @param configurationModel
 *
 * @since 1.17
 */
class Toolkit(private val title: String, private val configurationModel: ZpaConfigurationModel) {
    fun run() {
        SwingUtilities.invokeLater {
            FlatIntelliJLaf.setup()
            val model = SourceCodeModel(configurationModel)
            val presenter = ToolkitPresenter(configurationModel, model)
            presenter.setView(ToolkitViewImpl(presenter))
            presenter.run(title)
        }
    }
}
