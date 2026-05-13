package ma.emsi.game_platform_backend.gamification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Jonction User ↔ Badge.
 * RG-15 : index unique (userId + badgeId) → badge non duplicable par user.
 */
@Document(collection = "user_badges")
@CompoundIndex(
        name   = "idx_user_badge_unique",
        def    = "{'userId': 1, 'badgeId': 1}",
        unique = true
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserBadge {

    @Id
    private String id;

    private String userId;

    private String badgeId;

    private LocalDateTime awardedAt;
}