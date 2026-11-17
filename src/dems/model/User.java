package dems.model;

import dems.util.HashUtil;

import java.util.Set;

/**
 * ABSTRACT CLASS: a system user. Concrete subclasses (AdminUser, InvestigatorUser,
 * AuditorUser) decide their role and which permissions they hold (POLYMORPHISM).
 * Passwords are never stored in plain text: we keep a random salt + SHA-256 hash.
 */
public abstract class User implements Identifiable<String> {

    private final String username;
    private final String fullName;
    private final String salt;
    private final String passwordHash;

    protected User(String username, String fullName, String salt, String passwordHash) {
        this.username = username;
        this.fullName = fullName;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    /** Which role this user has. */
    public abstract Role getRole();

    /** Which permissions this role grants. */
    protected abstract Set<Permission> grantedPermissions();

    public final boolean can(Permission permission) {
        return grantedPermissions().contains(permission);
    }

    public boolean checkPassword(String plainPassword) {
        return passwordHash.equals(hashPassword(salt, plainPassword));
    }

    public static String hashPassword(String salt, String plainPassword) {
        return HashUtil.sha256(salt + plainPassword);
    }

    /** Factory method: builds the right subclass for a role. */
    public static User create(Role role, String username, String fullName, String salt, String hash) {
        switch (role) {
            case ADMIN:
                return new AdminUser(username, fullName, salt, hash);
            case INVESTIGATOR:
                return new InvestigatorUser(username, fullName, salt, hash);
            case AUDITOR:
                return new AuditorUser(username, fullName, salt, hash);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    @Override
    public String getId() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSalt() {
        return salt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole().getLabel() + ")";
    }
}
