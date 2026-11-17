package dems.service;

import dems.exception.PermissionDeniedException;
import dems.exception.DemsException;
import dems.exception.RecordNotFoundException;
import dems.exception.TamperDetectedException;
import dems.exception.ValidationException;
import dems.model.CaseRecord;
import dems.model.CaseStatus;
import dems.model.Evidence;
import dems.model.EvidenceFactory;
import dems.model.IntegrityResult;
import dems.model.Permission;
import dems.model.User;
import dems.repository.Repository;
import dems.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All case and evidence operations. Every method receives the acting User and checks
 * their permissions first, so the rules are enforced here and not only in the UI.
 */
public class CaseService {

    private final Repository<CaseRecord, String> caseRepo;
    private final Repository<Evidence, String> evidenceRepo;
    private final AuditLogger audit;
    private final IntegrityScanner scanner = new IntegrityScanner(4);
    private final Path evidenceDir;

    public CaseService(Repository<CaseRecord, String> caseRepo, Repository<Evidence, String> evidenceRepo,
                       AuditLogger audit, Path evidenceDir) throws IOException {
        this.caseRepo = caseRepo;
        this.evidenceRepo = evidenceRepo;
        this.audit = audit;
        this.evidenceDir = evidenceDir;
        Files.createDirectories(evidenceDir);
    }

    // ------------------------------------------------------------------ permissions

    private void require(User actor, Permission permission, String action) throws PermissionDeniedException {
        if (!actor.can(permission)) {
            deny(actor, action);
        }
    }

    private void deny(User actor, String action) throws PermissionDeniedException {
        audit.log(actor.getUsername(), "ACCESS_DENIED", "Tried to " + action);
        throw new PermissionDeniedException(actor.getRole().getLabel() + " is not allowed to " + action + ".");
    }

    /** Admin and Auditor see every case; an Investigator sees only their own. */
    public boolean canView(User actor, CaseRecord c) {
        return actor.can(Permission.VIEW_ALL_CASES)
                || (actor.can(Permission.VIEW_OWN_CASES) && c.isOwnedBy(actor.getUsername()));
    }

    /** Only users who may edit cases AND can see this particular case. Auditors never can. */
    public boolean canModify(User actor, CaseRecord c) {
        return actor.can(Permission.EDIT_CASE) && canView(actor, c);
    }

    // ------------------------------------------------------------------ cases

    public List<CaseRecord> visibleCases(User actor) {
        return caseRepo.findWhere(c -> canView(actor, c)).stream()
                .sorted(Comparator.comparing(CaseRecord::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<CaseRecord> modifiableCases(User actor) {
        return visibleCases(actor).stream()
                .filter(c -> canModify(actor, c) && c.getStatus() != CaseStatus.CLOSED)
                .collect(Collectors.toList());
    }

    public synchronized CaseRecord createCase(User actor, String title, String description, String assignedTo)
            throws DemsException, IOException {
        require(actor, Permission.CREATE_CASE, "create cases");
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Case title is required.");
        }
        String assignee = actor.can(Permission.ASSIGN_CASE) ? (assignedTo == null ? "" : assignedTo)
                : actor.getUsername();
        String id = nextId("CASE-", caseRepo.findAll().stream().map(CaseRecord::getId));
        CaseRecord c = new CaseRecord(id, title.trim(), description == null ? "" : description.trim(),
                CaseStatus.OPEN, assignee, actor.getUsername(), LocalDateTime.now());
        caseRepo.save(c);
        audit.log(actor.getUsername(), "CASE_CREATED", id + " - " + c.getTitle());
        return c;
    }

    public synchronized void updateCase(User actor, String caseId, String title, String description,
                                        CaseStatus status, String assignedTo) throws DemsException, IOException {
        CaseRecord c = requireCase(caseId);
        if (!canModify(actor, c)) {
            deny(actor, "modify case " + caseId);
        }
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Case title is required.");
        }
        c.setTitle(title.trim());
        c.setDescription(description == null ? "" : description.trim());
        c.setStatus(status);
        if (actor.can(Permission.ASSIGN_CASE)) {
            c.setAssignedTo(assignedTo == null ? "" : assignedTo);
        }
        caseRepo.save(c);
        audit.log(actor.getUsername(), "CASE_UPDATED", caseId + " status=" + status.getLabel());
    }

    public synchronized void deleteCase(User actor, String caseId) throws DemsException, IOException {
        require(actor, Permission.DELETE_CASE, "delete cases");
        requireCase(caseId);
        for (Evidence e : evidenceRepo.findWhere(ev -> ev.getCaseId().equals(caseId))) {
            Files.deleteIfExists(Paths.get(e.getStoredPath()));
            evidenceRepo.delete(e.getId());
        }
        caseRepo.delete(caseId);
        audit.log(actor.getUsername(), "CASE_DELETED", caseId + " and its evidence");
    }

    // ------------------------------------------------------------------ evidence

    public List<Evidence> visibleEvidence(User actor) {
        Set<String> caseIds = visibleCases(actor).stream().map(CaseRecord::getId).collect(Collectors.toSet());
        return evidenceRepo.findWhere(e -> caseIds.contains(e.getCaseId())).stream()
                .sorted(Comparator.comparing(Evidence::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<Evidence> evidenceForCase(String caseId) {
        return evidenceRepo.findWhere(e -> e.getCaseId().equals(caseId));
    }

    /**
     * Copies the chosen file into the evidence store and records its SHA-256 hash.
     * The hash of the original and of the stored copy must match, otherwise the copy is rejected.
     */
    public synchronized Evidence addEvidence(User actor, String caseId, Path source, String description)
            throws DemsException, IOException {
        require(actor, Permission.ADD_EVIDENCE, "add evidence");
        CaseRecord c = requireCase(caseId);
        if (!canModify(actor, c)) {
            deny(actor, "add evidence to case " + caseId);
        }
        if (c.getStatus() == CaseStatus.CLOSED) {
            throw new ValidationException("Case " + caseId + " is closed. Re-open it to add evidence.");
        }
        if (source == null || !Files.isRegularFile(source)) {
            throw new ValidationException("Please choose a valid file.");
        }

        String id = nextId("EVD-", evidenceRepo.findAll().stream().map(Evidence::getId));
        String originalName = source.getFileName().toString();
        Path stored = evidenceDir.resolve(id + "_" + originalName);

        String sourceHash = HashUtil.sha256(source);
        Files.copy(source, stored, StandardCopyOption.REPLACE_EXISTING);
        String storedHash = HashUtil.sha256(stored);
        if (!sourceHash.equals(storedHash)) {
            Files.deleteIfExists(stored);
            throw new IOException("The stored copy does not match the original file. Evidence was not added.");
        }

        Evidence e = EvidenceFactory.create(EvidenceFactory.categoryFor(originalName), id, caseId, originalName,
                stored.toString(), storedHash, Files.size(stored), actor.getUsername(), LocalDateTime.now(),
                description == null ? "" : description.trim());
        e.setLastStatus(IntegrityResult.INTACT);
        evidenceRepo.save(e);
        audit.log(actor.getUsername(), "EVIDENCE_ADDED",
                id + " to " + caseId + " (" + originalName + ", SHA-256 " + storedHash.substring(0, 12) + "...)");
        return e;
    }

    public synchronized void deleteEvidence(User actor, String evidenceId) throws DemsException, IOException {
        require(actor, Permission.DELETE_EVIDENCE, "delete evidence");
        Evidence e = requireEvidence(evidenceId);
        Files.deleteIfExists(Paths.get(e.getStoredPath()));
        evidenceRepo.delete(evidenceId);
        audit.log(actor.getUsername(), "EVIDENCE_DELETED", evidenceId + " (" + e.getFileName() + ")");
    }

    // ------------------------------------------------------------------ integrity

    public IntegrityResult verifyEvidence(User actor, String evidenceId) throws DemsException, IOException {
        require(actor, Permission.VERIFY_EVIDENCE, "verify evidence");
        Evidence e = requireVisibleEvidence(actor, evidenceId);
        IntegrityResult result = e.verifyIntegrity();
        evidenceRepo.save(e);
        audit.log(actor.getUsername(), result == IntegrityResult.INTACT ? "EVIDENCE_VERIFIED" : "TAMPER_ALERT",
                evidenceId + " -> " + result.getLabel());
        return result;
    }

    /** Verifies every evidence item the user can see, using a pool of worker threads. */
    public Map<String, IntegrityResult> verifyAll(User actor)
            throws DemsException, IOException, InterruptedException {
        require(actor, Permission.VERIFY_EVIDENCE, "verify evidence");
        List<Evidence> items = visibleEvidence(actor);
        Map<String, IntegrityResult> results = scanner.scan(items);
        evidenceRepo.saveAll(items);
        long problems = results.values().stream().filter(r -> r != IntegrityResult.INTACT).count();
        audit.log(actor.getUsername(), problems == 0 ? "VERIFY_ALL" : "TAMPER_ALERT",
                items.size() + " item(s) checked, " + problems + " problem(s)");
        return results;
    }

    /** Exports a copy of the evidence file, but only if its hash still matches. */
    public Path exportEvidence(User actor, String evidenceId, Path targetFolder) throws DemsException, IOException {
        require(actor, Permission.EXPORT_EVIDENCE, "export evidence");
        Evidence e = requireVisibleEvidence(actor, evidenceId);
        IntegrityResult result = e.verifyIntegrity();
        evidenceRepo.save(e);
        if (result != IntegrityResult.INTACT) {
            audit.log(actor.getUsername(), "EXPORT_BLOCKED", evidenceId + " -> " + result.getLabel());
            throw new TamperDetectedException("Export blocked: " + evidenceId + " is " + result.getLabel()
                    + ". The file no longer matches its recorded SHA-256 hash.");
        }
        Path target = targetFolder.resolve(e.getFileName());
        Files.copy(Paths.get(e.getStoredPath()), target, StandardCopyOption.REPLACE_EXISTING);
        audit.log(actor.getUsername(), "EVIDENCE_EXPORTED", evidenceId + " to " + targetFolder);
        return target;
    }

    // ------------------------------------------------------------------ helpers

    private CaseRecord requireCase(String caseId) throws RecordNotFoundException {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new RecordNotFoundException("Case " + caseId + " not found."));
    }

    private Evidence requireEvidence(String evidenceId) throws RecordNotFoundException {
        return evidenceRepo.findById(evidenceId)
                .orElseThrow(() -> new RecordNotFoundException("Evidence " + evidenceId + " not found."));
    }

    private Evidence requireVisibleEvidence(User actor, String evidenceId) throws DemsException {
        Evidence e = requireEvidence(evidenceId);
        CaseRecord c = requireCase(e.getCaseId());
        if (!canView(actor, c)) {
            deny(actor, "access evidence " + evidenceId);
        }
        return e;
    }

    /** Next id such as CASE-0004: finds the highest number already used and adds one. */
    private static String nextId(String prefix, Stream<String> existingIds) {
        int max = existingIds
                .map(id -> id.substring(prefix.length()))
                .mapToInt(CaseService::safeParse)
                .max()
                .orElse(0);
        return String.format("%s%04d", prefix, max + 1);
    }

    private static int safeParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
