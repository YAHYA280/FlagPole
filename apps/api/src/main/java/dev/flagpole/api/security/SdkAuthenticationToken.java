package dev.flagpole.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class SdkAuthenticationToken extends AbstractAuthenticationToken {

    public static final String ROLE = "SDK";

    private final SdkPrincipal principal;

    public SdkAuthenticationToken(SdkPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // never keep the SDK key around after authentication
    }

    @Override
    public SdkPrincipal getPrincipal() {
        return principal;
    }
}
