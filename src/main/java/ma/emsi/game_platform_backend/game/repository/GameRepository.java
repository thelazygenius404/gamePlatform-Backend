package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends MongoRepository<Game, String> {

    Optional<Game> findBySlugAndIsActiveTrue(String slug);

    Optional<Game> findByIdAndIsActiveTrue(String id);

    List<Game> findAllByIsActiveTrue();

    boolean existsBySlug(String slug);

    long countByIsActiveTrue();}