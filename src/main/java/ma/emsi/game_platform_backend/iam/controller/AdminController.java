package ma.emsi.game_platform_backend.iam.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.billing.dto.SubscriptionDTO;
import ma.emsi.game_platform_backend.billing.model.Subscription;
import ma.emsi.game_platform_backend.billing.repository.PaymentTransactionRepository;
import ma.emsi.game_platform_backend.billing.repository.SubscriptionRepository;
import ma.emsi.game_platform_backend.billing.service.SubscriptionService;
import ma.emsi.game_platform_backend.game.dto.GameCreateRequest;
import ma.emsi.game_platform_backend.game.dto.GameDTO;
import ma.emsi.game_platform_backend.game.model.Category;
import ma.emsi.game_platform_backend.game.model.Game;
import ma.emsi.game_platform_backend.game.repository.CategoryRepository;
import ma.emsi.game_platform_backend.game.repository.GameCategoryRepository;
import ma.emsi.game_platform_backend.game.repository.GameRepository;
import ma.emsi.game_platform_backend.game.repository.ScoreRepository;
import ma.emsi.game_platform_backend.game.service.GameService;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.shared.enums.AccountStatus;
import ma.emsi.game_platform_backend.shared.enums.PaymentStatus;
import ma.emsi.game_platform_backend.shared.enums.Role;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur Admin – Gestion centralisée.
 * Tous les endpoints nécessitent le rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final ScoreRepository scoreRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final GameCategoryRepository gameCategoryRepository;

    // ================================================================
    //  1. GESTION DES UTILISATEURS
    // ================================================================

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getStatus() != AccountStatus.DELETED)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, String>> activateAccount(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));

        if (user.getStatus() == AccountStatus.DELETED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Impossible de réactiver un compte supprimé."));
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Compte de " + user.getPseudo() + " activé avec succès.",
                "userId", user.getId(),
                "status", AccountStatus.ACTIVE.name()
        ));
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendAccount(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Impossible de suspendre un compte ADMIN."));
        }
        if (user.getStatus() == AccountStatus.DELETED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ce compte est déjà supprimé."));
        }
        user.setStatus(AccountStatus.SUSPENDED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String reason = (body != null && body.containsKey("reason"))
                ? body.get("reason")
                : "Aucune raison spécifiée";

        return ResponseEntity.ok(Map.of(
                "message", "Compte de " + user.getPseudo() + " suspendu.",
                "userId", user.getId(),
                "status", AccountStatus.SUSPENDED.name(),
                "reason", reason
        ));
    }

    @PatchMapping("/users/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));

        if (user.getStatus() != AccountStatus.LOCKED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ce compte n'est pas verrouillé."));
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Compte de " + user.getPseudo() + " déverrouillé.",
                "userId", user.getId(),
                "status", AccountStatus.ACTIVE.name()
        ));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> changeRole(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));

        String newRoleStr = body.get("role");
        Role newRole;
        try {
            newRole = Role.valueOf(newRoleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rôle invalide : " + newRoleStr + ". Valeurs : USER, PREMIUM, ADMIN"));
        }
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Rôle de " + user.getPseudo() + " mis à jour.",
                "userId", user.getId(),
                "newRole", newRole.name()
        ));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteAccount(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + id));

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Impossible de supprimer un compte ADMIN."));
        }
        user.setStatus(AccountStatus.DELETED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Compte de " + user.getPseudo() + " supprimé (soft-delete).",
                "userId", user.getId()
        ));
    }

    // ================================================================
    //  2. GESTION DES JEUX
    // ================================================================

    @GetMapping("/games")
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @PostMapping(value = "/games/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameDTO> uploadGame(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("slug") String slug,
            @RequestParam("description") String description,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("isPremium") boolean isPremium,
            @RequestParam("multiplier") double multiplier,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl,
            @RequestParam(value = "categoryIds", required = false) List<String> categoryIds) {

        GameCreateRequest request = GameCreateRequest.builder()
                .title(title).slug(slug).description(description)
                .difficulty(difficulty).isPremium(isPremium).multiplier(multiplier)
                .thumbnailUrl(thumbnailUrl).categoryIds(categoryIds).build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createGameWithFile(request, file));
    }

    @PutMapping("/games/{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable String id,
                                              @Valid @RequestBody GameCreateRequest request) {
        return ResponseEntity.ok(gameService.updateGame(id, request));
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<Map<String, String>> deleteGame(@PathVariable String id) {
        gameService.deleteGame(id);
        return ResponseEntity.ok(Map.of("message", "Jeu supprimé (soft-delete)."));
    }

    @PatchMapping("/games/{id}/restore")
    public ResponseEntity<Map<String, String>> restoreGame(@PathVariable String id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Jeu non trouvé : " + id));
        game.setIsActive(true);
        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);
        return ResponseEntity.ok(Map.of("message", "Jeu restauré avec succès."));
    }

    // ================================================================
    //  3. GESTION DES ABONNEMENTS
    // ================================================================

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDTO>> getAllSubscriptions() {
        List<SubscriptionDTO> subs = subscriptionRepository.findAll().stream()
                .map(this::toSubscriptionDTO).collect(Collectors.toList());
        return ResponseEntity.ok(subs);
    }

    @PostMapping("/subscriptions/cancel/{userId}")
    public ResponseEntity<SubscriptionDTO> cancelUserSubscription(
            @PathVariable String userId,
            @RequestParam String reason) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId, reason));
    }

    @PostMapping("/subscriptions/revoke/{userId}")
    public ResponseEntity<Map<String, String>> revokeSubscription(@PathVariable String userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé pour : " + userId));
        sub.setStatus(SubscriptionStatus.EXPIRED);
        sub.setEndDate(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
        return ResponseEntity.ok(Map.of("message", "Abonnement révoqué immédiatement."));
    }

    // ================================================================
    //  4. STATISTIQUES
    // ================================================================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE).count();
        long totalGames = gameRepository.count();
        long totalScores = scoreRepository.count();
        long activeSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).count();
        double totalRevenue = paymentRepository.findAll().stream()
                .filter(tx -> tx.getStatus() == PaymentStatus.PAID)
                .mapToDouble(tx -> tx.getAmount()).sum();

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "totalGames", totalGames,
                "totalScores", totalScores,
                "activeSubscriptions", activeSubscriptions,
                "totalRevenue", totalRevenue
        ));
    }

    // ── Helper DTO ──────────────────────────────────────────────────

    private SubscriptionDTO toSubscriptionDTO(Subscription sub) {
        return SubscriptionDTO.builder()
                .id(sub.getId()).userId(sub.getUserId()).plan(sub.getPlan())
                .status(sub.getStatus()).startDate(sub.getStartDate()).endDate(sub.getEndDate())
                .daysRemaining(sub.getDaysRemaining())
                .autoRenew(sub.isAutoRenew())
                .active(sub.isActive())
                .cancelledAt(sub.getCancelledAt())
                .cancelReason(sub.getCancelReason())
                .build();
    }


// ================================================================
//  5. GESTION DES CATÉGORIES
// ================================================================

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        if (categoryRepository.existsBySlug(category.getSlug())) {
            return ResponseEntity.badRequest().build();
        }
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable String id,
                                                   @RequestBody Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        existing.setLabel(category.getLabel());
        existing.setSlug(category.getSlug());
        existing.setDescription(category.getDescription());
        existing.setIconUrl(category.getIconUrl());
        existing.setActive(category.isActive());
        categoryRepository.save(existing);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable String id) {
        // Supprime d’abord toutes les associations game_id ↔ category_id
        gameCategoryRepository.deleteByCategoryId(id);
        // Puis supprime la catégorie elle‑même
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Catégorie supprimée"));
    }
}