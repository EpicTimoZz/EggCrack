package net.teamlixo.eggcrack.plugin;

public class PluginLoadException extends Exception {
    public PluginLoadException(Exception cause) {
        super(cause);
    }
    public PluginLoadException(String message) {
        super(message);
    }
}
