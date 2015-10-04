package net.teamlixo.eggcrack.plugin.java.json;

import com.google.gson.Gson;
import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLoadException;
import net.teamlixo.eggcrack.plugin.java.JavaPluginConfiguration;
import net.teamlixo.eggcrack.plugin.java.JavaPluginLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

public class JsonPluginLoader extends JavaPluginLoader {
    @Override
    protected JavaPluginConfiguration getConfiguration(ClassLoader classLoader)
            throws FileNotFoundException, PluginLoadException {
        InputStream pluginConfigurationStream = classLoader.getResourceAsStream("plugin.json");
        if (pluginConfigurationStream == null)
            throw new FileNotFoundException("plugin.json");

        JavaPluginConfiguration configuration = (new Gson()).fromJson(
                new InputStreamReader(pluginConfigurationStream),
                JsonPluginConfiguration.class
        );

        //Check the discovered configuration for common mistakes.
        if (configuration == null)
            throw new PluginLoadException(new NullPointerException("Configuration is null"));

        if (configuration.getName() == null || configuration.getName().trim().length() <= 0)
            throw new PluginLoadException("Plugin name not provided.");

        if (configuration.getMainClass() == null || configuration.getMainClass().trim().length() <= 0)
            throw new PluginLoadException("Plugin entrypoint not provided.");

        if (configuration.getVersion() < 1)
            throw new PluginLoadException("Illegal plugin version provided.");

        return configuration;
    }

    @Override
    public Plugin loadPlugin(URL url) throws PluginLoadException {
        try {
            return loadPlugin(new URLClassLoader(new URL[] { url} ));
        } catch (ClassNotFoundException e) {
            throw new PluginLoadException(e);
        } catch (IllegalAccessException e) {
            throw new PluginLoadException(e);
        } catch (InstantiationException e) {
            throw new PluginLoadException(e);
        } catch (NoSuchMethodException e) {
            throw new PluginLoadException(e);
        } catch (InvocationTargetException e) {
            throw new PluginLoadException(e);
        } catch (FileNotFoundException e) {
            throw new PluginLoadException(e);
        }
    }
}
