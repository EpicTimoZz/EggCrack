package net.teamlixo.eggcrack.plugin.java;

public abstract class JavaPluginConfiguration {

    /**
     * Gets the application version this plugin depends on.
     * @return Plugin version dependency.
     */
    public abstract int getVersionDependency();

    /**
     * Gets the plugin build version.
     * @return Plugin build version.
     */
    public abstract int getVersion();

    /**
     * Gets the plugin name.
     * @return Plugin name.
     */
    public abstract String getName();

    /**
     * Gets the plugin entry-point.
     * @return Plugin entry-point.
     */
    public abstract String getMainClass();

}
