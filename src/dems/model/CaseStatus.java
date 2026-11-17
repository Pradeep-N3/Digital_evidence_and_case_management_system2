package dems.model;

public enum CaseStatus {
    OPEN("Open"),
    UNDER_INVESTIGATION("Under Investigation"),
    CLOSED("Closed");

    private final String label;

    CaseStatus(String label) {
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
