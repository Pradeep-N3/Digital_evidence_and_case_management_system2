package dems.model;

import java.time.LocalDateTime;

public class ImageEvidence extends Evidence {

    public ImageEvidence(String id, String caseId, String fileName, String storedPath, String sha256,
                         long sizeBytes, String addedBy, LocalDateTime addedAt, String description) {
        super(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
    }

    @Override
    public String getCategory() {
        return "IMAGE";
    }

    @Override
    public String getCategoryLabel() {
        return "Image";
    }

    @Override
    public String describe() {
        return "Image file (." + getExtension() + "). Check EXIF metadata and signs of editing.";
    }
}
