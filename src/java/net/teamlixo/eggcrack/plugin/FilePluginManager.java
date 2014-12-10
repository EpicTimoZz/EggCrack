package net.teamlixo.eggcrack.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class FilePluginManager implements PluginManager {
    private Map<String, PluginLoader> pluginLoaderMap = new HashMap<>();

    /**
     * Registers a plugin loader to a file extension, overwriting previous
     * file extensions recognized by this plugin manager.
     * @param extension File extension to register.
     * @param pluginLoader Plugin loader to register.
     */
    public void registerPluginLoader(String extension, PluginLoader pluginLoader) {
        this.pluginLoaderMap.put(extension.toLowerCase(), pluginLoader);
    }

    /**
     * Unregisters a plugin loader from this plugin manager.
     * @param extension Extension to unregister.
     * @return PluginLoader previously associated with the extension provided.
     */
    public PluginLoader unregisterPluginLoader(String extension) {
        return this.pluginLoaderMap.remove(extension.toLowerCase());
    }

    @Override
    public PluginLoader getLoader(File file) {
        String[] extensions = file.getName().split("[.]");
        if (extensions.length <= 1) return null;

        return pluginLoaderMap.get(extensions[extensions.length - 1].toLowerCase());
    }
}
