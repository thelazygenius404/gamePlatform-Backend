package ma.emsi.game_platform_backend.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.repository.SessionRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 *  Filtre JWT — Intercepte chaque requête HTTP
 * ============================================================
 *
 * APPROCHE SANS Spring Security (filtre Servlet manuel) :
 * --------------------------------------------------------
 * En J2EE classique, on implémenterait javax.servlet.Filter :
 *
 *   @WebFilter("/*")
 *   public class JwtFilter implements Filter {
 *
 *     @Override
 *     public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
 *         throws IOException, ServletException {
 *
 *       HttpServletRequest request = (HttpServletRequest) req;
 *       HttpServletResponse response = (HttpServletResponse) res;
 *
 *       String path = request.getRequestURI();
 *       // Exclusion manuelle des routes publiques
 *       if (path.startsWith("/api/auth/")) {
 *         chain.doFilter(req, res);
 *         return;
 *       }
 *
 *       String header = request.getHeader("Authorization");
 *       if (header == null || !header.startsWith("Bearer ")) {
 *         response.setStatus(401);
 *         response.getWriter().write("{\"error\": \"Token manquant\"}");
 *         return;
 *       }
 *
 *       String token = header.substring(7);
 *       // Validation manuelle du token, extraction du userId
 *       // Chargement de l'utilisateur depuis la DB
 *       // Stockage dans request.setAttribute("currentUser", user)
 *       // Aucune intégration avec les rôles/permissions
 *
 *       chain.doFilter(req, res);
 *     }
 *   }
 *
 * Problèmes :
 *   - Enregistrement manuel dans web.xml ou @WebFilter
 *   - Pas d'intégration avec un système de rôles
 *   - Gestion des exceptions manuelle dans chaque filtre
 *   - Difficile à tester unitairement
 *
 * APPROCHE AVEC Spring Security (OncePerRequestFilter) :
 * -------------------------------------------------------
 * OncePerRequestFilter garantit qu'il est exécuté UNE SEULE fois par requête
 * (même en cas de forward/include internes).
 *
 * Après validation, on place un UsernamePasswordAuthenticationToken dans le
 * SecurityContext → Spring Security gère automatiquement les autorisations
 * dans toute la chaîne (contrôleurs, services avec @PreAuthorize, etc.).
 *
 * CHAÎNE DE FILTRES SPRING SECURITY :
 * Pour visualisation UML, voici l'ordre d'exécution :
 *
 * HTTP Request
 *   → SecurityContextPersistenceFilter
 *   → UsernamePasswordAuthenticationFilter (désactivé ici, on utilise JWT)
 *   → JwtAuthFilter  ← NOTRE FILTRE (inséré avant UsernamePasswordAuthFilter)
 *   → ExceptionTranslationFilter
 *   → FilterSecurityInterceptor
 *   → Controller
 * ============================================================
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── 1. Extraction du header Authorization ──────────────────────
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → on laisse passer (Spring Security refusera plus loin
            // si la route est protégée via SecurityFilterChain)
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // Retire "Bearer "

        // ── 2. Validation du format et de la signature JWT ────────────
        if (!jwtUtil.isTokenValid(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 3. Vérification de la blacklist (tokens révoqués) ──────────
        // Un token peut être valide cryptographiquement mais révoqué (logout).
        // SANS Spring : vérification manuelle dans chaque filtre.
        // AVEC Spring : ce filtre centralisé vérifie pour toutes les routes.
        if (sessionRepository.existsByJwtTokenAndIsRevokedTrue(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token révoqué. Veuillez vous reconnecter.\"}");
            return;
        }

        // ── 4. Extraction de l'userId et chargement depuis MongoDB ─────
        final String userId = jwtUtil.extractUserId(jwt);

        /*
         * On ne charge l'utilisateur que si le SecurityContext est vide
         * (évite les chargements redondants pour une même requête).
         *
         * SANS Spring : on ne saurait pas si l'utilisateur a déjà été
         * authentifié dans cette requête → risque de double chargement.
         */
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // loadUserByUsername() → MongoDB query findByEmail()
            // Ici on charge par email via le claim "sub" qui contient l'userId.
            // Adaptation : on peut stocker l'email dans le token aussi.
            String role = jwtUtil.extractRole(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    // Note : on utilise l'email comme subject dans generateToken()
                    // Si le subject est le userId, adapter findByEmail → findById
                    userId
            );

            // ── 5. Construction du token d'authentification Spring Security ──
            /*
             * UsernamePasswordAuthenticationToken représente une authentification réussie.
             * Paramètres : (principal, credentials, authorities)
             * - principal    : UserDetails (contient email, rôle)
             * - credentials  : null (pas besoin après validation JWT)
             * - authorities  : List<GrantedAuthority> (ROLE_USER, ROLE_ADMIN...)
             *
             * SANS Spring : on stockerait userId + role dans request.setAttribute()
             * et on ferait des if/else dans chaque Servlet.
             */
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ── 6. Stockage dans le SecurityContext ───────────────────────
            /*
             * SecurityContextHolder utilise un ThreadLocal → accessible dans
             * toute la stack de cette requête (service, contrôleur, etc.).
             *
             * SANS Spring : injection dans request.setAttribute("auth", ...) puis
             * cast dans chaque couche → fragile, non typé.
             */
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}