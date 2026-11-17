package dems.exception;

/** The logged-in user's role does not allow the requested action. */
public class PermissionDeniedException extends DemsException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
