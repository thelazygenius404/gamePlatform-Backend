package ma.emsi.game_platform_backend.game.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameDTO {

    private String id;
    private String slug;
    private String title;
    private String description;
    private String gameUrl;
    private String thumbnailUrl;
    private String difficulty;

    @JsonProperty("isPremium")
    private Boolean isPremium;

    @JsonProperty("isActive")
    private Boolean isActive;

    private double multiplier;
    private int    plays30d;
    private long   totalPlays;
    private double averageScore;
    private String createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<String> categoryIds = List.of();
}