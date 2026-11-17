package dems.model;

/**
 * INTERFACE: an object whose integrity can be checked (here: evidence files,
 * verified by comparing a freshly computed SHA-256 hash with the stored one).
 */
public interface Verifiable {
    IntegrityResult verifyIntegrity();
}
