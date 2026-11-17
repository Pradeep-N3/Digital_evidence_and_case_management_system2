package dems.model;

import java.time.LocalDateTime;

/** A criminal / forensic case that evidence files are attached to. */
public class CaseRecord implements Identifiable<String> {

    private final String id;
    private String title;
    private String description;
    private CaseStatus status;
    private String assignedTo;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public CaseRecord(String id, String title, String description, CaseStatus status,
                      String assignedTo, String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** True if this user created the case or it is assigned to them. */
    public boolean isOwnedBy(String username) {
        return username != null
                && (username.equalsIgnoreCase(assignedTo) || username.equalsIgnoreCase(createdBy));
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return id + " - " + title;
    }
}
