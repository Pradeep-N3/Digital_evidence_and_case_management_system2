package dems.exception;

/** User input is missing or invalid. */
public class ValidationException extends DemsException {
    public ValidationException(String message) {
        super(message);
    }
}
