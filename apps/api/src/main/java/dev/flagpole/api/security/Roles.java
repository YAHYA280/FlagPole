package dev.flagpole.api.security;

/**
 * Realm roles defined in Keycloak (infra/keycloak/flagpole-realm.json), upper-cased by
 * {@link KeycloakRealmRoleConverter}. Use in {@code @PreAuthorize("hasRole(...)")}.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    private Roles() {
    }
}
