package ma.emsi.game_platform_backend.game.controller;

import ma.emsi.game_platform_backend.game.dto.LeaderboardDTO;
import ma.emsi.game_platform_backend.game.dto.ScoreDTO;
import ma.emsi.game_platform_backend.game.service.ScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints Score :
 *
 * POST /api/scores                              → soumettre un score
 * GET  /api/scores/leaderboard/{gameId}         → top 10 d'un jeu
 * GET  /api/scores/best?userId=&gameId=         → meilleur score personnel
 */
@RestController
@RequestMapping("/api/scores")
@CrossOrigin(origins = "*")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    // ── POST /api/scores ──────────────────────────────────────────
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

    // ── GET /api/scores/leaderboard/{gameId} ──────────────────────
    @GetMapping("/leaderboard/{gameId}")
    public ResponseEntity<LeaderboardDTO> getLeaderboard(@PathVariable String gameId) {
        return ResponseEntity.ok(scoreService.getLeaderboard(gameId));
    }

    // ── GET /api/scores/best?userId=&gameId= ──────────────────────
    @GetMapping("/best")
    public ResponseEntity<ScoreDTO> getPersonalBest(
            @RequestParam String userId,
            @RequestParam String gameId) {
        return ResponseEntity.ok(scoreService.getPersonalBest(userId, gameId));
    }

    // ── Record interne pour le body de la requête ─────────────────
    record SubmitScoreRequest(String userId, String gameId, int value, int duration) {}
}