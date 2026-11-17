# Digital Evidence & Case Management System (DEMS)

Java OOP mini project. Desktop app (Swing) for storing digital evidence against cases,
with role-based access and SHA-256 tamper detection.

## 1. What you need to install

Only a **JDK (Java Development Kit) 8 or newer** (17 or 21 recommended).
No libraries, no database server, no Maven/Gradle.

Check it is installed (both commands must print a version):

    java -version
    javac -version

If `javac` is "not recognized", you have only a JRE: install a JDK
(e.g. Temurin / Adoptium from https://adoptium.net) and reopen the terminal.

## 2. How to run

- **Windows:** double-click `run.bat`  (or run it from a terminal)
- **Linux / macOS:** `sh run.sh`
- **Manually:**

      javac -d out -sourcepath src src/dems/Main.java
      java -cp out dems.Main

Run it from the project folder. A `data/` folder is created next to it on first start.

## 3. Login accounts (created on first run)

| Role         | Username       | Password    | What it can do |
|--------------|----------------|-------------|----------------|
| Administrator| `admin`        | `admin123`  | Everything: all cases, create/edit/delete cases and evidence, assign cases, manage users, audit log |
| Investigator | `investigator` | `invest123` | Create cases; view/edit **only own cases** (created by or assigned to them); add, verify, export evidence. Cannot delete, cannot see audit log or users |
| Auditor      | `auditor`      | `audit123`  | **Read-only**: view all cases and evidence, verify hashes, read audit log. Cannot add or change anything |

The admin can create more users (Users screen).

## 4. Quick demo (shows hash verification)

1. Sign in as `investigator`, **New case**, give it a title.
2. **Evidence > Add evidence**, pick a file. The SHA-256 is computed and shown.
3. Open `data/evidence_files/` and change the stored file (edit and save it).
4. Back in the app: **Verify selected** (or **Verify all**). The item turns **TAMPERED**,
   export is blocked, and a `TAMPER_ALERT` appears in the audit log (sign in as `auditor` or `admin`).
5. Sign in as `auditor` to see the read-only view, as `admin` to manage users.

To start fresh, close the app and delete the `data/` folder.

## 5. Where is the "database"?

Records are stored in plain text files inside `data/`:

| File | Content |
|------|---------|
| `users.db`    | users (passwords stored as salted SHA-256, never plain text) |
| `cases.db`    | cases |
| `evidence.db` | evidence records (name, SHA-256, size, who added, status ...) |
| `audit.log`   | who did what and when |
| `evidence_files/` | the stored copies of evidence files |

This keeps the project install-free. All access goes through the generic `Repository<T, ID>`
interface, so replacing `FileRepository` with a JDBC/SQLite/MySQL implementation later does
not change the services or the UI.

## 6. Project structure and where each OOP concept is used

    src/dems/
      Main.java
      model/       User (abstract) -> AdminUser, InvestigatorUser, AuditorUser
                   Evidence (abstract) -> DocumentEvidence, ImageEvidence, MediaEvidence, GenericEvidence
                   CaseRecord, Role, Permission, CaseStatus, IntegrityResult (enums)
                   Identifiable<ID>, Verifiable (interfaces), EvidenceFactory
      exception/   DemsException -> AuthenticationException, PermissionDeniedException,
                   ValidationException, DuplicateRecordException, RecordNotFoundException,
                   TamperDetectedException
      repository/  Repository<T,ID> (generic interface), FileRepository<T> (generic class)
      service/     AppContext, AuthService, CaseService, AuditLogger, IntegrityScanner
      util/        HashUtil (SHA-256), RecordCodec, TimeUtil
      ui/          LoginFrame, MainFrame, CasePanel, EvidencePanel, AuditPanel, UserPanel,
                   FormDialog (abstract) -> CaseDialog, EvidenceDialog, UserDialog,
                   SimpleTableModel<T>, Refreshable (interface), Theme

| Requirement | Where |
|-------------|-------|
| Classes and methods | every class; e.g. `CaseService.addEvidence(...)`, `HashUtil.sha256(...)` |
| Inheritance | `User` -> `AdminUser`/`InvestigatorUser`/`AuditorUser`; `Evidence` -> 4 subclasses; `DemsException` -> 6 exceptions; `FormDialog` -> 3 dialogs |
| Abstract classes | `User` (`getRole`, `grantedPermissions`), `Evidence` (`getCategory`, `describe`), `FormDialog` (`validateAndSave`) |
| Interfaces | `Identifiable<ID>`, `Verifiable`, `Repository<T,ID>`, `Refreshable` |
| Packages | `dems.model`, `dems.exception`, `dems.repository`, `dems.service`, `dems.util`, `dems.ui` |
| Exception handling | custom checked exceptions, `try/catch/finally`, multi-catch (`catch (DemsException \| IOException ex)`), try-with-resources in `HashUtil`, `FileRepository`, `AuditLogger` |
| Multithreading | `AuditLogger` (background writer thread + `BlockingQueue`, producer/consumer); `IntegrityScanner` (`ExecutorService` thread pool hashing files in parallel with `Callable`/`Future`); `SwingWorker` keeps the UI responsive; `synchronized` methods in `CaseService`, `FileRepository` |
| File I/O | `FileRepository` (read/write database files, atomic temp-file swap), `HashUtil` (streams files), `AuditLogger`, evidence copy with `Files.copy`, audit report export |
| Generics | `Repository<T extends Identifiable<ID>, ID>`, `FileRepository<T>`, `SimpleTableModel<T>`, `MainFrame.addPage(<P extends JPanel & Refreshable>)` |
| Lambda expressions | repository encoders/decoders in `AppContext`, `repo.findWhere(c -> ...)`, stream pipelines in `CaseService`, button listeners, `Theme.colorColumn(..., s -> ...)`, table columns `c -> c.getStatus()` |
| Collection classes | `LinkedHashMap` (repository store), `TreeMap` (scan results), `HashSet`/`EnumSet` (permissions, file types), `ArrayList`, `List`, `BlockingQueue`, `Set`, `Optional` |
| Hash for modification check | `Evidence.verifyIntegrity()` recomputes SHA-256 of the stored file and compares it with the hash saved at intake |
| Role based limits | `Permission` enum + `canView` / `canModify` in `CaseService`; checked in the service layer, not only hidden in the UI |

## 7. Notes

- Passwords: random salt + SHA-256 (fine for a mini project; real systems use bcrypt/Argon2).
- The hash proves the stored file is unchanged *since it was added*; it cannot prove the file was
  genuine before it was added.
- Evidence of a closed case cannot receive new files (re-open the case first).
