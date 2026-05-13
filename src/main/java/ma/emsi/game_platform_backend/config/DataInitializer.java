package ma.emsi.game_platform_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.game_platform_backend.game.model.Category;
import ma.emsi.game_platform_backend.game.repository.CategoryRepository;
import ma.emsi.game_platform_backend.gamification.model.Badge;
import ma.emsi.game_platform_backend.gamification.model.LevelConfig;
import ma.emsi.game_platform_backend.gamification.repository.BadgeRepository;
import ma.emsi.game_platform_backend.gamification.repository.LevelConfigRepository;
import ma.emsi.game_platform_backend.shared.enums.BadgeType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository    categoryRepository;
    private final LevelConfigRepository levelConfigRepository;
    private final BadgeRepository       badgeRepository;

    @Override
    public void run(String... args) {
        seedLevelConfigs();
        seedBadges();
    }

    private void seedLevelConfigs() {
        if (levelConfigRepository.count() > 0) return;

        // Table de correspondance pour les 20 premiers niveaux (libellés personnalisés)
        String[] labels = {
                "Débutant", "Apprenti", "Confirmé", "Expert", "Légende",
                "Vétéran", "Maître", "Champion", "Immortel", "Divinité",
                "Sage", "Guerrier", "Gladiateur", "Empereur", "Phénix",
                "Titan", "Colosse", "Dieu", "Dragon", "Légende Vivante"
        };

        List<LevelConfig> levels = new ArrayList<>();

        // Génère les 100 premiers niveaux de manière progressive
        for (int lvl = 1; lvl <= 100; lvl++) {
            // Calcul des points seuils : exponentiel léger pour éviter des écarts trop grands
            int minPoints = 0;
            int maxPoints = 0;
            if (lvl == 1) {
                minPoints = 0;
                maxPoints = 99;
            } else {
                // Seuil = 100 * (lvl - 1) (on peut ajuster plus tard)
                minPoints = 100 * (lvl - 1);
                maxPoints = 100 * lvl - 1;
            }
            // Dernier niveau : max = Integer.MAX_VALUE pour ne jamais le dépasser
            if (lvl == 100) {
                maxPoints = Integer.MAX_VALUE;
            }

            String label;
            if (lvl <= labels.length) {
                label = labels[lvl - 1];
            } else {
                label = "Niveau " + lvl;
            }

            LevelConfig config = LevelConfig.builder()
                    .level(lvl)
                    .minPoints(minPoints)
                    .maxPoints(maxPoints)
                    .label(label)
                    .rewardBadgeId(null)
                    .build();
            levels.add(config);
        }

        levelConfigRepository.saveAll(levels);
        log.info("[Init] {} niveaux insérés.", levels.size());
    }

    private void seedBadges() {
        if (badgeRepository.count() > 0) return;

        List<Badge> badges = new ArrayList<>();

        // Badge première partie
        badges.add(Badge.builder()
                .name("Premier Pas")
                .description("Jouer sa première partie")
                .iconUrl("/badges/first.svg")
                .type(BadgeType.FIRST_GAME)
                .threshold(1)
                .isActive(true)
                .build());

        // Badges de nombre de parties
        badges.add(Badge.builder()
                .name("Joueur Assidu")
                .description("Jouer 10 parties")
                .iconUrl("/badges/plays10.svg")
                .type(BadgeType.PLAYS_COUNT)
                .threshold(10)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Vétéran")
                .description("Jouer 50 parties")
                .iconUrl("/badges/veteran.svg")
                .type(BadgeType.PLAYS_COUNT)
                .threshold(50)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Centurion")
                .description("Jouer 100 parties")
                .iconUrl("/badges/centurion.svg")
                .type(BadgeType.PLAYS_COUNT)
                .threshold(100)
                .isActive(true)
                .build());

        // Badges de score
        badges.add(Badge.builder()
                .name("Score 500")
                .description("Atteindre 500 points en une partie")
                .iconUrl("/badges/score500.svg")
                .type(BadgeType.SCORE_MILESTONE)
                .threshold(500)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Maître du Score")
                .description("Atteindre 1000 points en une partie")
                .iconUrl("/badges/score1000.svg")
                .type(BadgeType.SCORE_MILESTONE)
                .threshold(1000)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Demi-Dieu")
                .description("Score de 5000 points")
                .iconUrl("/badges/score5000.svg")
                .type(BadgeType.SCORE_MILESTONE)
                .threshold(5000)
                .isActive(true)
                .build());

        // Badges de niveau atteint
        badges.add(Badge.builder()
                .name("Champion")
                .description("Atteindre le niveau 5")
                .iconUrl("/badges/champ.svg")
                .type(BadgeType.LEVEL_REACHED)
                .threshold(5)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Immortel")
                .description("Niveau 20")
                .iconUrl("/badges/immortal.svg")
                .type(BadgeType.LEVEL_REACHED)
                .threshold(20)
                .isActive(true)
                .build());
        badges.add(Badge.builder()
                .name("Dieu du Jeu")
                .description("Niveau 50")
                .iconUrl("/badges/god.svg")
                .type(BadgeType.LEVEL_REACHED)
                .threshold(50)
                .isActive(true)
                .build());

        // Badge spécial (ex: compléter 3 badges)
        badges.add(Badge.builder()
                .name("Collectionneur")
                .description("Débloquer 5 badges")
                .iconUrl("/badges/collector.svg")
                .type(BadgeType.SPECIAL)
                .threshold(5)   // nombre de badges débloqués requis
                .isActive(true)
                .build());

        badgeRepository.saveAll(badges);
        log.info("[Init] {} badges insérés.", badges.size());
    }
}