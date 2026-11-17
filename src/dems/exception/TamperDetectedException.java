package dems.exception;

/** The stored evidence file no longer matches the hash recorded when it was added. */
public class TamperDetectedException extends DemsException {
    public TamperDetectedException(String message) {
        super(message);
    }
}
