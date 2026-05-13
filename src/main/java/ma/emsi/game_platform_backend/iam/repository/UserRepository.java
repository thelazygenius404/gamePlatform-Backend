package ma.emsi.game_platform_backend.iam.repository;

import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.shared.enums.AccountStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  USER REPOSITORY - COMPARAISON SPRING DATA vs DAO MANUEL
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE CLASSIQUE : DAO MANUEL (Data Access Object)                       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CONFIGURATION MONGODB (Singleton Connection) :
 *    ───────────────────────────────────────────────
 *    public class MongoDBConnection {
 *        private static MongoClient mongoClient;
 *        private static MongoDatabase database;
 *
 *        static {
 *            try {
 *                // Lecture du fichier de configuration
 *                Properties props = new Properties();
 *                InputStream is = MongoDBConnection.class
 *                    .getResourceAsStream("/config.properties");
 *                props.load(is);
 *
 *                String host = props.getProperty("mongo.host");
 *                int port = Integer.parseInt(props.getProperty("mongo.port"));
 *                String dbName = props.getProperty("mongo.database");
 *                String username = props.getProperty("mongo.username");
 *                String password = props.getProperty("mongo.password");
 *
 *                // Construction de la chaîne de connexion
 *                String connectionString = String.format(
 *                    "mongodb://%s:%s@%s:%d/%s",
 *                    username, password, host, port, dbName
 *                );
 *
 *                // Initialisation du client
 *                MongoClientSettings settings = MongoClientSettings.builder()
 *                    .applyConnectionString(new ConnectionString(connectionString))
 *                    .applyToConnectionPoolSettings(builder ->
 *                        builder.maxSize(100)
 *                               .minSize(10)
 *                               .maxWaitTime(2, TimeUnit.SECONDS))
 *                    .build();
 *
 *                mongoClient = MongoClients.create(settings);
 *                database = mongoClient.getDatabase(dbName);
 *
 *            } catch (Exception e) {
 *                throw new RuntimeException("Erreur de connexion MongoDB", e);
 *            }
 *        }
 *
 *        public static MongoDatabase getDatabase() {
 *            return database;
 *        }
 *
 *        public static void close() {
 *            if (mongoClient != null) {
 *                mongoClient.close();
 *            }
 *        }
 *    }
 *
 *    → PROBLÈMES :
 *      • Configuration hardcodée ou lecture manuelle de fichier properties
 *      • Gestion manuelle du pool de connexions
 *      • Fermeture manuelle nécessaire (risque d'oubli)
 *      • Pas de health check automatique
 *      • Difficile à tester (dépendance à un MongoDB réel)
 *
 * 2. IMPLÉMENTATION DU DAO MANUEL (UserDAO.java) :
 *    ──────────────────────────────────────────────────
 *    public class UserDAO {
 *        private final MongoCollection<Document> collection;
 *
 *        public UserDAO() {
 *            this.collection = MongoDBConnection.getDatabase()
 *                                               .getCollection("users");
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // FINDBYEMAIL : ~30 lignes de code
 *        // ══════════════════════════════════════════════════════════════════════
 *        public Optional<User> findByEmail(String email) {
 *            try {
 *                // Construction du filtre MongoDB
 *                Bson filter = Filters.eq("email", email);
 *
 *                // Exécution de la requête
 *                Document doc = collection.find(filter).first();
 *
 *                if (doc == null) {
 *                    return Optional.empty();
 *                }
 *
 *                // MAPPING MANUEL Document → User (15+ lignes)
 *                User user = new User();
 *                user.setId(doc.getObjectId("_id").toString());
 *                user.setEmail(doc.getString("email"));
 *                user.setPassword(doc.getString("password"));
 *                user.setPseudo(doc.getString("pseudo"));
 *
 *                // Conversion String → Enum avec gestion null
 *                String roleStr = doc.getString("role");
 *                user.setRole(roleStr != null ? Role.valueOf(roleStr) : Role.USER);
 *
 *                String statusStr = doc.getString("status");
 *                user.setStatus(statusStr != null ? AccountStatus.valueOf(statusStr) : AccountStatus.ACTIVE);
 *
 *                user.setFailedAttempts(doc.getInteger("failedAttempts", 0));
 *
 *                // Conversion Date → LocalDateTime avec gestion null
 *                Object lockedUntilObj = doc.get("lockedUntil");
 *                if (lockedUntilObj instanceof Date) {
 *                    Date date = (Date) lockedUntilObj;
 *                    user.setLockedUntil(date.toInstant()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toLocalDateTime());
 *                }
 *
 *                user.setPoints(doc.getInteger("points", 0));
 *                user.setLevel(doc.getInteger("level", 1));
 *
 *                Object createdAtObj = doc.get("createdAt");
 *                if (createdAtObj instanceof Date) {
 *                    user.setCreatedAt(((Date) createdAtObj).toInstant()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toLocalDateTime());
 *                }
 *
 *                Object updatedAtObj = doc.get("updatedAt");
 *                if (updatedAtObj instanceof Date) {
 *                    user.setUpdatedAt(((Date) updatedAtObj).toInstant()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toLocalDateTime());
 *                }
 *
 *                return Optional.of(user);
 *
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur lors de la recherche", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // SAVE : ~50 lignes de code
 *        // ══════════════════════════════════════════════════════════════════════
 *        public User save(User user) {
 *            try {
 *                // MAPPING MANUEL User → Document (25+ lignes)
 *                Document doc = new Document();
 *
 *                // Gestion de l'ID (insert vs update)
 *                if (user.getId() != null && !user.getId().isEmpty()) {
 *                    doc.append("_id", new ObjectId(user.getId()));
 *                }
 *
 *                doc.append("email", user.getEmail());
 *                doc.append("password", user.getPassword());
 *                doc.append("pseudo", user.getPseudo());
 *
 *                // Conversion Enum → String
 *                doc.append("role", user.getRole() != null ? user.getRole().name() : "USER");
 *                doc.append("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
 *
 *                doc.append("failedAttempts", user.getFailedAttempts());
 *
 *                // Conversion LocalDateTime → Date pour MongoDB
 *                if (user.getLockedUntil() != null) {
 *                    Date date = Date.from(user.getLockedUntil()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toInstant());
 *                    doc.append("lockedUntil", date);
 *                }
 *
 *                doc.append("points", user.getPoints());
 *                doc.append("level", user.getLevel());
 *
 *                if (user.getCreatedAt() != null) {
 *                    doc.append("createdAt", Date.from(user.getCreatedAt()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toInstant()));
 *                }
 *
 *                if (user.getUpdatedAt() != null) {
 *                    doc.append("updatedAt", Date.from(user.getUpdatedAt()
 *                        .atZone(ZoneId.systemDefault())
 *                        .toInstant()));
 *                }
 *
 *                // INSERT ou UPDATE selon présence de l'ID
 *                if (user.getId() == null || user.getId().isEmpty()) {
 *                    // INSERT
 *                    collection.insertOne(doc);
 *                    ObjectId insertedId = doc.getObjectId("_id");
 *                    user.setId(insertedId.toString());
 *                } else {
 *                    // UPDATE
 *                    Bson filter = Filters.eq("_id", new ObjectId(user.getId()));
 *                    UpdateResult result = collection.replaceOne(filter, doc);
 *
 *                    if (result.getMatchedCount() == 0) {
 *                        throw new RuntimeException("User not found for update");
 *                    }
 *                }
 *
 *                return user;
 *
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur lors de la sauvegarde", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // FINDBYID : ~25 lignes (mapping répété)
 *        // ══════════════════════════════════════════════════════════════════════
 *        public Optional<User> findById(String id) {
 *            try {
 *                Bson filter = Filters.eq("_id", new ObjectId(id));
 *                Document doc = collection.find(filter).first();
 *
 *                if (doc == null) {
 *                    return Optional.empty();
 *                }
 *
 *                // Même code de mapping que findByEmail (DUPLICATION)
 *                User user = new User();
 *                // ... 15+ lignes de mapping identiques
 *
 *                return Optional.of(user);
 *
 *            } catch (IllegalArgumentException e) {
 *                // ObjectId invalide
 *                return Optional.empty();
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur lors de la recherche", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // FINDALL : ~20 lignes
 *        // ══════════════════════════════════════════════════════════════════════
 *        public List<User> findAll() {
 *            List<User> users = new ArrayList<>();
 *            try {
 *                for (Document doc : collection.find()) {
 *                    // Même code de mapping répété encore (DUPLICATION x3)
 *                    User user = new User();
 *                    // ... 15+ lignes de mapping
 *                    users.add(user);
 *                }
 *                return users;
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // EXISTSBYEMAIL : ~10 lignes
 *        // ══════════════════════════════════════════════════════════════════════
 *        public boolean existsByEmail(String email) {
 *            try {
 *                Bson filter = Filters.eq("email", email);
 *                return collection.countDocuments(filter) > 0;
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // DELETEBYID : ~10 lignes
 *        // ══════════════════════════════════════════════════════════════════════
 *        public void deleteById(String id) {
 *            try {
 *                Bson filter = Filters.eq("_id", new ObjectId(id));
 *                DeleteResult result = collection.deleteOne(filter);
 *                if (result.getDeletedCount() == 0) {
 *                    throw new RuntimeException("User not found for deletion");
 *                }
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur", e);
 *            }
 *        }
 *
 *        // ══════════════════════════════════════════════════════════════════════
 *        // FINDBYPSEUDO : ~30 lignes (même logique que findByEmail)
 *        // ══════════════════════════════════════════════════════════════════════
 *        public Optional<User> findByPseudo(String pseudo) {
 *            try {
 *                Bson filter = Filters.eq("pseudo", pseudo);
 *                Document doc = collection.find(filter).first();
 *
 *                if (doc == null) {
 *                    return Optional.empty();
 *                }
 *
 *                // Même mapping répété (DUPLICATION x4)
 *                User user = new User();
 *                // ... 15+ lignes
 *
 *                return Optional.of(user);
 *            } catch (MongoException e) {
 *                throw new RuntimeException("Erreur", e);
 *            }
 *        }
 *
 *        // ... autres méthodes similaires
 *    }
 *
 *    → BILAN DAO MANUEL :
 *      • UserDAO.java : ~300-350 lignes de code
 *      • Code de mapping DUPLIQUÉ 6-7 fois (findByEmail, findById, findAll, etc.)
 *      • Conversions de type répétitives (Date ↔ LocalDateTime, String ↔ Enum)
 *      • Gestion manuelle des exceptions MongoDB
 *      • Gestion manuelle des Optional
 *      • Code fragile : ajouter un champ User = modifier 10+ endroits
 *      • Tests complexes : mock de MongoCollection
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ APPROCHE AVEC SPRING DATA MONGODB                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 1. CONFIGURATION AUTOMATIQUE (application.yml - 3 lignes) :
 *    ──────────────────────────────────────────────────────────
 *    spring:
 *      data:
 *        mongodb:
 *          uri: mongodb://localhost:27017/game_platform
 *          auto-index-creation: true
 *
 *    → Spring Boot configure automatiquement :
 *      ✓ MongoClient avec pool de connexions optimisé
 *      ✓ MongoTemplate pour opérations avancées
 *      ✓ Convertisseurs de type (LocalDateTime ↔ Date, Enum ↔ String)
 *      ✓ Gestion du cycle de vie des connexions
 *      ✓ Health check MongoDB dans /actuator/health
 *      ✓ Métriques de performance dans /actuator/metrics
 *
 * 2. INTERFACE REPOSITORY (6 lignes de déclaration) :
 *    ─────────────────────────────────────────────────
 *    @Repository
 *    public interface UserRepository extends MongoRepository<User, String> {
 *        Optional<User> findByEmail(String email);
 *        Optional<User> findByPseudo(String pseudo);
 *        boolean existsByEmail(String email);
 *        boolean existsByPseudo(String pseudo);
 *
 *        @Query("{ 'email': ?0, 'status': { $ne: 'DELETED' } }")
 *        Optional<User> findActiveByEmail(String email);
 *    }
 *
 *    → Spring Data génère AUTOMATIQUEMENT l'implémentation complète à la compilation
 *    → Aucun code de mapping à écrire
 *    → Aucune gestion manuelle des exceptions
 *
 * 3. MÉTHODES HÉRITÉES DE MongoRepository (gratuites) :
 *    ───────────────────────────────────────────────────
 *    • save(User user)                    → insert ou update automatique
 *    • findById(String id)                → Optional<User>
 *    • findAll()                          → List<User>
 *    • findAll(Sort sort)                 → List<User> triée
 *    • findAll(Pageable pageable)         → Page<User> avec pagination
 *    • deleteById(String id)              → suppression
 *    • delete(User user)                  → suppression par entité
 *    • count()                            → long (nombre total)
 *    • existsById(String id)              → boolean
 *    • saveAll(List<User> users)          → insert/update en batch
 *    • deleteAll()                        → suppression totale
 *    • flush()                            → force l'écriture
 *
 *    → ~15 méthodes CRUD générées automatiquement
 *    → SANS écrire une seule ligne de code
 *
 * 4. QUERY METHOD DERIVATION (Génération automatique de requêtes) :
 *    ────────────────────────────────────────────────────────────────
 *    Spring Data parse le nom de la méthode et génère la requête MongoDB.
 *
 *    Syntaxe : [Action][Distinct?][By][Propriété][Opérateur][OrderBy][Propriété]
 *
 *    Exemples automatiquement supportés :
 *    ────────────────────────────────────
 *    • findByEmail(String email)
 *      → { "email": email }
 *
 *    • findByRole(Role role)
 *      → { "role": "USER" }
 *
 *    • findByStatusAndRole(AccountStatus status, Role role)
 *      → { "status": "ACTIVE", "role": "USER" }
 *
 *    • findByEmailOrPseudo(String email, String pseudo)
 *      → { $or: [ { "email": email }, { "pseudo": pseudo } ] }
 *
 *    • findByFailedAttemptsGreaterThan(int attempts)
 *      → { "failedAttempts": { $gt: attempts } }
 *
 *    • findByPointsBetween(int min, int max)
 *      → { "points": { $gte: min, $lte: max } }
 *
 *    • findByEmailContaining(String keyword)
 *      → { "email": { $regex: keyword, $options: "i" } }
 *
 *    • findByPseudoStartingWith(String prefix)
 *      → { "pseudo": { $regex: "^prefix" } }
 *
 *    • findByCreatedAtBefore(LocalDateTime date)
 *      → { "createdAt": { $lt: date } }
 *
 *    • findByRoleIn(List<Role> roles)
 *      → { "role": { $in: ["USER", "PREMIUM"] } }
 *
 *    • findByStatusNot(AccountStatus status)
 *      → { "status": { $ne: "DELETED" } }
 *
 *    • findTop10ByOrderByPointsDesc()
 *      → { } sort: { "points": -1 } limit: 10
 *
 *    • findFirstByOrderByCreatedAtDesc()
 *      → { } sort: { "createdAt": -1 } limit: 1
 *
 *    • countByStatus(AccountStatus status)
 *      → db.users.countDocuments({ "status": "ACTIVE" })
 *
 *    • existsByEmailAndStatus(String email, AccountStatus status)
 *      → db.users.countDocuments({ "email": email, "status": "ACTIVE" }) > 0
 *
 *    • deleteByEmail(String email)
 *      → db.users.deleteOne({ "email": email })
 *
 *    → Mots-clés supportés : And, Or, Between, LessThan, GreaterThan, After,
 *                            Before, Like, StartingWith, EndingWith, Containing,
 *                            In, NotIn, Not, True, False, OrderBy, Top, First,
 *                            Distinct, IgnoreCase, etc.
 *
 * 5. REQUÊTES PERSONNALISÉES AVEC @QUERY :
 *    ──────────────────────────────────────
 *    Pour les requêtes complexes non supportées par Query Derivation :
 *
 *    @Query("{ 'email': ?0, 'status': { $ne: 'DELETED' } }")
 *    Optional<User> findActiveByEmail(String email);
 *
 *    @Query("{ 'role': { $in: ?0 }, 'points': { $gte: ?1 } }")
 *    List<User> findByRolesAndMinPoints(List<Role> roles, int minPoints);
 *
 *    @Query(value = "{ 'status': 'ACTIVE' }", count = true)
 *    long countActiveUsers();
 *
 *    @Query("{ 'email': { $regex: ?0, $options: 'i' } }")
 *    List<User> searchByEmailIgnoreCase(String keyword);
 *
 *    @Query(value = "{ 'level': { $gte: ?0 } }", fields = "{ 'email': 1, 'pseudo': 1, 'level': 1 }")
 *    List<User> findHighLevelUsersProjection(int minLevel);
 *
 *    → Paramètres : ?0, ?1, ?2 (index) ou :#{#param} (SpEL)
 *    → Support JSON MongoDB natif (tous les opérateurs)
 *    → Type-safe : erreur de compilation si mauvais type
 *    → Projection possible avec fields
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ COMPARAISON CHIFFRÉE : DAO MANUEL vs SPRING DATA                           │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌───────────────────────────┬──────────────────┬─────────────────────────────┐
 * │ CRITÈRE                   │ DAO MANUEL       │ SPRING DATA MONGODB         │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Lignes de code            │ 300-350          │ 6 (interface)               │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Mapping Document ↔ User   │ Manuel (~15 l)   │ Automatique (@Document)     │
 * │                           │ Dupliqué 6-7×    │ MappingMongoConverter       │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Gestion exceptions        │ try-catch partout│ Traduction automatique      │
 * │                           │ ~100 lignes      │ DataAccessException         │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Ajout d'un champ User     │ ~30-40 lignes    │ 0 ligne (auto-détecté)      │
 * │                           │ (6 méthodes DAO) │                             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Nouvelle méthode find...  │ 20-30 lignes     │ 1 ligne (signature)         │
 * │                           │ + mapping dupli  │                             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Tests unitaires           │ Mock MongoClient │ @DataMongoTest              │
 * │                           │ ~50 lignes/test  │ ~10 lignes/test             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Pagination                │ skip/limit       │ Pageable (1 paramètre)      │
 * │                           │ ~20 lignes       │                             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Tri                       │ sort() manuel    │ Sort parameter              │
 * │                           │ ~10 lignes       │                             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Cache                     │ À implémenter    │ @Cacheable natif            │
 * │                           │ ~100+ lignes     │                             │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Transactions              │ ClientSession    │ @Transactional              │
 * │                           │ ~30 lignes       │ (annotation)                │
 * ├───────────────────────────┼──────────────────┼─────────────────────────────┤
 * │ Auditing (createdAt...)   │ Setter manuel    │ @CreatedDate / @LastMod...  │
 * │                           │ partout          │ (automatique)               │
 * └───────────────────────────┴──────────────────┴─────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ AVANTAGES DE SPRING DATA MONGODB                                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ✅ RÉDUCTION DU CODE :
 *    • 98% de code en moins (6 lignes vs 350 lignes)
 *    • Pas de code de mapping répétitif
 *    • Pas de gestion manuelle des exceptions
 *
 * ✅ PRODUCTIVITÉ :
 *    • Nouvelle méthode = 1 ligne de signature vs 20-30 lignes
 *    • Modification du modèle = 0 modification du repository
 *    • Requêtes générées automatiquement = zéro bug de typo
 *
 * ✅ MAINTENABILITÉ :
 *    • Code déclaratif (intention claire dans le nom de méthode)
 *    • Pas de duplication
 *    • Refactoring sûr (IDE détecte tous les usages)
 *
 * ✅ TESTABILITÉ :
 *    • @DataMongoTest : contexte Spring léger pour tests
 *    • Pas de mock complexe de MongoClient
 *    • Tests rapides avec base embarquée (Flapdoodle)
 *
 * ✅ PERFORMANCE :
 *    • Pool de connexions optimisé automatiquement
 *    • Cache de metadata des entités
 *    • Support natif du lazy loading
 *    • Pagination efficace (pas de chargement complet en mémoire)
 *
 * ✅ FONCTIONNALITÉS AVANCÉES (Sans code supplémentaire) :
 *    • Pagination & Tri (Pageable, Sort)
 *    • Projections (DTO mapping automatique)
 *    • Auditing (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)
 *    • Query by Example (QBE)
 *    • Aggregation Framework
 *    • Change Streams (réactivité temps réel)
 *    • Transactions (@Transactional)
 *    • Cache (@Cacheable)
 *
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * @Repository : Active la traduction automatique des exceptions MongoDB
 *               en DataAccessException Spring (hiérarchie unifiée).
 *
 *               SANS @Repository : MongoException (spécifique MongoDB, non portable)
 *               AVEC @Repository : DataAccessException → gérée par @ExceptionHandler
 *                                → Même gestion d'erreur pour MongoDB, MySQL, PostgreSQL
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FINDBYEMAIL - Recherche par email (Query Method Derivation)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * GÉNÉRATION AUTOMATIQUE PAR SPRING DATA :
     * ────────────────────────────────────────
     * Spring Data parse le nom de la méthode :
     *    1. "find"       → Opération de lecture
     *    2. "By"         → Début des critères
     *    3. "Email"      → Propriété de l'entité User
     *    4. Paramètre    → Valeur du filtre
     *
     * Requête MongoDB générée :
     *    db.users.findOne({ "email": email })
     *
     * Code équivalent généré (invisible, généré par Spring à la compilation) :
     * ───────────────────────────────────────────────────────────────────────
     * public Optional<User> findByEmail(String email) {
     *     Query query = new Query(Criteria.where("email").is(email));
     *     User user = mongoTemplate.findOne(query, User.class, "users");
     *     return Optional.ofNullable(user);
     * }
     *
     * COMPARAISON AVEC DAO MANUEL :
     * ─────────────────────────────
     * DAO Manuel :
     *    • 30+ lignes de code (requête + mapping + gestion exception)
     *    • Mapping manuel Document → User (15 lignes)
     *    • Conversions de type manuelles
     *    • try-catch MongoException
     *
     * Spring Data :
     *    • 1 ligne (signature de méthode)
     *    • Mapping automatique via @Document
     *    • Conversions automatiques (Date ↔ LocalDateTime, String ↔ Enum)
     *    • Gestion automatique des exceptions
     *
     * AVANTAGES :
     *    ✓ Aucun risque d'erreur dans la requête MongoDB
     *    ✓ Type-safe (erreur de compilation si email n'existe pas dans User)
     *    ✓ Support Optional<T> natif (pas de null à gérer)
     *    ✓ Testable facilement avec @DataMongoTest
     */
    Optional<User> findByEmail(String email);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FINDBYPSEUDO - Recherche par pseudo
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Même principe que findByEmail.
     * Requête générée : { "pseudo": pseudo }
     */
    Optional<User> findByPseudo(String pseudo);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FINDACTIVEBYEMAIL - Requête personnalisée avec @Query
     * ════════════════════════════════════════════════════════════════════════════
     *
     * UTILISATION DE @QUERY :
     * ───────────────────────
     * Quand Query Derivation ne suffit pas, @Query permet d'écrire la requête
     * MongoDB en JSON directement.
     *
     * Syntaxe :
     * ─────────
     * @Query("{ 'champ1': ?0, 'champ2': { $operateur: ?1 } }")
     * Optional<User> maMethode(Type param0, Type param1);
     *
     * Ici : { 'email': ?0, 'status': { $ne: 'DELETED' } }
     *   → Cherche un user avec l'email donné (?0 = premier paramètre)
     *   → DONT le status n'est PAS ($ne = not equal) DELETED
     *
     * ÉQUIVALENT SANS SPRING :
     * ────────────────────────
     * Bson filter = Filters.and(
     *     Filters.eq("email", email),
     *     Filters.ne("status", "DELETED")
     * );
     * Document doc = collection.find(filter).first();
     * // + 15 lignes de mapping manuel
     *
     * AVANTAGES DE @QUERY :
     * ─────────────────────
     * ✓ Syntaxe MongoDB native (JSON)
     * ✓ Support de tous les opérateurs MongoDB :
     *   - Comparaison : $eq, $ne, $gt, $gte, $lt, $lte
     *   - Logique : $and, $or, $not, $nor
     *   - Tableaux : $in, $nin, $all, $elemMatch
     *   - Texte : $regex, $text
     *   - Géospatial : $near, $geoWithin
     * ✓ Mapping automatique du résultat
     * ✓ Paramètres type-safe
     * ✓ Testable facilement
     */
    @Query("{ 'email': ?0, 'status': { $ne: 'DELETED' } }")
    Optional<User> findActiveByEmail(String email);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * EXISTSBYEMAIL - Vérification d'existence (Optimisée)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * GÉNÉRATION AUTOMATIQUE :
     * ────────────────────────
     * Parsing : existsByEmail
     *   → Préfixe : "exists" (retourne boolean)
     *   → Propriété : email
     *   → Requête générée : db.users.countDocuments({ "email": email }) > 0
     *
     * OPTIMISATION AUTOMATIQUE :
     * ─────────────────────────
     * Spring Data utilise countDocuments() au lieu de find().first()
     *   → Plus performant (pas de récupération de document complet)
     *   → Index utilisé automatiquement (@Indexed sur email)
     *   → Réseau : ~10 bytes vs ~500-1000 bytes
     *
     * ÉQUIVALENT MANUEL :
     * ───────────────────
     * Bson filter = Filters.eq("email", email);
     * long count = collection.countDocuments(filter);
     * return count > 0;
     *
     * USAGE TYPIQUE :
     * ──────────────
     * // Dans AuthServiceImpl.register()
     * if (userRepository.existsByEmail(request.getEmail())) {
     *     throw new IllegalArgumentException("Email déjà utilisé");
     * }
     */
    boolean existsByEmail(String email);

    /**
     * EXISTSBYPSEUDO - Vérification d'unicité du pseudo
     * Même principe que existsByEmail.
     */
    boolean existsByPseudo(String pseudo);
}