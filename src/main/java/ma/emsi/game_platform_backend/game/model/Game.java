package ma.emsi.game_platform_backend.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  DOCUMENT GAME - COMPARAISON SPRING DATA MONGODB vs MONGOCLIENT MANUEL
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : POJO + MONGOCLIENT MANUEL (BSON)                      │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * SANS SPRING DATA (Mapping manuel avec le driver MongoDB) :
 * ─────────────────────────────────────────────────────────
 *    Pour insérer un jeu sans Spring Data, vous devriez construire manuellement
 *    un objet BSON (Document) :
 *
 *    Document doc = new Document("_id", new ObjectId())
 *            .append("title", game.getTitle())
 *            .append("description", game.getDescription())
 *            .append("difficulty", game.getDifficulty())
 *            .append("isPremium", game.getIsPremium())
 *            .append("createdAt", LocalDateTime.now());
 *
 *    collection.insertOne(doc);
 *
 *    → TOTAL : Beaucoup de code répétitif (boilerplate)
 *    → Le mapping Objet Java ↔ BSON (Document MongoDB) doit être fait à la main.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING DATA : @Document + Lombok                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES SPRING DATA MONGODB + LOMBOK :
 * ───────────────────────────────────────
 * ✅ RÉDUCTION DU CODE : Le framework gère la conversion Java ↔ BSON.
 * ✅ MAPPING AUTOMATIQUE : Les annotations indiquent comment persister le document.
 * ✅ INDEXATION : Création automatique d'index via @Indexed.
 * ✅ AUDIT AUTOMATIQUE : @CreatedDate et @LastModifiedDate gérés automatiquement.
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Document : Marque la classe comme un document MongoDB persistable.
 *             C'est l'équivalent NoSQL de @Entity en JPA.
 *
 * @Document(collection = "games") : Spécifie le nom de la collection dans MongoDB.
 *                                   Si omis, Spring utilise le nom de la classe
 *                                   en camelCase (ex: "game").
 *
 * @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder :
 *    Annotations Lombok qui génèrent automatiquement les getters, setters,
 *    constructeurs et le pattern Builder à la compilation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "games")
public class Game {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * ID - CLÉ PRIMAIRE (Identifiant du Document)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Id : Mappe ce champ au champ `_id` natif de MongoDB.
     *
     * GÉNÉRATION AUTOMATIQUE :
     * ────────────────────────
     * Contrairement à SQL/JPA où il faut souvent préciser @GeneratedValue,
     * MongoDB génère automatiquement un identifiant unique (ObjectId) si le
     * champ est de type String ou ObjectId et qu'il est null au moment de l'insertion.
     *
     * Résultat en base (BSON) :
     * "_id" : ObjectId("65dfa2b..."),
     */
    @Id
    private String id;

    /**
     * @Indexed : Demande à MongoDB de créer un index sur ce champ.
     * unique = true : Garantit qu'aucun autre jeu ne peut avoir le même slug.
     * Très utile pour des requêtes rapides (ex: findBySlug).
     */
    @Indexed(unique = true)
    private String slug; // Sert d'identifiant pour le dossier physique /games/{slug}/

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * TITLE - CHAMP STANDARD
     * ════════════════════════════════════════════════════════════════════════════
     *
     * En MongoDB (qui est sans schéma ou "schema-less"), il n'y a pas besoin
     * de définir des tailles max (length = 255) ou des types (VARCHAR, TEXT)
     * avec @Column comme en JPA. Spring Data convertit simplement le String
     * Java en un String BSON.
     */
    @Indexed
    private String title;

    /**
     * Pas besoin de (columnDefinition = "TEXT") car MongoDB gère nativement
     * des chaînes de caractères de très grande taille dans ses documents BSON
     * (jusqu'à 16 MB pour l'ensemble du document).
     */
    private String description;

    /**
     * gameUrl est désormais géré par le système.
     * Il pointe vers /games/{slug}/index.html
     */
    private String gameUrl;

    private String thumbnailUrl;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * DIFFICULTY - CHAÎNE DE CARACTÈRES
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Si c'était un Enum, MongoDB stockerait par défaut la valeur sous forme
     * de String. Il n'y a pas besoin d'utiliser @Enumerated(EnumType.STRING)
     * comme en JPA/SQL. Ici, c'est déjà un simple String Java.
     */
    private String difficulty;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * MAPPING SPÉCIFIQUE AVEC @Field
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @Field("isPremium") : Force le nom de la clé dans le document BSON.
     * Par défaut, Spring utilise le nom de la variable, mais pour les booléens,
     * cela permet d'éviter les confusions avec les conventions de nommage Java (getters).
     */
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

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CREATED_AT - AUDIT AUTOMATIQUE (Date de création)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @CreatedDate : Annotation Spring Data pour l'auditing automatique.
     *                Remplit automatiquement le champ lors de la création.
     *
     * CONFIGURATION REQUISE :
     * ──────────────────────
     * Vous devez ajouter @EnableMongoAuditing dans votre classe principale
     * @SpringBootApplication ou dans une classe @Configuration.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * UPDATED_AT - AUDIT AUTOMATIQUE (Date de dernière modification)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @LastModifiedDate : Mise à jour automatique à chaque modification (save).
     *
     * Stocké nativement dans MongoDB sous le type BSON Date.
     */
    @LastModifiedDate
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