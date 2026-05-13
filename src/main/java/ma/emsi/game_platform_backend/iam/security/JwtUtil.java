package ma.emsi.game_platform_backend.iam.security;

import io.jsonwebtoken.Claims; // <--- Utilise bien celui de io.jsonwebtoken
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * ============================================================
 *  Utilitaire JWT — Génération et Validation des tokens
 * ============================================================
 *
 * APPROCHE SANS JWT (session classique Servlet) :
 * -----------------------------------------------
 *   // Création d'une session lors du login
 *   HttpSession session = request.getSession(true);
 *   session.setAttribute("userId", user.getId());
 *   session.setAttribute("role", user.getRole().name());
 *   session.setMaxInactiveInterval(30 * 60); // 30 min
 *
 *   // Vérification dans un filtre
 *   HttpSession session = request.getSession(false);
 *   if (session == null || session.getAttribute("userId") == null) {
 *     response.sendError(401, "Non authentifié");
 *     return;
 *   }
 *
 * Problèmes de l'approche session :
 *   - STATEFUL : le serveur doit maintenir en mémoire toutes les sessions actives
 *   - SCALABILITÉ : avec plusieurs instances (load balancer), les sessions ne sont pas
 *     partagées sans infrastructure supplémentaire (Redis, sticky sessions)
 *   - COOKIES : sensible au CSRF (Cross-Site Request Forgery)
 *
 * APPROCHE JWT (JSON Web Token) :
 * --------------------------------
 * Un JWT est composé de 3 parties encodées en Base64URL, séparées par des points :
 *   HEADER.PAYLOAD.SIGNATURE
 *
 * Exemple de payload :
 *   { "sub": "userId123", "role": "USER", "iat": 1234567890, "exp": 1234654290 }
 *
 * Le serveur signe le token avec une clé secrète (HMAC-SHA256).
 * Pour valider, il re-signe les données reçues et compare → pas besoin de lookup DB.
 *
 * Avantages :
 *   ✓ STATELESS : aucun état côté serveur
 *   ✓ SCALABLE : fonctionne avec N instances sans partage d'état
 *   ✓ PORTABLE : utilisable par mobile, SPA, API tierce
 *   ✓ CLAIMS intégrés : userId, role, expiration dans le token lui-même
 *
 * @Component : bean Spring géré par l'IoC Container.
 * SANS Spring : instanciation manuelle, problème de partage de l'instance.
 * ============================================================
 */
@Component
public class JwtUtil {

    /**
     * @Value injecte la valeur depuis application.yml.
     * SANS Spring : System.getenv("JWT_SECRET") ou Properties.load() manuellement.
     */
    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Clé HMAC-SHA256 dérivée du secret.
     * La librairie jjwt utilise Keys.hmacShaKeyFor() pour garantir
     * une clé d'au moins 256 bits (requis pour HS256).
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un JWT signé contenant userId et role comme claims.
     *
     * @param userId  identifiant MongoDB de l'utilisateur
     * @param role    rôle de l'utilisateur (ex: "USER", "ADMIN")
     * @return token JWT sous forme de String
     */
    public String generateToken(String userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)                          // "sub" claim = userId
                .claim("role", role)                      // claim personnalisé
                .issuedAt(now)                            // "iat" claim
                .expiration(expiry)                       // "exp" claim
                .signWith(getSigningKey(), Jwts.SIG.HS256) // Signature HMAC-SHA256
                .compact();                               // Encode et retourne le String
    }

    /**
     * Génère un token avec des claims supplémentaires (pseudo, email).
     */
    public String generateToken(String userId, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claims(extraClaims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extrait tous les claims d'un JWT (après vérification de la signature).
     * Lance une JwtException si le token est invalide ou expiré.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Valide un JWT : vérifie la signature et l'expiration.
     * SANS Spring : implémentation manuelle de la vérification HMAC.
     * AVEC jjwt : parseSignedClaims() lance une exception si invalide.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token); // Lance une exception si invalide
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}