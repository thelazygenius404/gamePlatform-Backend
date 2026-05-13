package ma.emsi.game_platform_backend.iam.serviceImpl;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.model.*;
import ma.emsi.game_platform_backend.shared.enums.*;
import ma.emsi.game_platform_backend.iam.dto.*;
import ma.emsi.game_platform_backend.iam.repository.*;
import ma.emsi.game_platform_backend.iam.security.JwtUtil;
import ma.emsi.game_platform_backend.iam.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ============================================================
 *  Implémentation du service d'authentification
 * ============================================================
 *
 * @Service : bean Spring géré par l'IoC Container.
 * SANS Spring : instanciation manuelle new AuthServiceImpl(new UserDAO(), ...)
 *   → couplage fort, difficile à tester, difficile à maintenir.
 * AVEC Spring : @Autowired automatique via constructeur (@RequiredArgsConstructor),
 *   IoC Container gère le cycle de vie et les dépendances.
 *
 * PATTERN : Service Layer (couche service entre contrôleur et repository).
 * SANS Spring : logique métier dispersée dans les Servlets ou les DAOs.
 * AVEC Spring : séparation claire des responsabilités (SRP — Single Responsibility).
 * ============================================================
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * SANS Spring :
     *   - Validation manuelle de chaque champ
     *   - UserDAO.findByEmail() avec requête Bson manuelle
     *   - BCrypt.hashpw() appelé directement
     *   - UserDAO.insert() avec mapping Document manuel
     *
     * AVEC Spring :
     *   - @Valid dans le contrôleur gère la validation automatiquement
     *   - UserRepository.existsByEmail() généré par Spring Data
     *   - passwordEncoder.encode() injecté par Spring Security
     *   - userRepository.save() persiste l'objet Java directement
     */
    @Override
    public AuthResponse register(RegisterRequest request) {

        // Vérification de l'unicité de l'email (RG-01)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        // Vérification de l'unicité du pseudo
        if (userRepository.existsByPseudo(request.getPseudo())) {
            throw new IllegalArgumentException("Ce pseudo est déjà pris.");
        }

        // Construction du document User avec le pattern Builder (Lombok)
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                /*
                 * Hachage BCrypt.
                 * SANS Spring : String hash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt(12));
                 * AVEC Spring : passwordEncoder.encode() → délégué au bean BCryptPasswordEncoder.
                 * Le salt est inclus dans le hash → pas besoin de le stocker séparément.
                 */
                .password(passwordEncoder.encode(request.getPassword()))
                .pseudo(request.getPseudo())
                .role(Role.USER)                         // Rôle par défaut
                .status(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .points(0)
                .level(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        /*
         * Spring Data MongoDB : save() insère ou met à jour selon la présence de l'@Id.
         * SANS Spring : collection.insertOne(documentManuel) après mapping champ par champ.
         */
        User savedUser = userRepository.save(user);

        // Génération du JWT pour connexion directe après inscription
        String token = generateAndPersistToken(savedUser, null, null);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .pseudo(savedUser.getPseudo())
                .role(savedUser.getRole())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * Connexion utilisateur avec gestion du système de verrouillage (RG-03).
     *
     * SANS Spring :
     *   - Récupération manuelle depuis MongoDB avec Bson Filters
     *   - Vérification manuelle BCrypt.checkpw()
     *   - Gestion des tentatives : UPDATE manuel en base
     *   - Pas d'intégration avec un AuthenticationManager
     *
     * AVEC Spring Security :
     *   - authenticationManager.authenticate() délègue au DaoAuthenticationProvider
     *   - qui appelle loadUserByUsername() → findByEmail()
     *   - puis passwordEncoder.matches() pour vérification BCrypt
     *   - Lance des exceptions typées : BadCredentialsException, LockedException, DisabledException
     */
    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        // RG-03 : Vérification du verrouillage avant tentative
        // La méthode isLocked() déverrouille automatiquement si le délai est écoulé
        if (user.isLocked()) {
            throw new LockedException("Compte verrouillé jusqu'à : " + user.getLockedUntil());
        }

        // RG-20 : Compte suspendu par admin
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new DisabledException("Votre compte a été suspendu. Contactez l'administrateur.");
        }

        try {
            /*
             * authenticationManager.authenticate() :
             * SANS Spring : comparaison BCrypt manuelle + vérification statut manuelle
             * AVEC Spring : délègue au DaoAuthenticationProvider qui gère tout automatiquement
             * Lance BadCredentialsException si échec.
             */
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Authentification réussie → réinitialisation des tentatives échouées
            user.resetFailures();
            userRepository.save(user);

        } catch (BadCredentialsException e) {
            // RG-03 : incrément du compteur d'échecs avec verrouillage si seuil atteint
            user.incrementFailures(maxFailedAttempts, lockDurationMinutes);
            userRepository.save(user);

            if (user.isLocked()) {
                throw new LockedException(
                        "Trop de tentatives échouées. Compte verrouillé pour " + lockDurationMinutes + " minutes."
                );
            }
            userRepository.save(user);


            int remaining = maxFailedAttempts - user.getFailedAttempts();
            throw new BadCredentialsException(
                    "Email ou mot de passe incorrect. " + remaining + " tentative(s) restante(s)."
            );
        }

        String token = generateAndPersistToken(user, ipAddress, userAgent);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .pseudo(user.getPseudo())
                .role(user.getRole())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * Déconnexion : révocation du token JWT (ajout à la blacklist).
     *
     * SANS Spring : suppression de la session côté serveur (session.invalidate()).
     * AVEC JWT : on enregistre le token comme révoqué. Le JwtAuthFilter vérifie
     * cette blacklist à chaque requête.
     */
    @Override
    public void logout(String token) {
        sessionRepository.findByJwtToken(token)
                .ifPresent(session -> {
                    session.revoke();
                    sessionRepository.save(session);
                });
    }

    /**
     * Demande de réinitialisation de mot de passe.
     * Génère un token UUID valide 1h (RG-04) et l'enregistre en MongoDB.
     * En production : envoyer un email avec le lien de reset.
     */
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            resetTokenRepository.deleteByUserId(user.getId());
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId()).createdAt(LocalDateTime.now()).build();
            resetTokenRepository.save(resetToken);
            System.out.println("[DEV] Token de reset : " + resetToken.getToken());
        });
        // Pas d'exception → toujours 200, même si email inconnu
    }

    /**
     * Réinitialisation du mot de passe avec le token reçu par email.
     */
    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou expiré."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Token expiré.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.invalidate();
        resetTokenRepository.save(resetToken);
    }

    // ── Méthode privée utilitaire ────────────────────────────────────────

    private String generateAndPersistToken(User user, String ipAddress, String userAgent) {
        String token = jwtUtil.generateToken(
                user.getEmail(),    // subject = email (pour loadUserByUsername)
                Map.of(
                        "role", user.getRole().name(),
                        "pseudo", user.getPseudo(),
                        "userId", user.getId()
                )
        );

        // Persistance de la session pour la gestion de la blacklist (révocation)
        Session session = Session.builder()
                .userId(user.getId())
                .jwtToken(token)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000))
                .isRevoked(false)
                .build();

        sessionRepository.save(session);
        return token;
    }
}