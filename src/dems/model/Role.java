package dems.model;

public enum Role {
    ADMIN("Administrator", "Full access: manage users, cases, evidence and the audit trail."),
    INVESTIGATOR("Investigator", "Create and update your own cases, add and verify evidence."),
    AUDITOR("Auditor", "Read-only access. Verify evidence integrity and review the audit trail.");

    private final String label;
    private final String summary;

    Role(String label, String summary) {
        this.label = label;
        this.summary = summary;
    }

    public String getLabel() {
        return label;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return label;
    }
}
