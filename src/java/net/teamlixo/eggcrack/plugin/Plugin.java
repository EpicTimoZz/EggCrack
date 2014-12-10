package net.teamlixo.eggcrack.plugin;

import net.teamlixo.eggcrack.EggCrack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base plugin class.
 */
public abstract class Plugin {
    private boolean enabled;

    /**
     * Sets the enabled status of the plugin.
     * @param enabled Plugin enable status.
     */
    public final void setEnabled(boolean enabled) throws PluginLoadException {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                EggCrack.LOGGER.info("Enabling " + getName() + " version " + getVersion() + ".");
                onEnable();
            } else {
                EggCrack.LOGGER.info("Disabling " + getName() + ".");
                onDisable();
            }
        }
    }

    /**
     * Gets the plugin name.
     * @return plugin name.
     */
    public abstract String getName();

    /**
     * Gets the plugin version.
     * @return plugin version.
     */
    public abstract int getVersion();

    /**
     * Gets a map of properties provided by the plugin configuration.
     * @return plugin properties.
     */
    public abstract Map<String, String> getProperties() throws IOException;

    /**
     * Called when the plugin enables.
     */
    public abstract void onEnable() throws PluginLoadException;

    /**
     * Called when the plugin disables.
     */
    public abstract void onDisable() throws PluginLoadException;

}
