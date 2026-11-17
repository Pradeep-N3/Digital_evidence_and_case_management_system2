package dems.service;

import dems.model.CaseRecord;
import dems.model.CaseStatus;
import dems.model.Evidence;
import dems.model.EvidenceFactory;
import dems.model.Role;
import dems.model.User;
import dems.repository.FileRepository;
import dems.util.RecordCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Wires everything together: creates the "database" files, the repositories and the services.
 * The encode / decode LAMBDAS below define the file format of each table.
 */
public class AppContext {

    public final AuditLogger audit;
    public final AuthService auth;
    public final CaseService cases;

    private AppContext(AuditLogger audit, AuthService auth, CaseService cases) {
        this.audit = audit;
        this.auth = auth;
        this.cases = cases;
    }

    public static AppContext start(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        AuditLogger audit = new AuditLogger(dataDir.resolve("audit.log"));

        FileRepository<User> users = new FileRepository<>(dataDir.resolve("users.db"),
                u -> RecordCodec.encode(u.getUsername(), u.getFullName(), u.getRole().name(),
                        u.getSalt(), u.getPasswordHash()),
                line -> {
                    String[] f = RecordCodec.decode(line);
                    return User.create(Role.valueOf(f[2]), f[0], f[1], f[3], f[4]);
                });

        FileRepository<CaseRecord> caseRepo = new FileRepository<>(dataDir.resolve("cases.db"),
                c -> RecordCodec.encode(c.getId(), c.getTitle(), c.getDescription(), c.getStatus().name(),
                        c.getAssignedTo(), c.getCreatedBy(), c.getCreatedAt().toString()),
                line -> {
                    String[] f = RecordCodec.decode(line);
                    return new CaseRecord(f[0], f[1], f[2], CaseStatus.valueOf(f[3]), f[4], f[5],
                            LocalDateTime.parse(f[6]));
                });

        FileRepository<Evidence> evidenceRepo = new FileRepository<>(dataDir.resolve("evidence.db"),
                e -> RecordCodec.encode(e.getCategory(), e.getId(), e.getCaseId(), e.getFileName(),
                        e.getStoredPath(), e.getSha256(), String.valueOf(e.getSizeBytes()), e.getAddedBy(),
                        e.getAddedAt().toString(), e.getDescription(), e.getLastStatus().name()),
                line -> {
                    String[] f = RecordCodec.decode(line);
                    Evidence ev = EvidenceFactory.create(f[0], f[1], f[2], f[3], f[4], f[5],
                            Long.parseLong(f[6]), f[7], LocalDateTime.parse(f[8]), f[9]);
                    ev.setLastStatus(dems.model.IntegrityResult.valueOf(f[10]));
                    return ev;
                });

        AuthService auth = new AuthService(users, audit);
        CaseService cases = new CaseService(caseRepo, evidenceRepo, audit, dataDir.resolve("evidence_files"));
        return new AppContext(audit, auth, cases);
    }

    public void shutdown() {
        audit.shutdown();
    }
}
