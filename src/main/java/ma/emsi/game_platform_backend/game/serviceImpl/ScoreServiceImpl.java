package ma.emsi.game_platform_backend.game.serviceImpl;

import ma.emsi.game_platform_backend.game.dto.LeaderboardDTO;
import ma.emsi.game_platform_backend.game.dto.ScoreDTO;
import ma.emsi.game_platform_backend.game.model.Game;
import ma.emsi.game_platform_backend.game.model.Score;
import ma.emsi.game_platform_backend.game.repository.GameRepository;
import ma.emsi.game_platform_backend.game.repository.ScoreRepository;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.game.service.ScoreService;
import ma.emsi.game_platform_backend.gamification.service.GamificationService;  // ← AJOUT
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository     scoreRepository;
    private final GameRepository      gameRepository;
    private final UserRepository      userRepository;          // ← AJOUT
    private final GamificationService gamificationService;  // ← AJOUT
    private final MongoTemplate       mongoTemplate;


    public ScoreServiceImpl(ScoreRepository scoreRepository,
                            GameRepository gameRepository,
                            UserRepository userRepository,    // ← AJOUT
                            GamificationService gamificationService,  // ← AJOUT
                            MongoTemplate mongoTemplate) {
        this.scoreRepository     = scoreRepository;
        this.gameRepository      = gameRepository;
        this.userRepository     = userRepository;
        this.gamificationService = gamificationService;  // ← AJOUT
        this.mongoTemplate       = mongoTemplate;
    }

    @Override
    public ScoreDTO submitScore(String userId, String gameId, int value, int duration) {

        Game game = gameRepository.findByIdAndIsActiveTrue(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Game not found with id: " + gameId));

        int pointsEarned = game.calculatePoints(value);

        Score score = Score.builder()
                .userId(userId)
                .gameId(gameId)
                .value(value)
                .pointsEarned(pointsEarned)
                .duration(duration)
                .playedAt(LocalDateTime.now())
                .build();

        Score saved = scoreRepository.save(score);
        game.incrementPlays();
        game.updateAverageScore(value);
        gameRepository.save(game);

        // ── GAMIFICATION ─────────────────────────────────────────
        // Ajout des points + recalcul niveau + évaluation badges.
        // Non bloquant : si ça échoue, le score reste sauvegardé.
        try {
            gamificationService.addPoints(userId, pointsEarned);  // ← AJOUT
        } catch (Exception e) {
            System.err.println("[Gamification] addPoints error: " + e.getMessage());
        }
        // ─────────────────────────────────────────────────────────

        return toDTO(saved, null, null);
    }

        @Override
        public LeaderboardDTO getLeaderboard(String gameId) {

            Game game = gameRepository.findByIdAndIsActiveTrue(gameId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Game not found with id: " + gameId));

            // --- Agrégation avec lookup pour récupérer le pseudo ---
            UnwindOperation unwindUsers = Aggregation.unwind("userDocs", true);

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("gameId").is(gameId)),
                    Aggregation.sort(Sort.Direction.DESC, "value"),
                    Aggregation.group("userId")
                            .first("$$ROOT").as("doc"),
                    Aggregation.replaceRoot("doc"),
                    Aggregation.lookup("users", "userId", "_id", "userDocs"),
                    unwindUsers,
                    Aggregation.project()
                            .and("userId").as("userId")
                            .and("value").as("value")
                            .and("pointsEarned").as("pointsEarned")
                            .and("duration").as("duration")
                            .and("playedAt").as("playedAt")
                            .and("userDocs.pseudo").as("pseudo"),
                    Aggregation.sort(Sort.Direction.DESC, "value"),
                    Aggregation.limit(10)
            );

            List<ScoreDTO> topScores = mongoTemplate
                    .aggregate(aggregation, "scores", ScoreDTO.class)
                    .getMappedResults();

            // --- Récupère les pseudos manquants via UserRepository ---
            List<String> userIds = topScores.stream()
                    .filter(s -> s.getPseudo() == null || s.getPseudo().isBlank())
                    .map(ScoreDTO::getUserId)
                    .toList();

            if (!userIds.isEmpty()) {
                Map<String, String> pseudoMap = userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getPseudo() != null ? u.getPseudo() : u.getEmail()));

                topScores.forEach(s -> {
                    if ((s.getPseudo() == null || s.getPseudo().isBlank()) && pseudoMap.containsKey(s.getUserId())) {
                        s.setPseudo(pseudoMap.get(s.getUserId()));
                    }
                });
            }

            // --- Fallback final : si toujours pas de pseudo, on affiche un libellé lisible ---
            for (int i = 0; i < topScores.size(); i++) {
                ScoreDTO s = topScores.get(i);
                s.setRank(i + 1);
                if (s.getPseudo() == null || s.getPseudo().isBlank()) {
                    s.setPseudo("Joueur");
                }
            }

            return LeaderboardDTO.builder()
                    .gameId(gameId)
                    .gameTitle(game.getTitle())
                    .topScores(topScores)
                    .build();
        }



    @Override
    public ScoreDTO getPersonalBest(String userId, String gameId) {
        Score best = scoreRepository
                .findTopByUserIdAndGameIdOrderByValueDesc(userId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No score found for userId=" + userId + " gameId=" + gameId));

        long rank = scoreRepository.findByGameIdOrderByValueDesc(gameId)
                .stream()
                .takeWhile(s -> s.getValue() > best.getValue())
                .count() + 1;

        return toDTO(best, (int) rank, null);
    }

    private ScoreDTO toDTO(Score score, Integer rank, String pseudo) {
        return ScoreDTO.builder()
                .id(score.getId())
                .userId(score.getUserId())
                .gameId(score.getGameId())
                .pseudo(pseudo)
                .rank(rank)
                .value(score.getValue())
                .pointsEarned(score.getPointsEarned())
                .duration(score.getDuration())
                .playedAt(score.getPlayedAt())
                .build();
    }
}