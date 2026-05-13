package ma.emsi.game_platform_backend.game.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCreateRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "slug is required")
    private String slug;

    private String description;

    @NotBlank(message = "gameUrl is required")
    private String gameUrl;

    private String thumbnailUrl;

    private String difficulty;

    /**
     * Boolean (majuscule) — OBLIGATOIRE pour les 3 fichiers du pipeline :
     *
     *  1. GameCreateRequest  ← reçoit le JSON entrant   (ce fichier)
     *  2. Game               ← stocke en MongoDB
     *  3. GameDTO            ← retourne le JSON sortant
     *
     * Avec boolean (minuscule) :
     *   Lombok → setter setPremium()
     *   Jackson reçoit "isPremium" → cherche setIsPremium() → NOT FOUND → reste false ❌
     *
     * Avec Boolean (majuscule) :
     *   Lombok → setter setIsPremium()
     *   Jackson reçoit "isPremium" → setIsPremium(true) → OK ✅
     */
    @Builder.Default
    private Boolean isPremium = false;

    @DecimalMin(value = "0.1", message = "multiplier must be greater than 0")
    @Builder.Default
    private double multiplier = 1.0;

    private String createdBy;

    private List<String> categoryIds;
}