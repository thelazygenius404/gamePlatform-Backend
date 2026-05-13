package ma.emsi.game_platform_backend.game.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "scores")
@CompoundIndexes({
        @CompoundIndex(name = "idx_leaderboard", def = "{'gameId': 1, 'value': -1}"),
        @CompoundIndex(name = "idx_user_game", def = "{'userId': 1, 'gameId': 1}")
})
public class Score {

    @Id
    private String id;

    private String userId;
    private String gameId;

    private int value;
    private int pointsEarned;
    private int duration;

    private LocalDateTime playedAt;

    public int calculatePointsEarned(double multiplier) {
        return (int) Math.round(value * multiplier);
    }
}