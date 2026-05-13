package ma.emsi.game_platform_backend.gamification.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.game_platform_backend.gamification.dto.BadgeDTO;
import ma.emsi.game_platform_backend.gamification.dto.GamificationStatsDTO;
import ma.emsi.game_platform_backend.gamification.model.Badge;
import ma.emsi.game_platform_backend.gamification.model.LevelConfig;
import ma.emsi.game_platform_backend.gamification.model.UserBadge;
import ma.emsi.game_platform_backend.gamification.repository.BadgeRepository;
import ma.emsi.game_platform_backend.gamification.repository.LevelConfigRepository;
import ma.emsi.game_platform_backend.gamification.repository.UserBadgeRepository;
import ma.emsi.game_platform_backend.gamification.service.GamificationService;
import ma.emsi.game_platform_backend.gamification.service.NotificationService;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.shared.enums.NotificationType;
import ma.emsi.game_platform_backend.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final UserRepository        userRepository;
    private final BadgeRepository       badgeRepository;
    private final UserBadgeRepository   userBadgeRepository;
    private final LevelConfigRepository levelConfigRepository;
    private final NotificationService   notificationService;
    private final MongoTemplate         mongoTemplate;

    // ─────────────────────────────────────────────────────────────
    // 1. Ajout de points + recalcul niveau
    // ─────────────────────────────────────────────────────────────

    @Override
    public void addPoints(String userId, int points) {
        if (points <= 0) return;

        // Incrément atomique MongoDB
        Query  q = Query.query(Criteria.where("_id").is(userId));
        Update u = new Update().inc("points", points);
        mongoTemplate.updateFirst(q, u, "users");

        // Récupère le nouveau total
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Calcule et met à jour le niveau si nécessaire
        LevelConfig config   = findLevelForPoints(user.getPoints());
        int         newLevel = config != null ? config.getLevel() : 1;

        if (newLevel > user.getLevel()) {
            mongoTemplate.updateFirst(q, new Update().set("level", newLevel), "users");
            log.info("[Gamification] Niveau {} atteint → userId={}", newLevel, userId);

            // Badge de récompense du niveau si configuré
            if (config.getRewardBadgeId() != null) {
                awardBadge(userId, config.getRewardBadgeId());
            }
        }

        // Évalue tous les badges
        evaluateBadges(userId);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Évaluation des badges — RG-15
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Badge> evaluateBadges(String userId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();

        // Badges déjà obtenus (pour ne pas réévaluer)
        Set<String> earnedIds = userBadgeRepository
                .findByUserIdOrderByAwardedAtDesc(userId)
                .stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        // Contexte d'évaluation
        long totalPlays = countPlays(userId);
        int  bestScore  = getBestScore(userId);
        int  userLevel  = user.getLevel();

        List<Badge> newlyAwarded = new ArrayList<>();

        for (Badge badge : badgeRepository.findByIsActiveTrue()) {
            if (earnedIds.contains(badge.getId())) continue; // RG-15

            boolean met = switch (badge.getType()) {
                case FIRST_GAME      -> totalPlays >= 1;
                case PLAYS_COUNT     -> totalPlays >= badge.getThreshold();
                case SCORE_MILESTONE -> bestScore  >= badge.getThreshold();
                case LEVEL_REACHED   -> userLevel  >= badge.getThreshold();
                case SPECIAL         -> false; // manuel uniquement
            };

            if (met) {
                awardBadge(userId, badge.getId());
                newlyAwarded.add(badge);
            }
        }

        return newlyAwarded;
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Stats complètes pour le profil
    // ─────────────────────────────────────────────────────────────

    @Override
    public GamificationStatsDTO getUserStats(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Map badgeId → awardedAt pour les badges obtenus
        Map<String, LocalDateTime> earnedMap = userBadgeRepository
                .findByUserIdOrderByAwardedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(
                        UserBadge::getBadgeId,
                        UserBadge::getAwardedAt
                ));

        // Tous les badges avec earned=true/false
        List<BadgeDTO> allBadges = badgeRepository.findByIsActiveTrue()
                .stream()
                .map(b -> BadgeDTO.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .description(b.getDescription())
                        .iconUrl(b.getIconUrl())
                        .type(b.getType())
                        .threshold(b.getThreshold())
                        .earned(earnedMap.containsKey(b.getId()))
                        .awardedAt(earnedMap.get(b.getId()))
                        .build())
                .toList();

        // Progression niveau
        LevelConfig current = findLevelForPoints(user.getPoints());
        LevelConfig next    = current != null
                ? levelConfigRepository
                  .findFirstByLevelGreaterThanOrderByLevelAsc(current.getLevel())
                  .orElse(null)
                : null;

        int progressPercent   = 100;
        int pointsToNextLevel = 0;

        if (current != null && next != null) {
            int range    = next.getMinPoints() - current.getMinPoints();
            int progress = user.getPoints()    - current.getMinPoints();
            progressPercent   = range > 0 ? (int) Math.min(100, (progress * 100.0) / range) : 100;
            pointsToNextLevel = Math.max(0, next.getMinPoints() - user.getPoints());
        }

        long totalPlays   = countPlays(userId);
        long unreadNotifs = mongoTemplate.count(
                Query.query(Criteria.where("userId").is(userId).and("isRead").is(false)),
                "notifications"
        );

        return GamificationStatsDTO.builder()
                .userId(user.getId())
                .pseudo(user.getPseudo())
                .points(user.getPoints())
                .level(user.getLevel())
                .levelLabel(current != null ? current.getLabel() : "Débutant")
                .progressPercent(progressPercent)
                .pointsToNextLevel(pointsToNextLevel)
                .totalPlays(totalPlays)
                .badges(allBadges)
                .unreadNotifications(unreadNotifs)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────

    private void awardBadge(String userId, String badgeId) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) return;

        userBadgeRepository.save(UserBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .awardedAt(LocalDateTime.now())
                .build());

        // RG-18 : notification automatique
        badgeRepository.findById(badgeId).ifPresent(badge ->
                notificationService.send(
                        userId,
                        NotificationType.BADGE_EARNED,
                        "🏅 Nouveau badge débloqué !",
                        "Vous avez obtenu le badge \"" + badge.getName() + "\"",
                        badgeId
                )
        );
    }

    private LevelConfig findLevelForPoints(int points) {
        return levelConfigRepository
                .findByMinPointsLessThanEqualAndMaxPointsGreaterThanEqual(points, points)
                .orElseGet(() ->
                        levelConfigRepository.findAll().stream()
                                .max(Comparator.comparingInt(LevelConfig::getLevel))
                                .orElse(null)
                );
    }

    /** Nombre de parties jouées par un user via MongoTemplate. */
    private long countPlays(String userId) {
        return mongoTemplate.count(
                Query.query(Criteria.where("userId").is(userId)),
                "scores"
        );
    }

    /** Meilleur score toutes parties confondues via MongoTemplate. */
    private int getBestScore(String userId) {
        Query q = Query.query(Criteria.where("userId").is(userId));
        q.with(Sort.by(Sort.Direction.DESC, "value")).limit(1);
        org.bson.Document doc = mongoTemplate.findOne(q, org.bson.Document.class, "scores");
        return doc != null ? doc.getInteger("value", 0) : 0;
    }
}