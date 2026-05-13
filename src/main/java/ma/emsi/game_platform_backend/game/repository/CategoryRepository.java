package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  CATEGORY REPOSITORY - OPTIMISATION DES VALIDATIONS & LECTURES (MONGODB)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * Ce repository illustre les meilleures pratiques pour la gestion d'une entité
 * de type "Référentiel" ou "Dictionnaire" (une donnée qui change rarement mais
 * qui est lue très souvent).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : L'OPTIMISATION DES VALIDATIONS (existsBy)                   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Lors de la création d'une catégorie par un administrateur, vous devez vérifier
 * si le "label" ou le "slug" existent déjà pour renvoyer une belle erreur 400
 * (Bad Request) au client au lieu d'une erreur 500 (MongoWriteException) brutale.
 *
 * ❌ MAUVAISE PRATIQUE FRÉQUENTE :
 *    if (repository.findBySlug(slug).isPresent()) { throw new Exception... }
 *
 *    -> C'est très lourd ! MongoDB trouve le document, le lit en entier, l'envoie
 *       sur le réseau, Spring le convertit en Objet Java (BSON -> Java)...
 *       tout ça juste pour vérifier son existence !
 *
 * ✅ BONNE PRATIQUE (Utilisée ici) :
 *    Les méthodes `existsBy...`
 */
public interface CategoryRepository extends MongoRepository<Category, String> {

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * RECHERCHE EXACTE AVEC INDEX
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Utilisé pour récupérer les détails d'une catégorie depuis une URL.
     * Ex: GET /api/categories/jeux-de-role
     *
     * Cette requête est quasi-instantanée car le champ `slug` a été annoté avec
     * @Indexed(unique = true) dans le modèle Category.
     *
     * @param slug L'identifiant web de la catégorie.
     * @return Un Optional contenant la catégorie.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CANDIDAT IDÉAL POUR LE CACHE CÔTÉ SERVEUR
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Ramène toutes les catégories actives pour peupler le menu de navigation
     * du Frontend (React/Angular/Vue).
     *
     * 💡 ASTUCE D'ARCHITECTURE :
     * Puisque ce menu est affiché à chaque fois qu'un joueur ouvre l'application,
     * la méthode du Service qui appelle ce Repository devrait être annotée
     * avec @Cacheable("activeCategories"). Ainsi, MongoDB ne sera sollicité qu'une
     * seule fois, et les milliers de requêtes suivantes taperont directement dans
     * la mémoire vive (RAM) du serveur via Spring Cache.
     *
     * @return La liste de toutes les catégories visibles.
     */
    List<Category> findAllByIsActiveTrue();

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * VALIDATION ULTRA RAPIDE - SLUG
     * ════════════════════════════════════════════════════════════════════════════
     *
     * TRADUCTION MONGODB GÉNÉRÉE (Optimisée) :
     * db.categories.find({ "slug": "?0" }, { _id: 1 }).limit(1).hasNext()
     *
     * MongoDB s'arrête à la première occurrence trouvée dans son Index, et ne
     * renvoie AUCUNE donnée du document, juste un signal booléen sur le réseau.
     *
     * @param slug Le slug à vérifier.
     * @return true s'il est déjà pris.
     */
    boolean existsBySlug(String slug);

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * VALIDATION ULTRA RAPIDE - LABEL
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Indispensable dans votre Service avant un `repository.save()` pour s'assurer
     * que le nom de la catégorie (ex: "Action") n'est pas déjà utilisé.
     *
     * @param label Le nom public de la catégorie.
     * @return true si le nom existe déjà.
     */
    boolean existsByLabel(String label);
}