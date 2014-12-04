package net.teamlixo.eggcrack.plugin.java;

import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLoader;

public abstract class JavaPluginLoader extends PluginLoader {
    protected abstract JavaPluginConfiguration getConfiguration(ClassLoader classLoader);

    protected Plugin loadPlugin(ClassLoader classLoader)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        JavaPluginConfiguration pluginConfiguration = getConfiguration(classLoader);
        Class mainClass = classLoader.loadClass(pluginConfiguration.getMainClass());
        JavaPlugin plugin = (JavaPlugin) mainClass.newInstance();
        plugin.setPluginConfiguration(pluginConfiguration);
        return plugin;
    }
}
