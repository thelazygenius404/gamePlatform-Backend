package ma.emsi.game_platform_backend.game.service;

import ma.emsi.game_platform_backend.game.dto.LeaderboardDTO;
import ma.emsi.game_platform_backend.game.dto.ScoreDTO;

public interface ScoreService {
    ScoreDTO submitScore(String userId, String gameId, int value, int duration);
    LeaderboardDTO getLeaderboard(String gameId);
    ScoreDTO getPersonalBest(String userId, String gameId);
}