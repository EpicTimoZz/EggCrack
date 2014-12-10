package net.teamlixo.eggcrack.minecraft;

import com.mojang.authlib.Agent;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.authentication.AuthenticationException;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.plugin.PluginLoadException;
import net.teamlixo.eggcrack.plugin.java.JavaPlugin;
import net.teamlixo.eggcrack.plugin.java.JavaPluginConfiguration;

import java.io.IOException;
import java.net.Proxy;
import java.util.Map;

public final class MinecraftPlugin extends JavaPlugin {
    public MinecraftPlugin(JavaPluginConfiguration pluginConfiguration) {
        super(pluginConfiguration);
    }

    @Override
    public void onEnable() throws PluginLoadException {
        try {
            Map<String, String> properties = getProperties();
        } catch (IOException e) {
            throw new PluginLoadException(e);
        }

        EggCrack.getInstance().registerAuthenticationService(
                new MojangAuthenticationService("Minecraft", new MojangAuthenticationFactory(Agent.MINECRAFT))
        );
    }

    @Override
    public void onDisable() throws PluginLoadException {

    }
}
