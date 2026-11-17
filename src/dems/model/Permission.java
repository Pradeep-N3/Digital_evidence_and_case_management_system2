package dems.model;

/** Fine-grained actions. Each role is granted a subset of these. */
public enum Permission {
    VIEW_ALL_CASES,
    VIEW_OWN_CASES,
    CREATE_CASE,
    EDIT_CASE,
    DELETE_CASE,
    ASSIGN_CASE,
    ADD_EVIDENCE,
    DELETE_EVIDENCE,
    VERIFY_EVIDENCE,
    EXPORT_EVIDENCE,
    VIEW_AUDIT,
    MANAGE_USERS
}
