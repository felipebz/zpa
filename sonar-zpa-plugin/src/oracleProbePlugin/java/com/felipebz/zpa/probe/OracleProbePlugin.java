package com.felipebz.zpa.probe;

import org.sonar.api.Plugin;

/** Test-only plugin registering {@link ExternalTypeProbeCheck}. */
public class OracleProbePlugin implements Plugin {
    @Override
    public void define(Plugin.Context context) {
        context.addExtension(OracleProbeRulesDefinition.class);
    }
}
