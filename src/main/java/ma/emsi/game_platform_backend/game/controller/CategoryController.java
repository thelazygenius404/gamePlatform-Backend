package ma.emsi.game_platform_backend.game.controller;

import ma.emsi.game_platform_backend.game.model.Category;
import ma.emsi.game_platform_backend.game.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/api/categories")
    public ResponseEntity<List<Category>> getPublicCategories() {
        return ResponseEntity.ok(categoryRepository.findAllByIsActiveTrue());
    }
}