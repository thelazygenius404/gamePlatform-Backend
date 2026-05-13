package ma.emsi.game_platform_backend.iam.security;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================================
 *  UserDetailsService — Pont entre Spring Security et MongoDB
 * ============================================================
 *
 * RÔLE DANS L'ARCHITECTURE SPRING SECURITY :
 * -------------------------------------------
 * Spring Security utilise l'interface UserDetailsService pour charger
 * les informations d'un utilisateur lors de l'authentification.
 * C'est le seul point d'intégration nécessaire entre votre domaine
 * métier et la chaîne d'authentification de Spring Security.
 *
 * APPROCHE SANS Spring Security :
 * --------------------------------
 * Dans un filtre Servlet manuel, on chargerait l'utilisateur directement :
 *
 *   String token = request.getHeader("Authorization").substring(7);
 *   String userId = JwtUtil.extractUserId(token);
 *   User user = userDAO.findById(userId);
 *   if (user == null || user.getStatus() != ACTIVE) {
 *     response.sendError(401);
 *     return;
 *   }
 *   // Stocker dans request.setAttribute() pour les Servlets suivantes
 *   request.setAttribute("currentUser", user);
 *
 * Problèmes : pas de standardisation, chaque filtre réimplémente la logique,
 * aucune intégration avec la gestion des rôles/permissions.
 *
 * APPROCHE AVEC Spring Security :
 * ---------------------------------
 * Spring Security appelle loadUserByUsername() automatiquement lors de
 * l'authentification. Le UserDetails retourné est placé dans le
 * SecurityContext et accessible partout via :
 *   SecurityContextHolder.getContext().getAuthentication()
 *
 * Spring Security appelle automatiquement :
 *   - isEnabled()           → vérifie AccountStatus
 *   - isAccountNonLocked()  → vérifie le verrouillage (RG-03)
 *   - isCredentialsNonExpired() → vérifie si le mdp a expiré
 *   - getAuthorities()      → retourne les rôles pour @PreAuthorize
 *
 * @Service : bean géré par Spring IoC.
 * @RequiredArgsConstructor : @Autowired implicite sur UserRepository (Lombok).
 * ============================================================
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son email (utilisé comme "username").
     *
     * @param email l'email de l'utilisateur
     * @return UserDetails encapsulant notre User MongoDB
     * @throws UsernameNotFoundException si l'email n'existe pas en base
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'email : " + email));

        /*
         * Conversion Role → GrantedAuthority.
         * Spring Security attend le préfixe "ROLE_" pour hasRole().
         * Ex: Role.ADMIN → SimpleGrantedAuthority("ROLE_ADMIN")
         *
         * SANS Spring : comparaison manuelle de String dans chaque filtre.
         * AVEC Spring : @PreAuthorize("hasRole('ADMIN')") fonctionne automatiquement.
         */
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        /*
         * org.springframework.security.core.userdetails.User (builder)
         * encapsule notre User MongoDB dans le contrat UserDetails.
         *
         * isEnabled()           → status == ACTIVE (non suspendu, non supprimé)
         * isAccountNonLocked()  → compte non verrouillé (RG-03)
         * isAccountNonExpired() → toujours true ici (pas d'expiration de compte)
         * isCredentialsNonExpired() → toujours true (pas d'expiration de mdp)
         */
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(user.getStatus() ==
                        ma.emsi.game_platform_backend.shared.enums.AccountStatus.SUSPENDED ||
                        user.getStatus() ==
                                ma.emsi.game_platform_backend.shared.enums.AccountStatus.DELETED)
                .accountLocked(user.isLocked())
                .build();
    }
}