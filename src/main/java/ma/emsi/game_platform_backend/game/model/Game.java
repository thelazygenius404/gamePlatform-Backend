package ma.emsi.game_platform_backend.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "games")
public class Game {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug; // Sert d'identifiant pour le dossier physique /games/{slug}/

    @Indexed
    private String title;

    private String description;

    /**
     * gameUrl est désormais géré par le système.
     * Il pointe vers /games/{slug}/index.html
     */
    private String gameUrl;

    private String thumbnailUrl;
    private String difficulty;

    @Builder.Default
    @Field("isPremium")
    @JsonProperty("isPremium")
    private Boolean isPremium = false;

    @Builder.Default
    @Field("isActive")
    @JsonProperty("isActive")
    private Boolean isActive = true;

    @Builder.Default
    private double multiplier = 1.0;

    @Builder.Default
    private int plays30d = 0;

    @Builder.Default
    private long totalPlays = 0L;

    @Builder.Default
    private double averageScore = 0.0;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Méthodes métier ────────────────────────────────────────────

    /**
     * Force l'URL vers le stockage local interne.
     * Cette méthode garantit qu'aucune URL externe ne subsiste.
     */
    public void generateInternalUrl() {
        if (this.slug != null && !this.slug.isBlank()) {
            this.gameUrl = "/games/" + this.slug + "/index.html";
        }
        this.updatedAt = LocalDateTime.now();
    }

    public int calculatePoints(int rawScore) {
        return (int) Math.round(rawScore * multiplier);
    }

    public void incrementPlays() {
        this.plays30d++;
        this.totalPlays++;
    }

    public void updateAverageScore(int newScore) {
        if (totalPlays <= 0) {
            this.averageScore = newScore;
            return;
        }
        double previousTotal = this.averageScore * (this.totalPlays - 1);
        this.averageScore = (previousTotal + newScore) / this.totalPlays;
    }

    public void softDelete() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAccessibleBy(boolean premiumAccess) {
        return !isPremium || premiumAccess;
    }
}