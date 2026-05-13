package ma.emsi.game_platform_backend.shared.enums;


/**
 * Statut d'un compte utilisateur.
 *
 * Ces valeurs pilotent la logique d'autorisation dans AuthServiceImpl :
 * - ACTIVE   : connexion autorisée
 * - SUSPENDED: bloqué par un admin (RG-20 : login refusé)
 * - LOCKED   : verrouillé temporairement après X échecs (RG-03)
 * - DELETED  : soft-delete, compte non récupérable via login
 *
 * SANS Spring : on vérifierait ce statut manuellement dans le Servlet de login.
 * AVEC Spring Security : intégré dans UserDetails.isEnabled(), isAccountNonLocked(),
 * etc., Spring Security appelle ces méthodes automatiquement lors de l'authentification.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    LOCKED,
    DELETED
}