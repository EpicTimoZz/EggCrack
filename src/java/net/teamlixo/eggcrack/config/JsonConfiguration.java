package net.teamlixo.eggcrack.config;

import com.google.gson.annotations.Expose;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class JsonConfiguration extends EggCrackConfiguration {
    @Expose
    private String updateBranch;

    @Expose
    private String updateUrl;

    @Expose
    private boolean updateEnable;

    public String getUpdateBranch() {
        return updateBranch;
    }

    @Override
    public boolean isUpdateEnabled() {
        return updateEnable;
    }

    @Override
    public URL getUpdateURL() throws MalformedURLException {
        return URI.create(updateUrl.replace("$branch", updateBranch)).toURL();
    }
}
