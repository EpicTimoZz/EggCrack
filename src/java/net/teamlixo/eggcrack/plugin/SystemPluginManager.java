package net.teamlixo.eggcrack.plugin;

import java.util.HashMap;
import java.util.Map;

public class SystemPluginManager implements PluginManager {
    private Map<String, Plugin> pluginList = new HashMap<>();
    private PluginLoader pluginLoader;

    public SystemPluginManager(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @Override
    public PluginLoader getLoader() {
        return pluginLoader;
    }

    @Override
    public void registerPlugin(Plugin plugin) {
        pluginList.put(plugin.getName(), plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
        pluginList.remove(plugin.getName());
    }
}
