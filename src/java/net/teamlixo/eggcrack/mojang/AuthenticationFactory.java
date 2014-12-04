package net.teamlixo.eggcrack.mojang;

import com.mojang.authlib.UserAuthentication;

import java.net.Proxy;

public interface AuthenticationFactory {

    /**
     * Creates a new Mojang user authentication instance using the given proxy.
     * @param proxy Proxy to use.
     * @return User authentication instance.
     */
    public UserAuthentication createUserAuthentication(Proxy proxy);

}
