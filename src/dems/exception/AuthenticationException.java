package dems.exception;

/** Wrong username or password. */
public class AuthenticationException extends DemsException {
    public AuthenticationException(String message) {
        super(message);
    }
}
