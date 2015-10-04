package net.teamlixo.eggcrack.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class Configuration {

    /**
     * Finds if updates are enabled for this configuration.
     * @return true if updates are enabled, false otherwise.
     */
    public abstract boolean isUpdateEnabled();

    /**
     * Finds the URL used to update the program.
     * @return update URL.
     */
    public abstract URL getUpdateURL() throws MalformedURLException;

    /**
     * Finds the version of the program, incremented by one every
     * release.
     * @return version number.
     */
    public abstract int getVersion() throws IOException;

}
