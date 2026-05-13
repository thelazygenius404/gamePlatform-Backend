package ma.emsi.game_platform_backend.game.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreDTO {
    private String id;
    private String userId;
    private String gameId;
    private String pseudo;
    private Integer rank;
    private int value;
    private int pointsEarned;
    private int duration;
    private LocalDateTime playedAt;
}