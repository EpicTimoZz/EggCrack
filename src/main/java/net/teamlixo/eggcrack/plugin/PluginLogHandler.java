package net.teamlixo.eggcrack.plugin;

import net.teamlixo.eggcrack.EggCrack;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class PluginLogHandler extends Handler {
    private Plugin plugin;

    public PluginLogHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void publish(LogRecord record) {
        EggCrack.LOGGER.log(record.getLevel(), "[" + plugin.getName() + "] " + record.getMessage());
    }

    @Override
    public void flush() {
        //
    }

    @Override
    public void close() throws SecurityException {
        //
    }
}