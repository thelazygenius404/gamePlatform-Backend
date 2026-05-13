package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  GAME REPOSITORY - QUERY DERIVATION & SOFT DELETE (MONGODB)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * Ce repository illustre toute la puissance de Spring Data MongoDB à travers
 * la "Query Derivation" (Dérivation de requêtes). Spring génère le code de la
 * requête BSON automatiquement à partir du nom de la méthode.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : LE SOFT DELETE (Suppression Logique)                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Dans cette application, un jeu n'est jamais physiquement supprimé de la base
 * de données (hard delete). À la place, son champ `isActive` passe à `false`.
 * C'est pourquoi toutes les méthodes de lecture ci-dessous incluent le suffixe
 * `AndIsActiveTrue` ou `ByIsActiveTrue`. Cela garantit que les jeux "supprimés"
 * n'apparaissent jamais dans les résultats de l'utilisateur.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : LES TYPES DE RETOUR OPTIMISÉS                               │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * • Optional<T> : Remplace les retours `null` pour éviter les NullPointerException.
 * • List<T>     : Retourne une liste vide au lieu de `null` si aucun résultat.
 * • boolean     : Plus performant qu'un find(). Ne ramène pas le document.
 * • long        : Utilise les fonctions d'agrégation natives de MongoDB.
 */
public interface GameRepository extends MongoRepository<Game, String> {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * RECHERCHE PAR SLUG UNIQUE (Jeu Actif Seulement)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.games.findOne({ "slug": "?0", "isActive": true })
     *
     * SANS SPRING DATA (MongoClient Manuel) :
     * ──────────────────────────────────────
     * Document doc = collection.find(Filters.and(
     *     Filters.eq("slug", slug),
     *     Filters.eq("isActive", true)
     * )).first();
     * return Optional.ofNullable(mapToGame(doc));
     *
     * @param slug Le slug unique du jeu (ex: "super-mario-bros")
     * @return Un Optional contenant le jeu s'il existe ET est actif.
     */
    Optional<Game> findBySlugAndIsActiveTrue(String slug);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * RECHERCHE PAR ID (Jeu Actif Seulement)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Pourquoi recréer un findById alors qu'il existe déjà dans MongoRepository ?
     * Car la méthode native `findById(id)` ramènerait même les jeux supprimés
     * (isActive = false). Cette méthode force le filtrage.
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.games.findOne({ "_id": ObjectId("?0"), "isActive": true })
     *
     * @param id L'identifiant unique MongoDB
     * @return Un Optional contenant le jeu s'il existe ET est actif.
     */
    Optional<Game> findByIdAndIsActiveTrue(String id);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LISTER TOUS LES JEUX ACTIFS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Remplace la méthode native `findAll()` pour exclure les jeux supprimés.
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.games.find({ "isActive": true })
     *
     * @return La liste complète des jeux non supprimés.
     */
    List<Game> findAllByIsActiveTrue();

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * VÉRIFICATION D'EXISTENCE (Ultra Rapide)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Contrairement à un `find()`, cette requête s'arrête de chercher dès qu'elle
     * trouve une correspondance et ne télécharge pas le document BSON complet depuis
     * la base de données. Très utile pour valider qu'un slug est disponible lors
     * de la création d'un jeu.
     *
     * TRADUCTION MONGODB GÉNÉRÉE (Équivalent conceptuel) :
     * db.games.find({ "slug": "?0" }, { _id: 1 }).limit(1).hasNext()
     *
     * @param slug Le slug à vérifier
     * @return true si le slug existe déjà, false sinon.
     */
    boolean existsBySlug(String slug);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * COMPTAGE DES JEUX (Agrégation Rapide)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Ne ramène aucun document. Demande simplement au moteur MongoDB de compter
     * les documents qui correspondent aux critères en utilisant ses métadonnées.
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.games.countDocuments({ "isActive": true })
     *
     * @return Le nombre total de jeux actuellement actifs sur la plateforme.
     */
    long countByIsActiveTrue();
}