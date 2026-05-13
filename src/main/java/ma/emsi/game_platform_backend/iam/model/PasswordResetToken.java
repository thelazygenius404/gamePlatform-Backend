package ma.emsi.game_platform_backend.iam.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  PASSWORD RESET TOKEN - COMPARAISON SPRING vs APPROCHE CLASSIQUE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * RÈGLE MÉTIER (RG-04) : Réinitialisation de mot de passe
 *    • L'utilisateur demande un reset via son email
 *    • Un token unique est généré et valide 1 heure
 *    • Le token est envoyé par email
 *    • L'utilisateur clique sur le lien et définit un nouveau mot de passe
 *    • Le token ne peut être utilisé qu'une seule fois
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE (JDBC + Table SQL)                                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CRÉATION DE LA TABLE SQL :
 *    ───────────────────────────
 *    CREATE TABLE password_reset_tokens (
 *        id VARCHAR(50) PRIMARY KEY,
 *        user_id VARCHAR(50) NOT NULL,
 *        token VARCHAR(255) UNIQUE NOT NULL,
 *        expires_at TIMESTAMP NOT NULL,
 *        used BOOLEAN DEFAULT FALSE,
 *        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
 *    );
 *
 *    CREATE INDEX idx_token ON password_reset_tokens(token);
 *    CREATE INDEX idx_expires_at ON password_reset_tokens(expires_at);
 *
 * 2. GÉNÉRATION DU TOKEN (Servlet "Forgot Password") :
 *    ──────────────────────────────────────────────────
 *    @WebServlet("/forgot-password")
 *    public class ForgotPasswordServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            String email = request.getParameter("email");
 *
 *            // Recherche de l'utilisateur
 *            Connection conn = null;
 *            PreparedStatement ps = null;
 *            ResultSet rs = null;
 *
 *            try {
 *                conn = dataSource.getConnection();
 *
 *                // Vérification de l'existence de l'email
 *                String sql = "SELECT id FROM users WHERE email = ?";
 *                ps = conn.prepareStatement(sql);
 *                ps.setString(1, email);
 *                rs = ps.executeQuery();
 *
 *                if (!rs.next()) {
 *                    // Ne pas révéler si l'email existe (sécurité)
 *                    response.setStatus(200);
 *                    response.getWriter().write("{\"message\": \"Email sent if exists\"}");
 *                    return;
 *                }
 *
 *                String userId = rs.getString("id");
 *                rs.close();
 *                ps.close();
 *
 *                // Suppression des anciens tokens de cet utilisateur
 *                String deleteSql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
 *                ps = conn.prepareStatement(deleteSql);
 *                ps.setString(1, userId);
 *                ps.executeUpdate();
 *                ps.close();
 *
 *                // Génération du token
 *                String token = UUID.randomUUID().toString();
 *                LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
 *
 *                // Insertion du token
 *                String insertSql = "INSERT INTO password_reset_tokens " +
 *                                 "(id, user_id, token, expires_at, used, created_at) " +
 *                                 "VALUES (?, ?, ?, ?, FALSE, NOW())";
 *                ps = conn.prepareStatement(insertSql);
 *                ps.setString(1, UUID.randomUUID().toString());
 *                ps.setString(2, userId);
 *                ps.setString(3, token);
 *                ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
 *                ps.executeUpdate();
 *
 *                // Envoi de l'email (code omis pour la brièveté)
 *                String resetLink = "https://app.emsi.ma/reset-password?token=" + token;
 *                // emailService.send(email, resetLink);
 *
 *                response.setStatus(200);
 *                response.getWriter().write("{\"message\": \"Email sent\"}");
 *
 *            } catch (SQLException e) {
 *                e.printStackTrace();
 *                response.sendError(500, "Database error");
 *            } finally {
 *                // Fermeture des ressources (try-with-resources non utilisé pour la démo)
 *                try {
 *                    if (rs != null) rs.close();
 *                    if (ps != null) ps.close();
 *                    if (conn != null) conn.close();
 *                } catch (SQLException e) {
 *                    e.printStackTrace();
 *                }
 *            }
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • ~80 lignes de code pour une opération simple
 *      • Gestion manuelle des ressources JDBC (risque de fuite)
 *      • Code SQL embarqué dans le servlet (pas de séparation DAO)
 *      • Gestion manuelle des conversions (LocalDateTime → Timestamp)
 *      • Try-catch-finally répétitif
 *
 * 3. VÉRIFICATION ET RESET DU MOT DE PASSE :
 *    ────────────────────────────────────────
 *    @WebServlet("/reset-password")
 *    public class ResetPasswordServlet extends HttpServlet {
 *        protected void doPost(HttpServletRequest request, HttpServletResponse response) {
 *            String token = request.getParameter("token");
 *            String newPassword = request.getParameter("newPassword");
 *
 *            Connection conn = null;
 *            PreparedStatement ps = null;
 *            ResultSet rs = null;
 *
 *            try {
 *                conn = dataSource.getConnection();
 *
 *                // Recherche et vérification du token
 *                String sql = "SELECT user_id, expires_at, used " +
 *                           "FROM password_reset_tokens " +
 *                           "WHERE token = ?";
 *                ps = conn.prepareStatement(sql);
 *                ps.setString(1, token);
 *                rs = ps.executeQuery();
 *
 *                if (!rs.next()) {
 *                    response.sendError(400, "Token invalide");
 *                    return;
 *                }
 *
 *                String userId = rs.getString("user_id");
 *                Timestamp expiresAt = rs.getTimestamp("expires_at");
 *                boolean used = rs.getBoolean("used");
 *
 *                // Vérification expiration
 *                if (expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) {
 *                    response.sendError(400, "Token expiré");
 *                    return;
 *                }
 *
 *                // Vérification usage
 *                if (used) {
 *                    response.sendError(400, "Token déjà utilisé");
 *                    return;
 *                }
 *
 *                rs.close();
 *                ps.close();
 *
 *                // Hachage du nouveau mot de passe
 *                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
 *
 *                // Mise à jour du mot de passe
 *                String updateUserSql = "UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?";
 *                ps = conn.prepareStatement(updateUserSql);
 *                ps.setString(1, hashedPassword);
 *                ps.setString(2, userId);
 *                ps.executeUpdate();
 *                ps.close();
 *
 *                // Marquer le token comme utilisé
 *                String updateTokenSql = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";
 *                ps = conn.prepareStatement(updateTokenSql);
 *                ps.setString(1, token);
 *                ps.executeUpdate();
 *
 *                response.setStatus(200);
 *                response.getWriter().write("{\"message\": \"Password reset successful\"}");
 *
 *            } catch (SQLException e) {
 *                e.printStackTrace();
 *                response.sendError(500, "Database error");
 *            } finally {
 *                // Fermeture des ressources
 *                try {
 *                    if (rs != null) rs.close();
 *                    if (ps != null) ps.close();
 *                    if (conn != null) conn.close();
 *                } catch (SQLException e) {
 *                    e.printStackTrace();
 *                }
 *            }
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • ~70 lignes de code pour la vérification et le reset
 *      • Logique métier mélangée avec le code SQL
 *      • Pas de gestion transactionnelle (risque d'incohérence si update échoue)
 *      • Code difficile à tester unitairement
 *
 * 4. NETTOYAGE DES TOKENS EXPIRÉS :
 *    ────────────────────────────────
 *    // Job CRON (Quartz Scheduler ou @Scheduled manuel)
 *    public class TokenCleanupJob {
 *        public void execute() {
 *            Connection conn = null;
 *            PreparedStatement ps = null;
 *
 *            try {
 *                conn = dataSource.getConnection();
 *
 *                String sql = "DELETE FROM password_reset_tokens WHERE expires_at < NOW()";
 *                ps = conn.prepareStatement(sql);
 *                int deleted = ps.executeUpdate();
 *
 *                System.out.println("Deleted " + deleted + " expired tokens");
 *
 *            } catch (SQLException e) {
 *                e.printStackTrace();
 *            } finally {
 *                try {
 *                    if (ps != null) ps.close();
 *                    if (conn != null) conn.close();
 *                } catch (SQLException e) {
 *                    e.printStackTrace();
 *                }
 *            }
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • Job à configurer manuellement (Quartz, cron système)
 *      • Consommation CPU périodique
 *      • Code de nettoyage à écrire et maintenir
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE AVEC SPRING BOOT + SPRING DATA MONGODB                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. ENTITÉ AVEC LOMBOK ET SPRING DATA :
 *    ─────────────────────────────────────
 *    @Document(collection = "password_reset_tokens")
 *    @Builder
 *    public class PasswordResetToken {
 *        @Id
 *        private String id;
 *
 *        private String userId;
 *
 *        @Builder.Default
 *        private String token = UUID.randomUUID().toString();
 *
 *        @Indexed(expireAfterSeconds = 0)
 *        @Builder.Default
 *        private LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
 *
 *        @Builder.Default
 *        private boolean used = false;
 *    }
 *
 *    → RÉSULTAT : ~15 lignes vs ~80 lignes SQL + Java
 *
 * 2. REPOSITORY (Interface uniquement) :
 *    ────────────────────────────────────
 *    public interface PasswordResetTokenRepository
 *            extends MongoRepository<PasswordResetToken, String> {
 *        Optional<PasswordResetToken> findByToken(String token);
 *        Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
 *        void deleteByUserId(String userId);
 *    }
 *
 *    → Spring Data génère automatiquement l'implémentation
 *
 * 3. SERVICE (Logique métier) :
 *    ───────────────────────────
 *    @Service
 *    public class AuthServiceImpl {
 *        public void forgotPassword(String email) {
 *            userRepository.findByEmail(email).ifPresent(user -> {
 *                // Suppression des anciens tokens
 *                resetTokenRepository.deleteByUserId(user.getId());
 *
 *                // Génération du nouveau token
 *                PasswordResetToken token = PasswordResetToken.builder()
 *                    .userId(user.getId())
 *                    .createdAt(LocalDateTime.now())
 *                    .build();
 *
 *                resetTokenRepository.save(token);
 *
 *                // Envoi de l'email
 *                String link = "https://app.emsi.ma/reset?token=" + token.getToken();
 *                // emailService.send(user.getEmail(), link);
 *            });
 *        }
 *
 *        public void resetPassword(String token, String newPassword) {
 *            PasswordResetToken resetToken = resetTokenRepository
 *                .findByTokenAndUsedFalse(token)
 *                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));
 *
 *            if (!resetToken.isValid()) {
 *                throw new IllegalArgumentException("Token expiré");
 *            }
 *
 *            User user = userRepository.findById(resetToken.getUserId())
 *                .orElseThrow(() -> new IllegalArgumentException("User not found"));
 *
 *            user.setPassword(passwordEncoder.encode(newPassword));
 *            user.setUpdatedAt(LocalDateTime.now());
 *            userRepository.save(user);
 *
 *            resetToken.invalidate();
 *            resetTokenRepository.save(resetToken);
 *        }
 *    }
 *
 *    → RÉSULTAT : ~30 lignes vs ~150 lignes JDBC
 *
 * 4. NETTOYAGE AUTOMATIQUE (TTL MongoDB) :
 *    ──────────────────────────────────────
 *    @Indexed(expireAfterSeconds = 0)
 *    private LocalDateTime expiresAt;
 *
 *    → MongoDB supprime automatiquement les documents expirés
 *    → AUCUN code de nettoyage nécessaire
 *    → AUCUN job cron à configurer
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ COMPARAISON CHIFFRÉE                                                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌────────────────────────┬─────────────────────┬──────────────────────────────┐
 * │ CRITÈRE                │ JDBC Classique      │ Spring Data MongoDB          │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Lignes de code total   │ ~250 lignes         │ ~50 lignes                   │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Gestion ressources     │ Manuelle (finally)  │ Automatique (try-with-res)   │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Mapping objet          │ Manuel (15+ lignes) │ Automatique (@Document)      │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Nettoyage tokens       │ Job cron manuel     │ TTL MongoDB automatique      │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Transactions           │ Manuelle (commit)   │ @Transactional               │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Tests unitaires        │ Mock JDBC complexe  │ @DataMongoTest simple        │
 * ├────────────────────────┼─────────────────────┼──────────────────────────────┤
 * │ Séparation couches     │ ❌ Mixte servlet/DAO│ ✅ Controller/Service/Repo   │
 * └────────────────────────┴─────────────────────┴──────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ AVANTAGES DE L'APPROCHE SPRING                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ✅ RÉDUCTION DU CODE : 80% de code en moins
 * ✅ MAINTENANCE : Ajout d'un champ = 1 ligne vs 10+ lignes SQL/Java
 * ✅ SÉCURITÉ : Pas de risque de fuite de ressources JDBC
 * ✅ NETTOYAGE : Automatique via TTL MongoDB (zéro code)
 * ✅ TESTABILITÉ : Tests simples avec @DataMongoTest
 * ✅ LISIBILITÉ : Code déclaratif et intention claire
 *
 * ════════════════════════════════════════════════════════════════════════════════
 */
@Document(collection = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    private String id;

    /** FK vers User._id */
    private String userId;

    /**
     * Token unique généré automatiquement via UUID.
     * @Builder.Default permet d'initialiser automatiquement lors de la construction.
     *
     * SANS @Builder.Default :
     *    PasswordResetToken token = PasswordResetToken.builder()
     *        .userId(userId)
     *        .token(UUID.randomUUID().toString()) // À spécifier manuellement
     *        .build();
     *
     * AVEC @Builder.Default :
     *    PasswordResetToken token = PasswordResetToken.builder()
     *        .userId(userId)
     *        .build(); // Token généré automatiquement
     */
    @Indexed(unique = true)
    @Builder.Default
    private String token = UUID.randomUUID().toString();

    /**
     * Date d'expiration du token (RG-04 : 1 heure après création).
     *
     * @Indexed(expireAfterSeconds = 0) :
     *    → Crée un index TTL (Time To Live) MongoDB
     *    → MongoDB supprime automatiquement le document après expiration
     *    → expireAfterSeconds = 0 signifie "supprimer à la date expiresAt"
     *
     * SANS TTL (Approche classique) :
     *    → Job cron manuel pour nettoyer les tokens expirés
     *    → Consommation CPU périodique
     *    → Code supplémentaire à maintenir
     *
     * AVEC TTL (Spring Data MongoDB) :
     *    → Suppression automatique en arrière-plan par MongoDB
     *    → Aucun code Java nécessaire
     *    → Performance optimale (index natif MongoDB)
     */
    @Indexed(expireAfterSeconds = 0)
    @Builder.Default
    private LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

    /**
     * Flag indiquant si le token a déjà été utilisé.
     * Évite la réutilisation du même token (sécurité).
     */
    @Builder.Default
    private boolean used = false;

    private LocalDateTime createdAt;

    // ════════════════════════════════════════════════════════════════════════════
    // MÉTHODES MÉTIER (Encapsulation de la logique)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie si le token est valide (non utilisé et non expiré).
     *
     * SANS méthode métier :
     *    if (!token.isUsed() && LocalDateTime.now().isBefore(token.getExpiresAt())) {
     *        // Valide
     *    }
     *    → Logique dispersée et dupliquée dans les services
     *
     * AVEC méthode métier :
     *    if (token.isValid()) {
     *        // Valide
     *    }
     *    → Logique centralisée et réutilisable
     */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Invalide le token après utilisation.
     */
    public void invalidate() {
        this.used = true;
    }
}