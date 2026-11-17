package dems.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Factory: creates the right Evidence subclass from a category code or a file name. */
public final class EvidenceFactory {

    private static final Set<String> DOCUMENTS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx", "csv", "ppt", "pptx", "odt", "eml", "msg"));
    private static final Set<String> IMAGES = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "tif", "tiff", "webp", "heic"));
    private static final Set<String> MEDIA = new HashSet<>(Arrays.asList(
            "mp3", "wav", "m4a", "aac", "mp4", "avi", "mov", "mkv", "wmv", "flv"));

    private EvidenceFactory() {
    }

    /** Decides the category code from the file extension. */
    public static String categoryFor(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String ext = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
        if (DOCUMENTS.contains(ext)) {
            return "DOCUMENT";
        }
        if (IMAGES.contains(ext)) {
            return "IMAGE";
        }
        if (MEDIA.contains(ext)) {
            return "MEDIA";
        }
        return "OTHER";
    }

    public static Evidence create(String category, String id, String caseId, String fileName,
                                  String storedPath, String sha256, long sizeBytes, String addedBy,
                                  LocalDateTime addedAt, String description) {
        switch (category) {
            case "DOCUMENT":
                return new DocumentEvidence(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
            case "IMAGE":
                return new ImageEvidence(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
            case "MEDIA":
                return new MediaEvidence(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
            default:
                return new GenericEvidence(id, caseId, fileName, storedPath, sha256, sizeBytes, addedBy, addedAt, description);
        }
    }
}
