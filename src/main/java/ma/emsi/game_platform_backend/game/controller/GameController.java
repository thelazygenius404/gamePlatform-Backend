package ma.emsi.game_platform_backend.game.controller;

import jakarta.validation.Valid;
import ma.emsi.game_platform_backend.game.dto.GameCreateRequest;
import ma.emsi.game_platform_backend.game.dto.GameDTO;
import ma.emsi.game_platform_backend.game.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  GAME CONTROLLER - CONVENTIONS REST, VALIDATION & SÉCURITÉ CORS
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : L'ARCHITECTURE RESTful (Verbes et Ressources)               │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Une bonne API REST utilise les verbes HTTP pour définir l'action, et l'URL pour
 * définir la ressource (toujours au pluriel).
 *
 * ❌ MAUVAIS DESIGN (RPC Style) :
 *    POST /api/games/createGame
 *    POST /api/games/update?id=5
 *    GET  /api/games/delete/5
 *
 * ✅ BON DESIGN (RESTful - Utilisé ici) :
 *    POST   /api/games         (Créer un jeu)
 *    PUT    /api/games/{id}    (Remplacer un jeu)
 *    DELETE /api/games/{id}    (Supprimer un jeu)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 2 : LE CHEF D'ORCHESTRE (Délégation)                            │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Le Controller est intentionnellement "stupide" (dumb controller). Il n'y a
 * AUCUNE logique métier, boucle ou calcul ici. Son seul travail est :
 * 1. Recevoir la requête.
 * 2. Valider le format (@Valid).
 * 3. Déléguer au Service (gameService.createGame...).
 * 4. Renvoyer la bonne boîte HTTP (ResponseEntity) avec le bon code de statut.
 */
@RestController
@RequestMapping("/api/games")
/**
 * ════════════════════════════════════════════════════════════════════════════════
 * LA GESTION DU CORS (Cross-Origin Resource Sharing)
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * Par défaut, les navigateurs web bloquent les requêtes Ajax/Fetch si le frontend
 * et le backend ne sont pas sur le même domaine (ex: React sur localhost:3000 et
 * Spring sur localhost:8080). C'est une sécurité contre les attaques CSRF.
 *
 * @CrossOrigin dit à Spring d'ajouter les en-têtes HTTP (Access-Control-Allow-Origin)
 * qui disent au navigateur : "C'est bon, j'autorise le port 3000 à me parler".
 *
 * ⚠️ ATTENTION EN PRODUCTION :
 * Ne jamais utiliser `@CrossOrigin(origins = "*")` en prod sur des routes sensibles.
 * Utilisez des configurations globales de sécurité (ex: via SecurityFilterChain).
 */
@CrossOrigin(origins = "http://localhost:3000")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * ── GET /api/games ────────────────────────────────────────────
     * Renvoie un statut HTTP 200 (OK) par défaut.
     */
    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    /**
     * ── GET /api/games/{id} ───────────────────────────────────────
     * @PathVariable indique à Spring de prendre la valeur "{id}" dans
     * l'URL et de l'injecter dans la variable String id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable String id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }

    /**
     * ── GET /api/games/slug/{slug} ────────────────────────────────
     * L'URL est distincte (ajout de /slug/) pour que Spring ne confonde pas
     * cette route avec la route `/{id}` au-dessus.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<GameDTO> getGameBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(gameService.getGameBySlug(slug));
    }

    /**
     * ── POST /api/games ───────────────────────────────────────────
     *
     * @Valid : Déclenche les validations configurées dans GameCreateRequest
     *          (ex: @NotBlank, @Size). Si la validation échoue, Spring bloque
     *          la requête avant même d'entrer dans la méthode et renvoie un 400 Bad Request.
     *
     * @RequestBody : Demande à Spring de lire le JSON envoyé par le client
     *                et de le convertir (désérialiser) en objet GameCreateRequest.
     *
     * HTTP 201 (CREATED) : Bonne pratique REST. Quand une ressource est créée,
     *                      on ne renvoie pas 200 OK, mais 201 Created.
     */
    @PostMapping
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody GameCreateRequest request) {
        GameDTO created = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * ── PUT /api/games/{id} ───────────────────────────────────────
     * PUT est utilisé pour remplacer entièrement une ressource existante.
     * (Si on voulait juste modifier un seul champ, comme le titre, la bonne
     * pratique serait d'utiliser le verbe PATCH).
     */
    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> updateGame(
            @PathVariable String id,
            @Valid @RequestBody GameCreateRequest request) {
        return ResponseEntity.ok(gameService.updateGame(id, request));
    }

    /**
     * ── DELETE /api/games/{id} ────────────────────────────────────
     *
     * HTTP 204 (NO CONTENT) : Bonne pratique REST pour les suppressions.
     *                         La suppression a réussi, mais il n'y a plus de
     *                         données à renvoyer au client (d'où le `.build()`).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}