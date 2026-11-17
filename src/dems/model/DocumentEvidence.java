package dems.model;

import java.time.LocalDateTime;

public class DocumentEvidence extends Evidence {

    public DocumentEvidence(String id, String caseId, String fileName, String storedPath, String sha256,
                            long sizeBytes, String addedBy, LocalDateTime addedAt, String description) {
        super(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
    }

    @Override
    public String getCategory() {
        return "DOCUMENT";
    }

    @Override
    public String getCategoryLabel() {
        return "Document";
    }

    @Override
    public String describe() {
        return "Document / records file (." + getExtension() + "). Check authorship and revision metadata.";
    }
}
