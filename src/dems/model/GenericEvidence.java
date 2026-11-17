package dems.model;

import java.time.LocalDateTime;

/** Any other digital file (logs, disk images, archives, executables...). */
public class GenericEvidence extends Evidence {

    public GenericEvidence(String id, String caseId, String fileName, String storedPath, String sha256,
                           long sizeBytes, String addedBy, LocalDateTime addedAt, String description) {
        super(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
    }

    @Override
    public String getCategory() {
        return "OTHER";
    }

    @Override
    public String getCategoryLabel() {
        return "Digital file";
    }

    @Override
    public String describe() {
        return "General digital file (." + getExtension() + "). Handle read-only and keep the hash on record.";
    }
}
