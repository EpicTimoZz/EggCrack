package net.teamlixo.eggcrack.plugin.java;

import net.teamlixo.eggcrack.plugin.Plugin;

public abstract class JavaPlugin extends Plugin {
    private JavaPluginConfiguration pluginConfiguration;

    protected void setPluginConfiguration(JavaPluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    @Override
    public final String getName() {
        return pluginConfiguration.getName();
    }

    @Override
    public final String getVersion() {
        return pluginConfiguration.getVersion();
    }
}
