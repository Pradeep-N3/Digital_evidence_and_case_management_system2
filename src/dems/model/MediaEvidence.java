package dems.model;

import java.time.LocalDateTime;

public class MediaEvidence extends Evidence {

    public MediaEvidence(String id, String caseId, String fileName, String storedPath, String sha256,
                         long sizeBytes, String addedBy, LocalDateTime addedAt, String description) {
        super(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
    }

    @Override
    public String getCategory() {
        return "MEDIA";
    }

    @Override
    public String getCategoryLabel() {
        return "Audio / Video";
    }

    @Override
    public String describe() {
        return "Audio / video recording (." + getExtension() + "). Check timestamps and continuity.";
    }
}
