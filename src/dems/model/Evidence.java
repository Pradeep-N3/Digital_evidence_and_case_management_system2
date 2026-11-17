package dems.model;

import dems.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * ABSTRACT CLASS: one digital evidence item (a file) attached to a case.
 *
 * The SHA-256 hash is computed when the evidence is added. Later, verifyIntegrity()
 * recomputes the hash of the stored file; if even one byte changed, the hashes differ
 * and the item is reported as TAMPERED.
 *
 * Subclasses provide the category and a description (POLYMORPHISM).
 */
public abstract class Evidence implements Identifiable<String>, Verifiable {

    private final String id;
    private final String caseId;
    private final String fileName;
    private final String storedPath;
    private final String sha256;
    private final long sizeBytes;
    private final String addedBy;
    private final LocalDateTime addedAt;
    private final String description;
    private volatile IntegrityResult lastStatus = IntegrityResult.UNVERIFIED;

    protected Evidence(String id, String caseId, String fileName, String storedPath, String sha256,
                       long sizeBytes, String addedBy, LocalDateTime addedAt, String description) {
        this.id = id;
        this.caseId = caseId;
        this.fileName = fileName;
        this.storedPath = storedPath;
        this.sha256 = sha256;
        this.sizeBytes = sizeBytes;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
        this.description = description;
    }

    /** Short category code stored in the database: DOCUMENT, IMAGE, MEDIA or OTHER. */
    public abstract String getCategory();

    /** Human readable label of the category shown in the UI. */
    public abstract String getCategoryLabel();

    /** Short forensic hint about this kind of evidence. */
    public abstract String describe();

    @Override
    public IntegrityResult verifyIntegrity() {
        Path file = Paths.get(storedPath);
        if (!Files.isRegularFile(file)) {
            lastStatus = IntegrityResult.MISSING;
            return lastStatus;
        }
        try {
            String current = HashUtil.sha256(file);
            lastStatus = current.equalsIgnoreCase(sha256) ? IntegrityResult.INTACT : IntegrityResult.TAMPERED;
        } catch (IOException e) {
            lastStatus = IntegrityResult.MISSING;
        }
        return lastStatus;
    }

    public String getFormattedSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024L * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
    }

    public String getExtension() {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public String getSha256() {
        return sha256;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public String getDescription() {
        return description;
    }

    public IntegrityResult getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(IntegrityResult lastStatus) {
        this.lastStatus = lastStatus;
    }
}
