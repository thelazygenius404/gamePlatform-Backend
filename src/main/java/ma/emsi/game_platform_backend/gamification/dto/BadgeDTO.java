// ═══════════════════════════════════════════════════════
// BadgeDTO.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.dto;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.BadgeType;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BadgeDTO {
    private String        id;
    private String        name;
    private String        description;
    private String        iconUrl;
    private BadgeType     type;
    private int           threshold;
    private LocalDateTime awardedAt;  // null si pas encore obtenu
    private boolean       earned;
}
