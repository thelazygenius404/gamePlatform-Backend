package ma.emsi.game_platform_backend.gamification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * RG-17 : paliers de niveaux configurables en base.
 *
 * Seed dans DataInitializer :
 *   Nv1 :    0 –  99  → "Débutant"
 *   Nv2 :  100 – 299  → "Apprenti"
 *   Nv3 :  300 – 599  → "Confirmé"
 *   Nv4 :  600 – 999  → "Expert"
 *   Nv5 : 1000+       → "Légende"
 */
@Document(collection = "level_configs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LevelConfig {

    @Id
    private String id;

    @Indexed(unique = true)
    private int level;

    private int    minPoints;
    private int    maxPoints;
    private String label;
    private String rewardBadgeId; // nullable — badge offert en atteignant ce niveau
}