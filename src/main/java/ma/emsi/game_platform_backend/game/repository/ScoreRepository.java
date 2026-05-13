package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Score;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  SCORE REPOSITORY - LIMITES, TRIS & LEADERBOARDS (MONGODB)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * Ce repository met en lumière la façon dont Spring Data gère la limitation
 * des résultats (LIMIT) et le tri (SORT) uniquement grâce aux mots-clés
 * "Top", "First" et "OrderBy" dans le nom des méthodes.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ : LE MOT-CLÉ "TOP" ou "FIRST"                                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * En MongoDB natif, pour obtenir un classement, vous devez utiliser les
 * curseurs avec `.sort()` et `.limit()`. Spring Data le fait automatiquement
 * en lisant les préfixes `findTop<N>` ou `findFirst<N>`.
 */
public interface ScoreRepository extends MongoRepository<Score, String> {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LE LEADERBOARD GLOBAL (Top 10)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * • Top10 : Applique un .limit(10) au curseur MongoDB.
     * • ByGameId : Filtre sur le jeu en question.
     * • OrderByValueDesc : Applique un tri descendant (.sort({ value: -1 }))
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.scores.find({ "gameId": "?0" }).sort({ "value": -1 }).limit(10)
     *
     * SANS SPRING DATA (MongoClient Manuel) :
     * ──────────────────────────────────────
     * collection.find(Filters.eq("gameId", gameId))
     *           .sort(Sorts.descending("value"))
     *           .limit(10)
     *           .into(new ArrayList<>());
     *
     * @param gameId L'identifiant du jeu
     * @return Les 10 meilleurs scores de tous les temps pour ce jeu.
     */
    List<Score> findTop10ByGameIdOrderByValueDesc(String gameId);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * LE RECORD PERSONNEL (Personal Best)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * • Top : Sans chiffre derrière, cela équivaut à `Top1` (ou limit(1)).
     * • ByUserIdAndGameId : Combine deux filtres (AND logique).
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.scores.find({ "userId": "?0", "gameId": "?1" })
     *          .sort({ "value": -1 })
     *          .limit(1)
     *
     * @param userId L'identifiant du joueur
     * @param gameId L'identifiant du jeu
     * @return Un Optional contenant le meilleur score absolu de ce joueur sur ce jeu.
     */
    Optional<Score> findTopByUserIdAndGameIdOrderByValueDesc(String userId, String gameId);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * HISTORIQUE COMPLET TRIÉ
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Contrairement aux méthodes ci-dessus, il n'y a pas de mot-clé "Top".
     * Cette méthode ramènera TOUS les scores d'un jeu, du plus grand au plus petit.
     *
     * ⚠️ ATTENTION / BONNE PRATIQUE :
     * Si un jeu possède 500,000 scores, cette méthode va charger 500,000
     * documents en mémoire (RAM) et risque de faire crasher l'application (OutOfMemory).
     *
     * Pour de gros volumes de données, il serait préférable d'utiliser la pagination :
     * Page<Score> findByGameId(String gameId, Pageable pageable);
     *
     * TRADUCTION MONGODB GÉNÉRÉE :
     * db.scores.find({ "gameId": "?0" }).sort({ "value": -1 })
     *
     * @param gameId L'identifiant du jeu
     * @return La liste complète de TOUS les scores pour ce jeu.
     */
    List<Score> findByGameIdOrderByValueDesc(String gameId);
}