package net.teamlixo.eggcrack.config;

import java.io.*;

public abstract class EggCrackConfiguration extends Configuration {
    @Override
    public int getVersion() throws IOException {
        return getJarVersionNumber();
    }

    public static int getJarVersionNumber() throws IOException {
        InputStream inputStream = EggCrackConfiguration.
                class.getResourceAsStream("/net/teamlixo/eggcrack/VERSION");
        if (inputStream == null || inputStream.available() <= 0)
            return 0;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        return Integer.parseInt(bufferedReader.readLine());
    }
}
