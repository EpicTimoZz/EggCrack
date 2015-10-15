package net.teamlixo.eggcrack.proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class RunnableProxyChecker implements Runnable {
    private Proxy proxy;
    private URL url;
    private ProxyCallback callback;
    private int timeout;

    public RunnableProxyChecker(Proxy proxy, URL url, ProxyCallback callback, int timeout) {
        this.proxy = proxy;
        this.url = url;
        this.callback = callback;
        this.timeout = Math.max(1, timeout);
    }

    @Override
    public void run() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);
            urlConnection.setConnectTimeout(timeout);
            urlConnection.setUseCaches(false);
            urlConnection.connect();
        } catch (IOException e) {
            callback.onProxyFailed(proxy);
        }
    }
}
