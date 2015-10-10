package net.teamlixo.eggcrack.authentication;

import net.teamlixo.eggcrack.authentication.configuration.ServiceConfiguration;

public abstract class AbstractAuthenticationService implements AuthenticationService {
    private final String name;
    private final ServiceConfiguration configuration = new ServiceConfiguration();

    public AbstractAuthenticationService(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public ServiceConfiguration getConfiguration() {
        return configuration;
    }
}
