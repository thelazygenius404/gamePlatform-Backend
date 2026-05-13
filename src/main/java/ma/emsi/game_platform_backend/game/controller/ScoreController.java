package ma.emsi.game_platform_backend.game.controller;

import ma.emsi.game_platform_backend.game.dto.LeaderboardDTO;
import ma.emsi.game_platform_backend.game.dto.ScoreDTO;
import ma.emsi.game_platform_backend.game.service.ScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 *  SCORE CONTROLLER - PATH VARIABLES vs REQUEST PARAMS & JAVA RECORDS
 * ════════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ CONCEPT CLÉ 1 : @PathVariable vs @RequestParam                              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Quand utiliser l'un ou l'autre pour passer des paramètres dans l'URL ?
 *
 * 1. @PathVariable (Ex: /api/scores/leaderboard/123)
 *    -> À utiliser pour IDENTIFIER une ressource spécifique dans la hiérarchie.
 *    -> Le "gameId" fait partie intégrante du chemin vers le leaderboard.
 *
 * 2. @RequestParam (Ex: /api/scores/best?userId=456&gameId=123)
 *    -> À utiliser pour FILTRER, TRIER ou faire des recherches croisées.
 *    -> Ici, on ne cherche pas "la ressource 456", on cherche la ressource "best"
 *       en appliquant des filtres (quel utilisateur ? quel jeu ?).
 */
@RestController
@RequestMapping("/api/scores")
/**
 * ⚠️ RAPPEL DE SÉCURITÉ :
 * origins = "*" autorise TOUS les sites web d'internet à interroger votre API.
 * Pratique en développement, mais à remplacer par votre vrai domaine en production
 * (ex: origins = "https://mon-super-jeu.com").
 */
@CrossOrigin(origins = "*")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * ── POST /api/scores ──────────────────────────────────────────
     *
     * Reçoit le score depuis le Front-End à la fin d'une partie.
     * Le code HTTP 201 (CREATED) est renvoyé pour confirmer l'insertion en base.
     */
    @PostMapping
    public ResponseEntity<ScoreDTO> submitScore(@RequestBody SubmitScoreRequest request) {
        ScoreDTO score = scoreService.submitScore(
                request.userId(),
                request.gameId(),
                request.value(),
                request.duration()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(score);
    }

    /**
     * ── GET /api/scores/leaderboard/{gameId} ──────────────────────
     *
     * Récupère le Top 10 pour un jeu spécifique.
     * Le paramètre fait partie du chemin (Path) car chaque jeu possède
     * physiquement son propre classement.
     */
    @GetMapping("/leaderboard/{gameId}")
    public ResponseEntity<LeaderboardDTO> getLeaderboard(@PathVariable String gameId) {
        return ResponseEntity.ok(scoreService.getLeaderboard(gameId));
    }

    /**
     * ── GET /api/scores/best?userId=&gameId= ──────────────────────
     *
     * Récupère le meilleur score croisé entre un joueur et un jeu.
     * Les paramètres sont passés dans la "Query String" (?key=value).
     */
    @GetMapping("/best")
    public ResponseEntity<ScoreDTO> getPersonalBest(
            @RequestParam String userId,
            @RequestParam String gameId) {
        return ResponseEntity.ok(scoreService.getPersonalBest(userId, gameId));
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * CONCEPT CLÉ 2 : LES JAVA RECORDS (Introduits dans Java 14/16)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Avant les Records, pour recevoir un simple JSON de 4 champs, il fallait :
     * - Créer une classe séparée.
     * - Ajouter @Data de Lombok ou générer des Getters/Setters.
     * - Définir des constructeurs.
     *
     * Un `record` est une classe immuable (read-only) ultra-compacte.
     * Le compilateur Java génère automatiquement et nativement le constructeur,
     * les méthodes d'accès (ex: `request.userId()`), `equals()`, `hashCode()`
     * et `toString()`.
     *
     * C'est l'outil PARFAIT pour définir des petits "Request Body" à la volée
     * directement dans le fichier du Controller.
     */
    record SubmitScoreRequest(String userId, String gameId, int value, int duration) {}
}