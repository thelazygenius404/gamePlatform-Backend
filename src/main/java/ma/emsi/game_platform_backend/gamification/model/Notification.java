package ma.emsi.game_platform_backend.gamification.model;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.NotificationType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * RG-18 : notification envoyée automatiquement à l'obtention d'un badge.
 */
@Document(collection = "notifications")
@CompoundIndex(name = "idx_notif_unread", def = "{'userId': 1, 'isRead': 1}")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    private String id;

    private String           userId;
    private NotificationType type;
    private String           title;
    private String           message;
    private String           relatedId;  // badgeId ou subscriptionId

    @Builder.Default
    private boolean isRead = false;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}