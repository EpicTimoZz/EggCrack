package net.teamlixo.eggcrack.authentication;

public class AuthenticationException extends Exception {
    private final AuthenticationFailure failure;

    public AuthenticationException(AuthenticationFailure failure) {
        super(failure.getMessage());

        this.failure = failure;
    }

    public AuthenticationFailure getFailure() {
        return failure;
    }

    public boolean isCritical() {
        return failure.getAction() == AuthenticationAction.STOP;
    }

    public enum AuthenticationFailure {
        INCORRECT_CREDENTIAL(AuthenticationAction.NEXT_CREDENTIALS, "Incorrect credential for account."),
        REJECTED(AuthenticationAction.RETRY_CREDENTIALS, "Authentication rejected."),
        BAD_PROXY(AuthenticationAction.RETRY_CREDENTIALS, "Bad proxy"),
        TIMEOUT(AuthenticationAction.RETRY_CREDENTIALS, "Authentication operation timed out."),
        INVALID_ACCOUNT(AuthenticationAction.STOP, "Account invalid; authentication cannot be performed."),
        NO_PROFILES(AuthenticationAction.STOP, "Account does not have any profiles."),
        INVALID_CREDENTIAL(AuthenticationAction.NEXT_CREDENTIALS, "Invalid credential for account.");

        private final AuthenticationAction action;
        private final String message;

        private AuthenticationFailure(AuthenticationAction action, String message) {
            this.action = action;
            this.message = message;
        }

        public AuthenticationAction getAction() {
            return action;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum AuthenticationAction {
        RETRY_CREDENTIALS,
        NEXT_CREDENTIALS,
        STOP
    }
}
