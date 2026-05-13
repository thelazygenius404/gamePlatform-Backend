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
 * Endpoints Game :
 *
 * GET    /api/games            → catalogue (tous)
 * GET    /api/games/{id}       → jeu par id
 * GET    /api/games/slug/{slug}→ jeu par slug
 * POST   /api/games            → créer (admin)
 * PUT    /api/games/{id}       → modifier (admin)
 * DELETE /api/games/{id}       → soft delete (admin)
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:3000") // Autorise ton frontend React // à restreindre en prod
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // ── GET /api/games ────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    // ── GET /api/games/{id} ───────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable String id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }

    // ── GET /api/games/slug/{slug} ────────────────────────────────
    @GetMapping("/slug/{slug}")
    public ResponseEntity<GameDTO> getGameBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(gameService.getGameBySlug(slug));
    }

    // ── POST /api/games ───────────────────────────────────────────
    @PostMapping
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody GameCreateRequest request) {
        GameDTO created = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /api/games/{id} ───────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> updateGame(
            @PathVariable String id,
            @Valid @RequestBody GameCreateRequest request) {
        return ResponseEntity.ok(gameService.updateGame(id, request));
    }

    // ── DELETE /api/games/{id} ────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}