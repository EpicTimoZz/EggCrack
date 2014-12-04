package net.teamlixo.eggcrack.minecraft;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.net.Proxy;
import java.util.UUID;

public class MinecraftAuthenticationFactory implements AuthenticationFactory {
    @Override
    public UserAuthentication createUserAuthentication(Proxy proxy) {
        return new YggdrasilAuthenticationService(
                        proxy,
                        UUID.randomUUID().toString()
        ).createUserAuthentication(Agent.MINECRAFT);
    }
}
