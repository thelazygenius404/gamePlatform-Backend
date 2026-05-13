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
 * ════════════════════════════════════════════════════════════════════════════════
 *  JWT UTIL - GÉNÉRATION ET VALIDATION DE TOKENS JWT
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÔLE : Utilitaire pour créer et valider les JSON Web Tokens (JWT).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ STRUCTURE D'UN JWT                                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Un JWT est composé de 3 parties séparées par des points :
 *
 *    HEADER.PAYLOAD.SIGNATURE
 *
 * Exemple réel :
 * ─────────────
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGVtc2kubWEiLCJyb2xlIjoiVVNFUiIsImV4cCI6MTcxMDk1MjgwMH0.4sD5k3jK9mN2pQ8rT6vU7wX0yY1zA3bC5eF8gH9iJ0k
 *
 * PARTIE 1 - HEADER (Base64URL encodé) :
 * ──────────────────────────────────────
 * {
 *   "alg": "HS256",      ← Algorithme de signature (HMAC-SHA256)
 *   "typ": "JWT"         ← Type de token
 * }
 *
 * PARTIE 2 - PAYLOAD (Base64URL encodé) :
 * ───────────────────────────────────────
 * {
 *   "sub": "test@emsi.ma",           ← Subject = identifiant utilisateur (email)
 *   "role": "USER",                  ← Claim custom
 *   "pseudo": "bilal",               ← Claim custom
 *   "userId": "65f7a8cd...",         ← Claim custom
 *   "iat": 1710866400,               ← Issued At (timestamp de création)
 *   "exp": 1710952800                ← Expiration (timestamp)
 * }
 *
 * PARTIE 3 - SIGNATURE (HMAC-SHA256) :
 * ────────────────────────────────────
 * HMACSHA256(
 *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *   SECRET_KEY
 * )
 *
 * → La signature garantit que :
 *   • Le token n'a pas été modifié (intégrité)
 *   • Le token provient bien du serveur (authenticité)
 *   • Seul celui qui possède la clé secrète peut créer/valider le token
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ COMPARAISON : JWT vs SESSION SERVEUR                                       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * SESSION CLASSIQUE (STATEFUL) :
 * ─────────────────────────────
 *    Client                          Serveur
 *      │                               │
 *      │ POST /login                   │
 *      ├──────────────────────────────>│
 *      │                               │ Création HttpSession
 *      │                               │ Stockage en mémoire :
 *      │                               │   Map<String, HttpSession>
 *      │                               │   sessionId → {userId, role, ...}
 *      │                               │
 *      │ 200 OK                        │
 *      │ Set-Cookie: JSESSIONID=ABC123 │
 *      │<──────────────────────────────┤
 *      │                               │
 *      │ GET /api/games                │
 *      │ Cookie: JSESSIONID=ABC123     │
 *      ├──────────────────────────────>│
 *      │                               │ Lookup en mémoire :
 *      │                               │   sessions.get("ABC123")
 *      │                               │   → {userId: "123", role: "USER"}
 *      │                               │
 *      │ 200 OK                        │
 *      │<──────────────────────────────┤
 *
 * JWT (STATELESS) :
 * ────────────────
 *    Client                          Serveur
 *      │                               │
 *      │ POST /login                   │
 *      ├──────────────────────────────>│
 *      │                               │ Génération JWT :
 *      │                               │   payload = {userId, role, exp}
 *      │                               │   signature = HMAC(payload, secret)
 *      │                               │   token = header.payload.signature
 *      │                               │
 *      │ 200 OK                        │
 *      │ { "token": "eyJhbG..." }      │
 *      │<──────────────────────────────┤
 *      │                               │
 *      │ Stockage dans localStorage    │
 *      │                               │
 *      │ GET /api/games                │
 *      │ Authorization: Bearer eyJ...  │
 *      ├──────────────────────────────>│
 *      │                               │ Vérification signature :
 *      │                               │   HMAC(header.payload, secret)
 *      │                               │   == signature ?
 *      │                               │
 *      │                               │ Vérification expiration :
 *      │                               │   now < exp ?
 *      │                               │
 *      │                               │ Extraction des claims :
 *      │                               │   userId, role depuis payload
 *      │                               │
 *      │                               │ AUCUN LOOKUP EN BASE
 *      │                               │
 *      │ 200 OK                        │
 *      │<──────────────────────────────┤
 *
 * AVANTAGES JWT :
 * ──────────────
 * ✅ STATELESS : Pas de stockage côté serveur (scalabilité horizontale)
 * ✅ PERFORMANCE : Pas de lookup en base/mémoire à chaque requête
 * ✅ MOBILE : Fonctionne nativement (pas de cookies compliqués)
 * ✅ MICROSERVICES : Chaque service peut valider indépendamment
 * ✅ CROSS-DOMAIN : Pas de problème Same-Origin Policy
 *
 * INCONVÉNIENTS JWT :
 * ──────────────────
 * ❌ RÉVOCATION : Difficile de révoquer avant expiration (solution : blacklist)
 * ❌ TAILLE : Plus gros qu'un JSESSIONID (150-300 bytes vs 20 bytes)
 * ❌ SÉCURITÉ : Si volé, valide jusqu'à expiration (solution : TTL court + refresh)
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Component : Enregistre cette classe comme un bean Spring.
 *              Permet l'injection dans les services/filtres.
 */
@Component
public class JwtUtil {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CONFIGURATION EXTERNALISÉE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Value : Injection de valeurs depuis application.yml
     *
     * SANS SPRING :
     * ────────────
     *    public class JwtUtil {
     *        private static final String SECRET_KEY = "ma_cle_secrete_super_longue_pour_hmac_sha256";
     *        private static final long EXPIRATION_MS = 86400000; // 24h
     *    }
     *
     *    → Valeurs hardcodées dans le code
     *    → Recompilation nécessaire pour changer
     *    → Même valeur en dev/test/prod (dangereux)
     *
     * AVEC SPRING (@Value) :
     * ─────────────────────
     *    @Value("${app.jwt.secret}")
     *    private String secretKey;
     *
     *    Fichier application.yml :
     *    app:
     *      jwt:
     *        secret: ${JWT_SECRET:default_secret_for_dev}
     *        expiration-ms: 86400000
     *
     *    Fichier application-prod.yml :
     *    app:
     *      jwt:
     *        secret: ${JWT_SECRET}  ← Variable d'environnement
     *        expiration-ms: 3600000 ← 1h en prod (plus sécurisé)
     *
     *    → Configuration externalisée
     *    → Différente par environnement
     *    → Secret en variable d'environnement en prod
     *    → Aucune recompilation nécessaire
     */
    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * GÉNÉRATION DE LA CLÉ DE SIGNATURE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * HMAC-SHA256 requiert une clé d'au moins 256 bits (32 bytes).
     *
     * Keys.hmacShaKeyFor() :
     *    • Génère une SecretKey depuis une chaîne de caractères
     *    • Valide automatiquement la longueur minimale
     *    • Lance une exception si la clé est trop courte
     *
     * SANS JJWT :
     * ──────────
     *    SecretKeySpec key = new SecretKeySpec(
     *        secretKey.getBytes(StandardCharsets.UTF_8),
     *        "HmacSHA256"
     *    );
     *
     *    → Pas de validation de la longueur
     *    → Risque de clé faible
     *
     * AVEC JJWT :
     * ──────────
     *    Keys.hmacShaKeyFor(secretKey.getBytes())
     *
     *    → Validation automatique
     *    → Exception claire si clé trop courte
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
     * ════════════════════════════════════════════════════════════════════════════
     * GÉNÉRATION D'UN JWT
     * ════════════════════════════════════════════════════════════════════════════
     *
     * SANS BIBLIOTHÈQUE (Implémentation manuelle - 100+ lignes) :
     * ──────────────────────────────────────────────────────────
     *    public String generateToken(String subject, Map<String, Object> claims) {
     *        // 1. Construction du header (JSON)
     *        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
     *        String headerEncoded = Base64.getUrlEncoder()
     *            .withoutPadding()
     *            .encodeToString(header.getBytes(StandardCharsets.UTF_8));
     *
     *        // 2. Construction du payload (JSON)
     *        long now = System.currentTimeMillis();
     *        long exp = now + expirationMs;
     *
     *        StringBuilder payloadJson = new StringBuilder("{");
     *        payloadJson.append("\"sub\":\"").append(subject).append("\",");
     *        payloadJson.append("\"iat\":").append(now / 1000).append(",");
     *        payloadJson.append("\"exp\":").append(exp / 1000);
     *
     *        // Ajout des claims personnalisés
     *        for (Map.Entry<String, Object> entry : claims.entrySet()) {
     *            payloadJson.append(",\"").append(entry.getKey()).append("\":\"")
     *                       .append(entry.getValue()).append("\"");
     *        }
     *        payloadJson.append("}");
     *
     *        String payloadEncoded = Base64.getUrlEncoder()
     *            .withoutPadding()
     *            .encodeToString(payloadJson.toString().getBytes(StandardCharsets.UTF_8));
     *
     *        // 3. Calcul de la signature HMAC-SHA256
     *        String data = headerEncoded + "." + payloadEncoded;
     *
     *        try {
     *            Mac mac = Mac.getInstance("HmacSHA256");
     *            SecretKeySpec secretKeySpec = new SecretKeySpec(
     *                secretKey.getBytes(StandardCharsets.UTF_8),
     *                "HmacSHA256"
     *            );
     *            mac.init(secretKeySpec);
     *            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
     *
     *            String signatureEncoded = Base64.getUrlEncoder()
     *                .withoutPadding()
     *                .encodeToString(signatureBytes);
     *
     *            // 4. Assemblage final
     *            return data + "." + signatureEncoded;
     *
     *        } catch (Exception e) {
     *            throw new RuntimeException("Erreur génération JWT", e);
     *        }
     *    }
     *
     *    → 100+ lignes de code
     *    → Gestion manuelle de l'encodage Base64URL
     *    → Gestion manuelle du JSON
     *    → Gestion manuelle de HMAC-SHA256
     *    → Risque d'erreur (typo, mauvais algorithme, etc.)
     *
     * AVEC JJWT (Bibliothèque io.jsonwebtoken) :
     * ─────────────────────────────────────────
     *    → 10 lignes (voir ci-dessous)
     *    → Sécurisé et testé
     *    → Support de tous les algorithmes JWT
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
    /**
     * ════════════════════════════════════════════════════════════════════════════
     * EXTRACTION DES CLAIMS (Données du payload)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Claims = Données stockées dans le payload du JWT.
     * Exemples : userId, role, pseudo, email, etc.
     *
     * SANS BIBLIOTHÈQUE (Parsing manuel - 30+ lignes) :
     * ─────────────────────────────────────────────────
     *    public String extractUserId(String token) {
     *        try {
     *            String[] parts = token.split("\\.");
     *            String payloadEncoded = parts[1];
     *
     *            String payloadJson = new String(
     *                Base64.getUrlDecoder().decode(payloadEncoded),
     *                StandardCharsets.UTF_8
     *            );
     *
     *            ObjectMapper mapper = new ObjectMapper();
     *            JsonNode payload = mapper.readTree(payloadJson);
     *
     *            return payload.get("userId").asText();
     *
     *        } catch (Exception e) {
     *            throw new RuntimeException("Erreur extraction userId", e);
     *        }
     *    }
     *
     *    → 15+ lignes par claim
     *    → Duplication pour chaque claim (userId, role, pseudo, etc.)
     *
     * AVEC JJWT :
     * ──────────
     *    → 1 ligne par claim (voir ci-dessous)
     *    → Type-safe (conversion automatique)
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * VALIDATION D'UN JWT
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Vérifications effectuées :
     *    1. Format du token (3 parties séparées par des points)
     *    2. Décodage Base64URL
     *    3. Vérification de la signature HMAC-SHA256
     *    4. Vérification de l'expiration (claim "exp")
     *
     * SANS BIBLIOTHÈQUE (Implémentation manuelle - 80+ lignes) :
     * ─────────────────────────────────────────────────────────
     *    public boolean isTokenValid(String token) {
     *        try {
     *            // 1. Vérification du format
     *            String[] parts = token.split("\\.");
     *            if (parts.length != 3) {
     *                return false;
     *            }
     *
     *            String headerEncoded = parts[0];
     *            String payloadEncoded = parts[1];
     *            String signatureEncoded = parts[2];
     *
     *            // 2. Recalcul de la signature
     *            String data = headerEncoded + "." + payloadEncoded;
     *
     *            Mac mac = Mac.getInstance("HmacSHA256");
     *            SecretKeySpec secretKeySpec = new SecretKeySpec(
     *                secretKey.getBytes(StandardCharsets.UTF_8),
     *                "HmacSHA256"
     *            );
     *            mac.init(secretKeySpec);
     *            byte[] calculatedSignature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
     *
     *            String calculatedSignatureEncoded = Base64.getUrlEncoder()
     *                .withoutPadding()
     *                .encodeToString(calculatedSignature);
     *
     *            // 3. Comparaison des signatures
     *            if (!calculatedSignatureEncoded.equals(signatureEncoded)) {
     *                return false; // Signature invalide (token modifié)
     *            }
     *
     *            // 4. Vérification de l'expiration
     *            String payloadJson = new String(
     *                Base64.getUrlDecoder().decode(payloadEncoded),
     *                StandardCharsets.UTF_8
     *            );
     *
     *            // Parsing JSON manuel (ou avec Jackson)
     *            ObjectMapper mapper = new ObjectMapper();
     *            JsonNode payload = mapper.readTree(payloadJson);
     *            long exp = payload.get("exp").asLong();
     *            long now = System.currentTimeMillis() / 1000;
     *
     *            if (now >= exp) {
     *                return false; // Token expiré
     *            }
     *
     *            return true;
     *
     *        } catch (Exception e) {
     *            return false;
     *        }
     *    }
     *
     *    → 80+ lignes de code
     *    → Gestion manuelle des exceptions
     *    → Risque d'erreur de sécurité (timing attack, etc.)
     *
     * AVEC JJWT :
     * ──────────
     *    → 7 lignes (voir ci-dessous)
     *    → Gestion automatique des exceptions
     *    → Sécurisé contre les attaques connues
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