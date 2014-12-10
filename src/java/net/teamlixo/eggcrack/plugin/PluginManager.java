package net.teamlixo.eggcrack.plugin;

import java.io.File;
import java.util.List;

public interface PluginManager {

    /**
     * Gets a plugin loader for the file specified.
     */
    public PluginLoader getLoader(File file);

    /**
     * Registers a plugin into the manager.
     * @param plugin Plugin to register.
     */
    public void registerPlugin(Plugin plugin);

    /**
     * Unregisters a plugin from the manager.
     * @param plugin Plugin to unregister.
     */
    public void unregisterPlugin(Plugin plugin);

    /**
     * Gets a plugin by its name.
     * @param name Plugin name.
     * @return Plugin.
     */
    public Plugin getPlugin(String name);

    /**
     * Lists all loaded plugins.
     * @return Plugins loaded.
     */
    public List<Plugin> listPlugins();

}
