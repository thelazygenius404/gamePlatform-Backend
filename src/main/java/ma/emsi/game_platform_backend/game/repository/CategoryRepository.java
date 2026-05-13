package ma.emsi.game_platform_backend.game.repository;

import ma.emsi.game_platform_backend.game.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByIsActiveTrue();

    boolean existsBySlug(String slug);

    boolean existsByLabel(String label);
}