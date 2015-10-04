package net.teamlixo.eggcrack.plugin.java;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.teamlixo.eggcrack.plugin.Plugin;
import net.teamlixo.eggcrack.plugin.PluginLogHandler;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public abstract class JavaPlugin extends Plugin {
    private final JavaPluginConfiguration pluginConfiguration;
    private final Logger logger;

    public JavaPlugin(JavaPluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
        this.logger = Logger.getLogger(pluginConfiguration.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(new PluginLogHandler(this));
    }

    @Override
    public final String getName() {
        return pluginConfiguration.getName();
    }

    @Override
    public final int getVersion() {
        return pluginConfiguration.getVersion();
    }

    private final File getConfigurationFile() throws IOException {
        File configurationFile = new File("." + File.separator + "plugins" + File.separator + getName()
                + File.separator + "properties.json");

        if (!configurationFile.exists()) {
            InputStream inputStream = getClass().getResourceAsStream("/properties.json");
            if (inputStream == null) throw new FileNotFoundException("properties.json");

            configurationFile.getParentFile().mkdirs();
            configurationFile.createNewFile();

            ReadableByteChannel rbc = Channels.newChannel(inputStream);
            FileOutputStream fos = new FileOutputStream(configurationFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        return configurationFile;
    }

    @Override
    public final Map<String, String> getProperties() throws IOException, JsonIOException, JsonSyntaxException {
        Type listType = new TypeToken<ArrayList<HashMap<String,String>>>(){}.getType();
        List<HashMap<String, String>> list =
                (new Gson()).fromJson(new InputStreamReader(new FileInputStream(getConfigurationFile())), listType);
        Map<String, String> configurationMap = new HashMap<>();
        for (HashMap<String, String> thisMap : list)
            for (String key : thisMap.keySet())
                configurationMap.put(key, thisMap.get(key));
        return configurationMap;
    }

    public final Logger getLogger() {
        return logger;
    }
}
