// ═══════════════════════════════════════════════════════
// GamificationService.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.service;

import ma.emsi.game_platform_backend.gamification.dto.GamificationStatsDTO;
import ma.emsi.game_platform_backend.gamification.model.Badge;

import java.util.List;

public interface GamificationService {

    /**
     * Ajoute des points à l'utilisateur et recalcule son niveau.
     * Appelé après chaque soumission de score.
     */
    void addPoints(String userId, int points);

    /**
     * Évalue tous les badges actifs pour un utilisateur.
     * Attribue les badges dont les conditions sont remplies.
     * RG-15 : un badge ne peut être obtenu qu'une seule fois.
     * @return liste des nouveaux badges obtenus
     */
    List<Badge> evaluateBadges(String userId);

    /**
     * Retourne les stats complètes de gamification pour le profil.
     */
    GamificationStatsDTO getUserStats(String userId);
}
