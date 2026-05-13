// ═══════════════════════════════════════════════════════
// NotificationDTO.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.dto;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.NotificationType;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private String           id;
    private NotificationType type;
    private String           title;
    private String           message;
    private String           relatedId;
    private boolean          isRead;
    private LocalDateTime    createdAt;
}
