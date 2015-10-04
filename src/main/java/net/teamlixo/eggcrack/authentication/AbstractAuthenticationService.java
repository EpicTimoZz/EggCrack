package net.teamlixo.eggcrack.authentication;

public abstract class AbstractAuthenticationService implements AuthenticationService {
    private final String name;

    public AbstractAuthenticationService(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }
}
