package dems.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Investigator: works only on cases they created or that were assigned to them.
 * Can add / verify / export evidence, but cannot delete anything or see the audit trail.
 */
public class InvestigatorUser extends User {

    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.VIEW_OWN_CASES,
            Permission.CREATE_CASE,
            Permission.EDIT_CASE,
            Permission.ADD_EVIDENCE,
            Permission.VERIFY_EVIDENCE,
            Permission.EXPORT_EVIDENCE));

    public InvestigatorUser(String username, String fullName, String salt, String passwordHash) {
        super(username, fullName, salt, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.INVESTIGATOR;
    }

    @Override
    protected Set<Permission> grantedPermissions() {
        return PERMISSIONS;
    }
}
