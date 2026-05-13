package ma.emsi.game_platform_backend.game.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardDTO {
    private String gameId;
    private String gameTitle;
    private String pseudo;
    private List<ScoreDTO> topScores;
}