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
@Document(collection = "game_categories")
@CompoundIndexes({
        @CompoundIndex(name = "idx_game_category_unique", def = "{'gameId': 1, 'categoryId': 1}", unique = true)
})
public class GameCategory {

    @Id
    private String id;

    private String gameId;
    private String categoryId;

    private LocalDateTime assignedAt;
}