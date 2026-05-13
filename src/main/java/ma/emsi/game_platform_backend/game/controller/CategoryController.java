package ma.emsi.game_platform_backend.game.controller;

import ma.emsi.game_platform_backend.game.model.Category;
import ma.emsi.game_platform_backend.game.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  CATEGORY CONTROLLER - COUCHE EXPOSITION (API REST) & ARCHITECTURE
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * Ce composant est la "Porte d'entrée" de votre application. Son seul rôle est de
 * recevoir les requêtes HTTP du client (ex: Frontend React), de les déléguer,
 * et de formater la réponse HTTP (JSON + Code de statut).
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : @RestController vs @Controller                              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * L'annotation @RestController est une combinaison de deux annotations :
 * 1. @Controller : Indique à Spring que cette classe gère des routes web.
 * 2. @ResponseBody : Indique que le retour des méthodes ne doit pas chercher une
 *                    vue HTML (comme Thymeleaf), mais doit être sérialisé
 *                    directement en JSON dans le corps de la réponse HTTP.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : LE "RACCOURCI" CONTROLLER -> REPOSITORY                     │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Dans une architecture classique en 3 couches (N-Tiers), le flux est :
 * Controller -> Service -> Repository.
 *
 * Ici, le Controller appelle directement le Repository. Est-ce une erreur ?
 * -> Non, c'est acceptable (et même conseillé par certains) pour des entités
 *    "Référentielles" très simples en lecture seule, comme lister des catégories.
 *    Créer un `CategoryService` juste pour faire un "return repository.findAll()"
 *    serait du code inutile (Over-engineering).
 * -> MAIS, si vous ajoutez de la logique (validation, création, DTOs), il faudra
 *    absolument introduire un Service.
 */
@RestController
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * L'INJECTION DE DÉPENDANCES PAR CONSTRUCTEUR (Best Practice)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * ❌ APPROCHE DÉCONSEILLÉE (Field Injection) :
     *    @Autowired
     *    private CategoryRepository categoryRepository;
     *
     *    Problèmes :
     *    - Difficile à tester (nécessite des outils de réflexion pour mocker).
     *    - Le champ n'est pas `final`, il pourrait être modifié (immuabilité non garantie).
     *
     * ✅ APPROCHE RECOMMANDÉE (Constructor Injection) :
     *    Déclarer le champ `final` et l'initialiser dans le constructeur.
     *    Depuis Spring 4.3, si la classe n'a qu'un seul constructeur, l'annotation
     *    @Autowired est implicite (plus besoin de l'écrire).
     *
     *    Avantages :
     *    - Immuabilité garantie (`final`).
     *    - Tests unitaires très simples (on passe un faux repository au constructeur).
     */
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * DÉFINITION DE LA ROUTE & GESTION DE LA RÉPONSE
     * ════════════════════════════════════════════════════════════════════════════
     *
     * @GetMapping("/api/categories") :
     *    Associe les requêtes HTTP de type GET sur l'URL "/api/categories" à
     *    cette méthode.
     *
     * ResponseEntity<List<Category>> :
     *    Pourquoi ne pas renvoyer juste `List<Category>` ?
     *    `ResponseEntity` permet de contrôler explicitement le code de statut HTTP.
     *    Ici, `ResponseEntity.ok(...)` génère un statut 200 (OK).
     *    Si les données n'étaient pas trouvées, on aurait pu faire un
     *    `ResponseEntity.notFound().build()` (statut 404).
     *
     * @return Une réponse HTTP 200 contenant la liste JSON des catégories actives.
     */
    @GetMapping("/api/categories")
    public ResponseEntity<List<Category>> getPublicCategories() {
        return ResponseEntity.ok(categoryRepository.findAllByIsActiveTrue());
    }
}