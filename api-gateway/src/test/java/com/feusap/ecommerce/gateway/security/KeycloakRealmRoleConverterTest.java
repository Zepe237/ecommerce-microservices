package com.feusap.ecommerce.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    @DisplayName("maps each Keycloak realm role to a ROLE_ authority")
    void convert_mapsRealmRoles() {
        Jwt jwt = jwtWithClaims(Map.of("realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN"))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("returns no authority when the token has no realm_access claim")
    void convert_returnsEmptyWithoutRealmAccess() {
        Jwt jwt = jwtWithClaims(Map.of("scope", "openid"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("8f3c2a1e-0000-0000-0000-000000000000")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
