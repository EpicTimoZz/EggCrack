package net.teamlixo.eggcrack.plugin;

public interface PluginManager {

    /**
     * Gets a plugin loader.
     */
    public PluginLoader getLoader();

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

}
