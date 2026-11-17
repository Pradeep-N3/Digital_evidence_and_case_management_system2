package dems.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Auditor: read-only. Can view everything, verify hashes and read the audit trail. */
public class AuditorUser extends User {

    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.VIEW_ALL_CASES,
            Permission.VERIFY_EVIDENCE,
            Permission.VIEW_AUDIT));

    public AuditorUser(String username, String fullName, String salt, String passwordHash) {
        super(username, fullName, salt, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.AUDITOR;
    }

    @Override
    protected Set<Permission> grantedPermissions() {
        return PERMISSIONS;
    }
}
