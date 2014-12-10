package net.teamlixo.eggcrack;

import com.google.gson.Gson;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.config.Configuration;
import net.teamlixo.eggcrack.config.JsonConfiguration;
import net.teamlixo.eggcrack.plugin.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Logger;

public final class EggCrack {
    public static Logger LOGGER;

    private static EggCrack instance;
    public static final void setInstance(EggCrack newInstance) {
        instance = newInstance;
    }
    public static final EggCrack getInstance() {
        return instance;
    }

    private Map<String, AuthenticationService> authenticationServiceMap = new HashMap<>();
    private PluginManager pluginManager;
    private final int version;

    public EggCrack(PluginManager pluginManager, int version) {
        this.pluginManager = pluginManager;
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void registerAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationServiceMap.put(authenticationService.getName(), authenticationService);
    }

    public void unregisterAuthenticationService(String protocol) {
        this.authenticationServiceMap.remove(protocol);
    }

    public Collection<AuthenticationService> listAuthenticationServices() {
        return new ArrayList<>(authenticationServiceMap.values());
    }

    public List<Plugin> loadPlugins(File directory) throws UnsupportedOperationException {
        List<Plugin> plugins = new ArrayList<>();

        if (!directory.exists()) {
            EggCrack.LOGGER.fine("Creating plugin directory...");
            directory.mkdir(); //Make the directory.
        } else if (!directory.isDirectory())
            throw new UnsupportedOperationException("Plugin path exists but is not a directory.");

        if (!directory.canRead() || !directory.canExecute())
            throw new UnsupportedOperationException("Plugin path exists but cannot be read (access permissions).");

        File[] files = directory.listFiles();
        EggCrack.LOGGER.fine(files.length + " files/directories found in " + directory.getAbsolutePath() + "...");

        for (File pluginFile : files) {
            if (!pluginFile.exists() || pluginFile.isDirectory() || !pluginFile.canRead()) continue; //Permissions, etc.
            PluginLoader pluginLoader = pluginManager.getLoader(pluginFile); //Find a loader for this plugin.
            if (pluginLoader == null) continue; //No plugin loader available for this file (no extension?)
            EggCrack.LOGGER.finest("Loading plugin from file " + pluginFile.getName() + " using loader "
                    + pluginLoader.getClass().getName() + "...");

            try {
                Plugin plugin = pluginLoader.loadPlugin(pluginFile.toURI().toURL()); //Load the plugin.
                plugin.setEnabled(true); //Enable the plugin. Hopefully the plugin developers respect the exceptions.
                pluginManager.registerPlugin(plugin); //Register the plugin into the system.
                EggCrack.LOGGER.fine("Plugin loaded successfully.");
            } catch (PluginLoadException e) {
                EggCrack.LOGGER.severe("Problem loading plugin from file " + pluginFile.getName() + ":");
                e.printStackTrace();
            } catch (MalformedURLException e) {
                EggCrack.LOGGER.severe("Problem loading plugin from file " + pluginFile.getName() + ":");
                e.printStackTrace();
            }
        }

        return plugins;
    }

    public void unloadPlugins() {
        for (Plugin plugin : pluginManager.listPlugins())
            try {
                plugin.setEnabled(false);
            } catch (PluginLoadException e) {
                EggCrack.LOGGER.warning("Problem disabling " + plugin.getName()+  ":");
                e.printStackTrace();
            }
    }
}
