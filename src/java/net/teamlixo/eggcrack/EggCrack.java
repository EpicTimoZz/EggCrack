package net.teamlixo.eggcrack;

import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLoadException;
import net.teamlixo.eggcrack.plugin.PluginManager;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

public final class EggCrack {
    private static EggCrack instance;
    public static final void setInstance(EggCrack newInstance) {
        instance = newInstance;
    }
    public static final EggCrack getInstance() {
        return instance;
    }

    private Map<String, AuthenticationService> authenticationServiceMap = new HashMap<>();
    private PluginManager pluginManager;

    public EggCrack(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void registerAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationServiceMap.put(authenticationService.getName(), authenticationService);
    }

    public void unregisterAuthenticationService(String protocol) {
        this.authenticationServiceMap.remove(protocol);
    }

    public Collection<AuthenticationService> getAuthenticationServices() {
        return authenticationServiceMap.values();
    }

    public List<Plugin> loadPlugins(File file) {
        List<Plugin> plugins = new ArrayList<>();

        File[] files = file.listFiles();
        for (File listedFile : files) {
            String[] extensions = listedFile.getAbsoluteFile().getName().split("[.]");
            if (extensions[extensions.length - 1].equalsIgnoreCase("jar")) {
                try {
                    plugins.add(pluginManager.getLoader().loadPlugin(file.toURI().toURL()));
                } catch (PluginLoadException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        return plugins;
    }

}
