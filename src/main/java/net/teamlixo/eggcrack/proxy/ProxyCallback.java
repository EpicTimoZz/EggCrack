package net.teamlixo.eggcrack.proxy;

import java.net.Proxy;

public interface ProxyCallback {
    public void onProxyFailed(Proxy proxy);
}
