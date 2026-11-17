package dems.exception;

/** Base class of all application-specific (checked) exceptions. */
public class DemsException extends Exception {
    public DemsException(String message) {
        super(message);
    }
}
