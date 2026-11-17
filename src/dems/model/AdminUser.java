package dems.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Administrator: unrestricted access. */
public class AdminUser extends User {

    private static final Set<Permission> PERMISSIONS =
            Collections.unmodifiableSet(EnumSet.allOf(Permission.class));

    public AdminUser(String username, String fullName, String salt, String passwordHash) {
        super(username, fullName, salt, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    protected Set<Permission> grantedPermissions() {
        return PERMISSIONS;
    }
}
