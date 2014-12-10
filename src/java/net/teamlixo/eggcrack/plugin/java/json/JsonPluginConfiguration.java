package net.teamlixo.eggcrack.plugin.java.json;

import com.google.gson.annotations.Expose;
import net.teamlixo.eggcrack.plugin.java.JavaPluginConfiguration;

public class JsonPluginConfiguration extends JavaPluginConfiguration {
    @Expose
    private String mainClass;

    @Expose
    private String name;

    @Expose
    private int version = 1; //Default

    @Expose
    private int versionDependency = 0; //Default

    @Override
    public int getVersionDependency() { return versionDependency; }

    @Override
    public int getVersion() {
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
