package ma.emsi.game_platform_backend.shared.enums;

/**
 * Énumération des rôles utilisateur.
 *
 * SANS Spring Security :
 *   On stockerait un String "ROLE" en base et on ferait des comparaisons manuelles
 *   dans chaque Servlet/DAO : if ("ADMIN".equals(session.getAttribute("role"))) {...}
 *
 * AVEC Spring Security :
 *   Spring attend le préfixe "ROLE_" en interne. hasRole("ADMIN") correspond
 *   automatiquement à l'autorité "ROLE_ADMIN". Le mapping est géré via
 *   UserDetails.getAuthorities() → GrantedAuthority.
 */
public enum Role {
    VISITOR,
    USER,
    PREMIUM,
    ADMIN
}