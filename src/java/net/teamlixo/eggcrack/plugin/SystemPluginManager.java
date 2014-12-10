package net.teamlixo.eggcrack.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemPluginManager extends FilePluginManager {
    private Map<String, Plugin> pluginList = new HashMap<>();

    @Override
    public void registerPlugin(Plugin plugin) {
        pluginList.put(plugin.getName(), plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
        pluginList.remove(plugin.getName());
    }

    @Override
    public Plugin getPlugin(String name) {
        return pluginList.get(name);
    }

    @Override
    public List<Plugin> listPlugins() {
        return new ArrayList<>(pluginList.values());
    }
}
