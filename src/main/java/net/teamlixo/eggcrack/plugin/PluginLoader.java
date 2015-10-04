package net.teamlixo.eggcrack.plugin;

import java.net.URL;

public abstract class PluginLoader {

    /**
     * Loads a plugin from the URL given.
     * @param url URL of the plugin to provide.
     * @return Provided plugin.
     * @throws PluginLoadException
     */
    public abstract Plugin loadPlugin(URL url) throws PluginLoadException;

}
