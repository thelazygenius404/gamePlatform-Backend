package ma.emsi.game_platform_backend.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    @Indexed(unique = true)
    private String label;

    @Indexed(unique = true)
    private String slug;

    private String description;
    private String iconUrl;

    @Builder.Default
    @Field("isActive")
    @JsonProperty("isActive")
    private boolean isActive = true;
}