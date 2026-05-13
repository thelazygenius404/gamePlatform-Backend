package ma.emsi.game_platform_backend.game.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  DOCUMENT GAME_CATEGORY - RELATIONS MANY-TO-MANY & INDEX COMPOSÉS (MONGODB)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : MODÉLISATION MANY-TO-MANY (SQL vs NoSQL)                    │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * En SQL (JPA), on utiliserait une table de jointure générée via @ManyToMany.
 *
 * En MongoDB, vous avez choisi l'approche "Association Collection" (Collection
 * de liaison). C'est une excellente pratique lorsque la relation elle-même
 * possède des attributs (ici, la date d'assignation `assignedAt`).
 *
 * Alternative MongoDB (si pas d'attribut de liaison) :
 * On aurait pu simplement stocker une liste d'IDs dans l'entité Game :
 * `private List<String> categoryIds;`
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : L'INDEX COMPOSÉ (@CompoundIndex)                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * En SQL, on créerait une contrainte d'unicité sur deux colonnes :
 * `UNIQUE CONSTRAINT (game_id, category_id)`
 *
 * En MongoDB, on utilise un Index Composé (@CompoundIndex).
 *
 * def = "{'gameId': 1, 'categoryId': 1}" :
 * • Le "1" indique un tri ascendant dans l'index b-tree.
 * • unique = true : Empêche d'assigner DEUX FOIS la même catégorie au MÊME jeu.
 *   Exemple : Si (gameId="J1", categoryId="C1") existe, une nouvelle tentative
 *   d'insertion avec ("J1", "C1") lèvera une exception MongoWriteException.
 */
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

    /**
     * Identifiant unique de la relation généré par MongoDB.
     */
    @Id
    private String id;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CLÉS ÉTRANGÈRES "VIRTUELLES"
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Contrairement aux bases relationnelles (SQL), MongoDB n'applique pas de
     * contraintes de clés étrangères (Foreign Keys).
     * Si un `Game` ou une `Category` est supprimé(e), ce document continuera
     * d'exister (orphan document).
     *
     * C'est donc au niveau du Service ou du Repository (via des méthodes comme
     * `deleteByGameId`) qu'il faudra gérer la suppression en cascade.
     */
    private String gameId;

    private String categoryId;

    /**
     * Date à laquelle la catégorie a été attribuée au jeu.
     * Pratique pour trier les jeux par "Catégorie Récemment Ajoutée".
     */
    private LocalDateTime assignedAt;
}