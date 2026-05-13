
// ═══════════════════════════════════════════════════════
// GamificationStatsDTO.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GamificationStatsDTO {
    private String         userId;
    private String         pseudo;
    private int            points;
    private int            level;
    private String         levelLabel;
    private int            progressPercent;   // % vers prochain niveau
    private int            pointsToNextLevel; // points manquants
    private long           totalPlays;
    private List<BadgeDTO> badges;            // tous les badges (earned=true/false)
    private long           unreadNotifications;
}