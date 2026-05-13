package ma.emsi.game_platform_backend.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  DOCUMENT CATEGORY - CONCEPTS CLÉS : INDEXATION & SÉRIALISATION MONGODB
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : L'INDEXATION POUR LES PERFORMANCES                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Par défaut, quand vous cherchez une catégorie par son "slug", MongoDB doit
 * lire TOUS les documents de la collection un par un jusqu'à trouver le bon.
 * C'est ce qu'on appelle un "Collection Scan" (O(N) en complexité). Sur une base
 * de données avec des millions d'entrées, c'est extrêmement lent.
 *
 * L'annotation @Indexed crée une structure de données séparée (souvent un B-Tree)
 * ordonnée alphabétiquement. La recherche devient quasi-instantanée (O(log N)).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : LES BOOLÉENS ET LE PIÈGE DU "IS_"                           │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * En Java, la convention pour les getters d'un booléen `isActive` est `isActive()`
 * (et non `getIsActive()`). Le problème, c'est que les librairies JSON comme
 * Jackson (utilisée par Spring Boot) retirent souvent le "is" lors de la
 * génération du JSON.
 *
 * Résultat sans annotations : `{"active": true}` au lieu de `{"isActive": true}`.
 * Vos annotations (@JsonProperty et @Field) règlent ce problème tant pour
 * l'API REST que pour le stockage en base.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "categories")
public class Category {

    /**
     * Identifiant unique généré par MongoDB (ObjectId converti en String).
     */
    @Id
    private String id;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LABEL - INDEX UNIQUE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Indexed(unique = true) demande à MongoDB de créer un index d'unicité.
     *
     * Rôle 1 (Performance) : Accélère les requêtes `findByLabel`.
     * Rôle 2 (Intégrité)   : MongoDB rejettera (via une MongoWriteException)
     *                        toute tentative d'insertion d'une catégorie ayant un
     *                        label qui existe déjà (ex: deux fois "Action").
     */
    @Indexed(unique = true)
    private String label;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * SLUG - L'IDENTIFIANT D'URL
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Le "slug" est la version optimisée pour les URLs du label
     * (ex: "Jeux de Rôle" devient "jeux-de-role").
     * Il sert souvent d'identifiant public dans vos routes frontend (/category/action)
     * pour le SEO (référencement). Il doit donc absolument être unique et indexé.
     */
    @Indexed(unique = true)
    private String slug;

    /**
     * Champs standards sans contraintes particulières.
     */
    private String description;

    private String iconUrl;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * IS_ACTIVE - DOUBLE SÉCURISATION DU NOMMAGE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Builder.Default : Permet à Lombok d'initialiser ce champ à 'true' par
     *                    défaut lors de l'utilisation du Category.builder().
     *
     * @Field("isActive") : Force Spring Data MongoDB à enregistrer la clé sous
     *                      le nom exact "isActive" dans le document BSON de
     *                      la base de données.
     *
     * @JsonProperty("isActive") : Force Jackson (le sérialiseur REST de Spring)
     *                             à renvoyer "isActive": true dans les réponses JSON
     *                             du Controller (au lieu de le tronquer en "active": true).
     */
    @Builder.Default
    @Field("isActive")
    @JsonProperty("isActive")
    private boolean isActive = true;
}