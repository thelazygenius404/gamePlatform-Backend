package ma.emsi.game_platform_backend.game.service;

import ma.emsi.game_platform_backend.game.dto.GameCreateRequest;
import ma.emsi.game_platform_backend.game.dto.GameDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  GAME SERVICE - COMPARAISON APPROCHES LOGIQUE MÉTIER (NOSQL)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : Service + Driver MongoDB Natif (MongoClient)           │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * public class GameService {
 *
 *     private MongoCollection<Document> collection;
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // CREATE
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public GameDTO create(GameCreateRequest request) {
 *         // Validation manuelle
 *         if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
 *             throw new IllegalArgumentException("Le titre est obligatoire");
 *         }
 *
 *         // Construction manuelle du Document BSON
 *         Document doc = new Document()
 *             .append("title", request.getTitle())
 *             .append("description", request.getDescription())
 *             .append("createdAt", LocalDateTime.now())
 *             .append("updatedAt", LocalDateTime.now());
 *
 *         // Appel BDD natif
 *         collection.insertOne(doc);
 *
 *         // Mapping manuel de la réponse vers DTO
 *         return mapDocumentToDTO(doc);
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // READ - FINDALL avec PAGINATION MANUELLE
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public List<GameDTO> findAll(int page, int size) {
 *         int offset = page * size;
 *         List<GameDTO> games = new ArrayList<>();
 *
 *         // Curseur natif avec skip() et limit() manuels
 *         try (MongoCursor<Document> cursor = collection.find()
 *                                               .skip(offset)
 *                                               .limit(size)
 *                                               .iterator()) {
 *             while (cursor.hasNext()) {
 *                 // Mapping manuel Document -> DTO répété partout
 *                 games.add(mapDocumentToDTO(cursor.next()));
 *             }
 *         }
 *         return games;
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // UPDATE
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public GameDTO update(String id, GameCreateRequest request) {
 *         ObjectId objectId = new ObjectId(id);
 *
 *         // Document de mise à jour (Syntaxe $set de MongoDB)
 *         Document updateFields = new Document()
 *             .append("title", request.getTitle())
 *             .append("updatedAt", LocalDateTime.now()); // Timestamps manuels
 *
 *         Document update = new Document("$set", updateFields);
 *
 *         Document updatedDoc = collection.findOneAndUpdate(
 *             Filters.eq("_id", objectId),
 *             update,
 *             new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
 *         );
 *
 *         if (updatedDoc == null) {
 *             throw new RuntimeException("Jeu non trouvé : " + id);
 *         }
 *
 *         return mapDocumentToDTO(updatedDoc);
 *     }
 *
 *     // ═══════════════════════════════════════════════════════════════════════
 *     // SEARCH (Recherche textuelle ou Regex)
 *     // ═══════════════════════════════════════════════════════════════════════
 *
 *     public List<GameDTO> searchGames(String keyword) {
 *         List<GameDTO> games = new ArrayList<>();
 *
 *         // Filtre Regex natif (équivalent du LIKE '%keyword%')
 *         Bson filter = Filters.regex("title", ".*" + keyword + ".*", "i");
 *
 *         try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
 *             while (cursor.hasNext()) {
 *                 games.add(mapDocumentToDTO(cursor.next()));
 *             }
 *         }
 *         return games;
 *     }
 * }
 *
 * → BILAN SERVICE MONGOCLIENT NATIF :
 *   • Code verbeux pour des opérations simples.
 *   • Syntaxe BSON ("$set", Filters) omniprésente dans la couche métier.
 *   • Gestion manuelle des curseurs (MongoCursor) et des fermetures (try-with-resources).
 *   • Pagination manuelle complexe (calcul des offsets).
 *   • Obligation de convertir les String en ObjectId manuellement.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE SPRING DATA MONGODB (Implémentation de cette interface)            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * AVANTAGES :
 * ──────────
 * ✅ CODE MINIMAL : Délégation des opérations complexes au MongoRepository.
 * ✅ SÉPARATION DES PRÉOCCUPATIONS : La couche métier manipule des Objets Java
 *                                    purs, sans jamais voir de syntaxe BSON.
 * ✅ PAGINATION NATIVE : Utilisation de l'objet Pageable de Spring au lieu des
 *                        calculs .skip() et .limit().
 * ✅ TRANSACTIONS : Support de @Transactional (si Replica Set MongoDB configuré).
 * ✅ EXCEPTIONS SPRING : Unification des erreurs via DataAccessException.
 * ✅ MAPPING AUTOMATIQUE : Utilisation de MapStruct ou ModelMapper pour passer
 *                          de l'Entité (Game) au DTO (GameDTO) en une ligne.
 *
 * @Service : Marque la classe d'implémentation comme un bean métier.
 *
 * @RequiredArgsConstructor : Génère le constructeur avec dépendances (Repository,
 *                            Mapper). Spring injecte automatiquement.
 */
public interface GameService {
    GameDTO createGame(GameCreateRequest request);
    GameDTO createGameWithFile(GameCreateRequest request, MultipartFile file);
    GameDTO updateGame(String id, GameCreateRequest request);
    void deleteGame(String id);
    List<GameDTO> getAllGames();
    GameDTO getGameById(String id);
    GameDTO getGameBySlug(String slug);
}