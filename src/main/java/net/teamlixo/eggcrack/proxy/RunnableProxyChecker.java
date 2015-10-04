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

    public RunnableProxyChecker(Proxy proxy, URL url, ProxyCallback callback) {
        this.proxy = proxy;
        this.url = url;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);
            urlConnection.setConnectTimeout(1000);
            urlConnection.setReadTimeout(1000);
            urlConnection.connect();

            if (urlConnection.getResponseCode() / 100 != 2)
                callback.onProxyFailed(proxy);

            urlConnection.getInputStream().close();
        } catch (IOException e) {
            callback.onProxyFailed(proxy);
        }
    }
}
