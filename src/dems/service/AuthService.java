package dems.service;

import dems.exception.PermissionDeniedException;
import dems.exception.AuthenticationException;
import dems.exception.DuplicateRecordException;
import dems.exception.ValidationException;
import dems.model.Permission;
import dems.model.Role;
import dems.model.User;
import dems.repository.Repository;
import dems.util.HashUtil;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Login and user management. */
public class AuthService {

    private final Repository<User, String> users;
    private final AuditLogger audit;

    public AuthService(Repository<User, String> users, AuditLogger audit) throws IOException {
        this.users = users;
        this.audit = audit;
        seedDefaultUsers();
    }

    /** On the very first run the user database is empty, so three demo accounts are created. */
    private void seedDefaultUsers() throws IOException {
        if (!users.findAll().isEmpty()) {
            return;
        }
        users.save(build(Role.ADMIN, "admin", "System Administrator", "admin123"));
        users.save(build(Role.INVESTIGATOR, "investigator", "Lead Investigator", "invest123"));
        users.save(build(Role.AUDITOR, "auditor", "Compliance Auditor", "audit123"));
    }

    private static User build(Role role, String username, String fullName, String password) {
        String salt = HashUtil.randomSalt();
        return User.create(role, username, fullName, salt, User.hashPassword(salt, password));
    }

    public User login(String username, String password) throws AuthenticationException {
        String name = username == null ? "" : username.trim().toLowerCase();
        User user = users.findById(name).orElse(null);
        if (user == null || !user.checkPassword(password == null ? "" : password)) {
            audit.log(name.isEmpty() ? "(blank)" : name, "LOGIN_FAILED", "Invalid username or password");
            throw new AuthenticationException("Invalid username or password.");
        }
        audit.log(user.getUsername(), "LOGIN", "Signed in as " + user.getRole().getLabel());
        return user;
    }

    public void logout(User user) {
        audit.log(user.getUsername(), "LOGOUT", "Signed out");
    }

    public User register(User actor, Role role, String username, String fullName, String password)
            throws PermissionDeniedException, DuplicateRecordException, ValidationException, IOException {
        if (!actor.can(Permission.MANAGE_USERS)) {
            audit.log(actor.getUsername(), "ACCESS_DENIED", "Tried to create a user");
            throw new PermissionDeniedException("Only an administrator can manage users.");
        }
        String name = username == null ? "" : username.trim().toLowerCase();
        if (!name.matches("[a-z0-9_.-]{3,20}")) {
            throw new ValidationException("Username must be 3-20 characters (letters, digits, . _ -).");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name is required.");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (users.exists(name)) {
            throw new DuplicateRecordException("Username '" + name + "' already exists.");
        }
        User created = build(role, name, fullName.trim(), password);
        users.save(created);
        audit.log(actor.getUsername(), "USER_CREATED", name + " (" + role.getLabel() + ")");
        return created;
    }

    public List<User> listUsers(User actor) throws PermissionDeniedException {
        if (!actor.can(Permission.MANAGE_USERS)) {
            throw new PermissionDeniedException("Only an administrator can manage users.");
        }
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getUsername))
                .collect(Collectors.toList());
    }

    /** Usernames of all investigators (used by the "assign case" drop-down). */
    public List<String> investigatorUsernames() {
        return users.findWhere(u -> u.getRole() == Role.INVESTIGATOR).stream()
                .map(User::getUsername)
                .sorted()
                .collect(Collectors.toList());
    }
}
