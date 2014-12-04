package net.teamlixo.eggcrack.account.output;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class UrlAccountOutput extends AccountOutput {
    private static final String CHARSET = "UTF-8";
    private final URL url;

    public UrlAccountOutput(URL url) {
        this.url = url;
    }

    @Override
    public void save(Account account, Credential credential) throws IOException {
        String query = String.format(
                "username=%s&password=%s",
                URLEncoder.encode(account.getUsername(), CHARSET),
                URLEncoder.encode(credential.toString(), CHARSET)
        );

        synchronized (url) {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Accept-Charset", CHARSET);
            urlConnection.setRequestProperty("User-Agent", "EggCrack");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(false);

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(query.getBytes(CHARSET));
            outputStream.flush();

            if (urlConnection.getResponseCode() / 100 != 2)
                throw new IOException("Request failed (HTTP " + urlConnection.getResponseCode() + "): "
                        + urlConnection.getResponseMessage());

            //Safely close the connection.
            urlConnection.getInputStream().close();
        }
    }
}
