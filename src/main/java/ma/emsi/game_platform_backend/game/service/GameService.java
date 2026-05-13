package ma.emsi.game_platform_backend.game.service;

import ma.emsi.game_platform_backend.game.dto.GameCreateRequest;
import ma.emsi.game_platform_backend.game.dto.GameDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GameService {
    GameDTO createGame(GameCreateRequest request);
    GameDTO createGameWithFile(GameCreateRequest request, MultipartFile file);
    GameDTO updateGame(String id, GameCreateRequest request);
    void deleteGame(String id);
    List<GameDTO> getAllGames();
    GameDTO getGameById(String id);
    GameDTO getGameBySlug(String slug);
}