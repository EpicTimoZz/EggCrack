package net.teamlixo.eggcrack.plugin.java.json;

import com.google.gson.Gson;
import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLoadException;
import net.teamlixo.eggcrack.plugin.java.JavaPluginConfiguration;
import net.teamlixo.eggcrack.plugin.java.JavaPluginLoader;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;

public class JsonPluginLoader extends JavaPluginLoader {
    @Override
    protected JavaPluginConfiguration getConfiguration(ClassLoader classLoader) {
        return (new Gson()).fromJson(
                new InputStreamReader(classLoader.getResourceAsStream("plugin.json")),
                JsonPluginConfiguration.class
        );
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
        }
    }
}
