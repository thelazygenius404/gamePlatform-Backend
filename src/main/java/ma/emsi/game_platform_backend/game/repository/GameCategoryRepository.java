package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.GameCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  GAME CATEGORY REPOSITORY - COMPARAISON COMPLÈTE CRUD
 *  MONGOCLIENT MANUEL vs MONGOTEMPLATE vs SPRING DATA MONGODB
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE 1 : MONGOCLIENT MANUEL (Le Driver Natif) (~200+ lignes)            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * public class GameCategoryDaoManual {
 *
 *     private MongoClient mongoClient;
 *     private MongoCollection<Document> collection;
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // CREATE - INSERTION
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public GameCategory save(GameCategory category) {
 *         // Mapping manuel Objet Java -> Document BSON
 *         Document doc = new Document("_id", new ObjectId())
 *                 .append("gameId", category.getGameId())
 *                 .append("categoryId", category.getCategoryId());
 *
 *         collection.insertOne(doc);
 *         category.setId(doc.getObjectId("_id").toHexString());
 *         return category;
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // READ - FINDBYGAMEID
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public List<GameCategory> findByGameId(String gameId) {
 *         List<GameCategory> results = new ArrayList<>();
 *
 *         // Requête BSON manuelle: { "gameId": "123" }
 *         Bson query = Filters.eq("gameId", gameId);
 *
 *         try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
 *             while (cursor.hasNext()) {
 *                 Document doc = cursor.next();
 *
 *                 // Mapping manuel Document BSON -> Objet Java
 *                 GameCategory cat = new GameCategory();
 *                 cat.setId(doc.getObjectId("_id").toHexString());
 *                 cat.setGameId(doc.getString("gameId"));
 *                 cat.setCategoryId(doc.getString("categoryId"));
 *                 results.add(cat);
 *             }
 *         }
 *         return results;
 *     }
 * }
 *
 * → BILAN MONGOCLIENT MANUEL :
 *   • Beaucoup de code répétitif (boilerplate).
 *   • Conversion manuelle BSON ↔ Objet Java à chaque opération.
 *   • Requêtes écrites via l'API Filters (pas très lisible pour des requêtes complexes).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE 2 : MONGOTEMPLATE (L'abstraction Spring) (~100 lignes)             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * public class GameCategoryDaoTemplate {
 *
 *     @Autowired
 *     private MongoTemplate mongoTemplate;
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // CREATE / UPDATE
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public GameCategory save(GameCategory category) {
 *         // MongoTemplate gère automatiquement la conversion BSON <-> Java
 *         return mongoTemplate.save(category);
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // READ - FINDBYGAMEID
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public List<GameCategory> findByGameId(String gameId) {
 *         Query query = new Query();
 *         query.addCriteria(Criteria.where("gameId").is(gameId));
 *
 *         return mongoTemplate.find(query, GameCategory.class);
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // DELETE
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public void deleteByGameId(String gameId) {
 *         Query query = new Query(Criteria.where("gameId").is(gameId));
 *         mongoTemplate.remove(query, GameCategory.class);
 *     }
 * }
 *
 * → BILAN MONGOTEMPLATE :
 *   • Réduction massive du code comparé au MongoClient natif.
 *   • Le mapping BSON ↔ Java est géré automatiquement.
 *   • Très puissant pour les requêtes dynamiques ou les aggrégations complexes.
 *   • MAIS : Il faut toujours écrire manuellement chaque méthode du DAO.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE 3 : SPRING DATA MONGODB (MongoRepository) (~5 lignes)              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES SPRING DATA MONGODB :
 * ──────────────────────────────
 *
 * ✅ RÉDUCTION MASSIVE DU CODE : Plus d'implémentation de classe nécessaire.
 * ✅ MÉTHODES CRUD HÉRITÉES : save(), findById(), findAll(), deleteById(), etc.
 * ✅ QUERY DERIVATION : Spring génère la requête BSON juste en lisant le nom de
 *                       la méthode (ex: findByGameId).
 * ✅ PAGINATION & TRI NATIFS : Support de Pageable et Sort.
 * ✅ @QUERY BSON : Possibilité d'écrire des requêtes MongoDB natives en JSON
 *                  via l'annotation @Query("{ 'gameId': ?0 }").
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * MongoRepository<GameCategory, String> :
 *    • GameCategory : Type du Document
 *    • String : Type de la clé primaire (@Id)
 *
 *    Hérite de PagingAndSortingRepository et CrudRepository, offrant toutes les
 *    opérations de base sans écrire une seule ligne d'implémentation.
 */
public interface GameCategoryRepository extends MongoRepository<GameCategory, String> {

    /**
     * QUERY DERIVATION :
     * Spring analyse le nom "findByGameId", voit la propriété "gameId" dans
     * l'objet GameCategory, et génère automatiquement la requête MongoDB :
     * db.gameCategory.find({"gameId": <valeur>})
     */
    List<GameCategory> findByGameId(String gameId);

    /**
     * Supprime automatiquement tous les documents correspondant au gameId.
     * Requête générée : db.gameCategory.deleteMany({"gameId": <valeur>})
     */
    void deleteByGameId(String gameId);

    /**
     * Supprime automatiquement tous les documents correspondant au categoryId.
     * Requête générée : db.gameCategory.deleteMany({"categoryId": <valeur>})
     */
    void deleteByCategoryId(String categoryId);
}
/**
 * ════════════════════════════════════════════════════════════════════════════════
 * COMPARAISON FINALE CHIFFRÉE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────────────┬─────────────────┬────────────────┬──────────────────┐
 * │ CRITÈRE                  │ MONGOCLIENT     │ MONGOTEMPLATE  │ MONGO REPOSITORY │
 * ├──────────────────────────┼─────────────────┼────────────────┼──────────────────┤
 * │ Lignes de code (CRUD)    │ ~150-200        │ ~50            │ 0 (hérité)       │
 * ├──────────────────────────┼─────────────────┼────────────────┼──────────────────┤
 * │ Query personnalisée      │ Manuelle (Bson) │ Criteria API   │ 1 ligne (derive) │
 * ├──────────────────────────┼─────────────────┼────────────────┼──────────────────┤
 * │ Mapping objet <-> BSON   │ Manuel          │ Automatique    │ Automatique      │
 * ├──────────────────────────┼─────────────────┼────────────────┼──────────────────┤
 * │ Pagination & Tri         │ limit() / skip()│ Query.with()   │ Pageable natif   │
 * ├──────────────────────────┼─────────────────┼────────────────┼──────────────────┤
 * │ Aggrégations complexes   │ API Native      │ Excellent      │ Via @Aggregation │
 * └──────────────────────────┴─────────────────┴────────────────┴──────────────────┘
 *
 * RECOMMANDATION D'USAGE :
 * ───────────────────────
 * • MongoRepository : Pour 90% des cas (CRUD standard, requêtes simples).
 * • MongoTemplate : À injecter dans des services pour les 10% restants
 *                   (Mises à jour partielles complexes, pipelines d'aggrégation
 *                   très avancés).
 *
 * ════════════════════════════════════════════════════════════════════════════════
 */