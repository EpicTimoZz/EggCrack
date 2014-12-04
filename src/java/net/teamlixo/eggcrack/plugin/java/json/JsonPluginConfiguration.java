package net.teamlixo.eggcrack.plugin.java.json;

import com.google.gson.annotations.Expose;
import net.teamlixo.eggcrack.plugin.java.JavaPluginConfiguration;

public class JsonPluginConfiguration extends JavaPluginConfiguration {
    @Expose
    private String mainClass;

    @Expose
    private String name;

    @Expose
    private String version;

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMainClass() {
        return mainClass;
    }
}
