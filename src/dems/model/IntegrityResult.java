package dems.model;

/** Outcome of a hash verification of an evidence file. */
public enum IntegrityResult {
    UNVERIFIED("Not verified"),
    INTACT("Intact"),
    TAMPERED("TAMPERED"),
    MISSING("File missing");

    private final String label;

    IntegrityResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
