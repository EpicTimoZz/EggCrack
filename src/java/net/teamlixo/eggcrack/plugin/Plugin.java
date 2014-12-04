package net.teamlixo.eggcrack.plugin;

/**
 * Base plugin class.
 */
public abstract class Plugin {
    private boolean enabled;

    /**
     * Sets the enabled status of the plugin.
     * @param enabled Plugin enable status.
     */
    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled)
                onEnable();
            else
                onDisable();
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
    public abstract String getVersion();

    /**
     * Called when the plugin enables.
     */
    public abstract void onEnable();

    /**
     * Called when the plugin disables.
     */
    public abstract void onDisable();

}
