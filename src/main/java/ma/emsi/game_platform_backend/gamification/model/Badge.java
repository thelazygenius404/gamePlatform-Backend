package ma.emsi.game_platform_backend.gamification.model;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.BadgeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "badges")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Badge {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String iconUrl;

    private BadgeType type;

    /**
     * Seuil numérique déclenchant le badge.
     * Ex : PLAYS_COUNT threshold=10 → attribué après 10 parties
     */
    private int threshold;

    @Builder.Default
    private boolean isActive = true;
}