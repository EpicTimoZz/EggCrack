package net.teamlixo.eggcrack.plugin.java;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLoadException;
import net.teamlixo.eggcrack.plugin.PluginLoader;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class JavaPluginLoader extends PluginLoader {
    protected abstract JavaPluginConfiguration getConfiguration(ClassLoader classLoader)
            throws FileNotFoundException, PluginLoadException;

    protected Plugin loadPlugin(ClassLoader classLoader)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException, FileNotFoundException, PluginLoadException {
        JavaPluginConfiguration pluginConfiguration = getConfiguration(classLoader);
        int version = EggCrack.getInstance().getVersion();
        if (pluginConfiguration.getVersionDependency() >= version)
            throw new InstantiationException("Plugin does not support current application version");

        Class mainClass = classLoader.loadClass(pluginConfiguration.getMainClass());
        Constructor constructor = mainClass.getConstructor(JavaPluginConfiguration.class);
        return (JavaPlugin) constructor.newInstance((Object) pluginConfiguration);
    }
}
