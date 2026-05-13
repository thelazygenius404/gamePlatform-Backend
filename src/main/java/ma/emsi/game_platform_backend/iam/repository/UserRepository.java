package ma.emsi.game_platform_backend.iam.repository;

import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.shared.enums.AccountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================
 *  Repository Spring Data MongoDB : User
 * ============================================================
 *
 * APPROCHE SANS Spring Data (DAO Manuel) :
 * ----------------------------------------
 * On créerait une classe UserDAO avec injection manuelle de MongoClient :
 *
 *   public class UserDAO {
 *     private MongoCollection<Document> collection;
 *
 *     public UserDAO(MongoDatabase db) {
 *       this.collection = db.getCollection("users");
 *     }
 *
 *     public Optional<User> findByEmail(String email) {
 *       Document doc = collection.find(Filters.eq("email", email)).first();
 *       if (doc == null) return Optional.empty();
 *       // Mapping manuel Document → User POJO
 *       User user = new User();
 *       user.setId(doc.getObjectId("_id").toString());
 *       user.setEmail(doc.getString("email"));
 *       // ... 20+ lignes de mapping
 *       return Optional.of(user);
 *     }
 *
 *     public void save(User user) {
 *       Document doc = new Document("email", user.getEmail())
 *                          .append("password", user.getPassword())
 *                          // ... chaque champ à la main
 *       collection.insertOne(doc);
 *     }
 *   }
 *
 * APPROCHE AVEC Spring Data MongoDB :
 * ------------------------------------
 * En étendant MongoRepository<User, String>, Spring génère AUTOMATIQUEMENT :
 *   - save(), findById(), findAll(), deleteById(), count(), existsById()...
 *   - Les méthodes personnalisées ci-dessous sont générées par parsing du nom
 *     de méthode (Query Method Derivation) :
 *     findByEmail → { "email": value }
 *     findByPseudo → { "pseudo": value }
 *     findByEmailAndStatusNot → { "email": value, "status": { "$ne": value } }
 *
 * AVANTAGES Spring Data :
 *   ✓ Zéro SQL/Bson manuel pour les requêtes simples
 *   ✓ Mapping POJO automatique via MappingMongoConverter
 *   ✓ Support Optional<T> natif
 *   ✓ Pagination via Pageable
 *   ✓ @Query pour les requêtes complexes en JSON MongoDB
 * ============================================================
 *
 * @Repository : active la traduction des exceptions MongoDB en DataAccessException Spring.
 * SANS Spring : on catcherait des MongoException manuellement partout.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Généré automatiquement par Spring Data depuis le nom de méthode.
     * Équivalent MongoDB : db.users.findOne({ email: email })
     * SANS Spring : requête Bson manuelle + mapping objet.
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByPseudo(String pseudo);

    /**
     * @Query : requête MongoDB en JSON pour cas complexes.
     * SANS Spring : Filters.and(Filters.eq("email", e), Filters.ne("status", "DELETED"))
     */
    @Query("{ 'email': ?0, 'status': { $ne: 'DELETED' } }")
    Optional<User> findActiveByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPseudo(String pseudo);
}
