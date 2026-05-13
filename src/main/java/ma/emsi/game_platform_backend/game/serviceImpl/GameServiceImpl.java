package ma.emsi.game_platform_backend.game.serviceImpl;

import ma.emsi.game_platform_backend.game.dto.GameCreateRequest;
import ma.emsi.game_platform_backend.game.dto.GameDTO;
import ma.emsi.game_platform_backend.game.model.Category;
import ma.emsi.game_platform_backend.game.model.Game;
import ma.emsi.game_platform_backend.game.model.GameCategory;
import ma.emsi.game_platform_backend.game.repository.CategoryRepository;
import ma.emsi.game_platform_backend.game.repository.GameCategoryRepository;
import ma.emsi.game_platform_backend.game.repository.GameRepository;
import ma.emsi.game_platform_backend.game.service.GameService;
import ma.emsi.game_platform_backend.shared.exception.BusinessException;
import ma.emsi.game_platform_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final CategoryRepository categoryRepository;
    private final GameCategoryRepository gameCategoryRepository;

    public GameServiceImpl(
            GameRepository gameRepository,
            CategoryRepository categoryRepository,
            GameCategoryRepository gameCategoryRepository
    ) {
        this.gameRepository = gameRepository;
        this.categoryRepository = categoryRepository;
        this.gameCategoryRepository = gameCategoryRepository;
    }

    /**
     * OBSOLÈTE : La création de jeu sans fichier n'est plus autorisée via l'API.
     * On la garde uniquement pour un usage interne (DataInitializer) si nécessaire.
     */
    @Override
    public GameDTO createGame(GameCreateRequest request) {
        throw new BusinessException("Le mode création par URL est désactivé. Veuillez uploader un fichier ZIP.");
    }

    @Override
    public GameDTO createGameWithFile(GameCreateRequest request, MultipartFile file) {
        // 1. Validation du slug
        if (gameRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException("Un jeu avec ce slug existe déjà.");
        }

        // 2. Extraction sécurisée du ZIP
        String gamesDir = "src/main/resources/static/games";
        Path gameFolder = Paths.get(gamesDir, request.getSlug()).normalize();

        try {
            // Nettoyage si le dossier existe déjà par erreur
            if (Files.exists(gameFolder)) {
                Files.walk(gameFolder)
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
            Files.createDirectories(gameFolder);

            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = gameFolder.resolve(entry.getName()).normalize();

                    // Sécurité contre le Zip Slip (vérifier que l'entrée reste dans le dossier cible)
                    if (!entryPath.startsWith(gameFolder)) {
                        throw new BusinessException("Fichier ZIP malveillant détecté.");
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur technique lors de l'extraction du ZIP : " + e.getMessage());
        }

        // 3. Vérifier la présence du point d'entrée
        if (!Files.exists(gameFolder.resolve("index.html"))) {
            throw new BusinessException("Le package est invalide : fichier index.html manquant à la racine.");
        }

        // 4. Construction de l'objet métier
        Game game = Game.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .difficulty(request.getDifficulty())
                .isPremium(request.getIsPremium())
                .multiplier(request.getMultiplier())
                .isActive(true)
                .plays30d(0)
                .totalPlays(0L)
                .averageScore(0.0)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Utilisation de la méthode helper définie dans le modèle corrigé
        game.generateInternalUrl();

        Game saved = gameRepository.save(game);

        // Gestion des catégories si présentes dans la requête
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            replaceCategories(saved.getId(), request.getCategoryIds());
        }

        return toDTO(saved);
    }

    @Override
    public GameDTO updateGame(String id, GameCreateRequest request) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jeu introuvable avec l'id : " + id));

        // On ne permet pas de changer l'URL manuellement ici pour les jeux locaux
        game.setTitle(request.getTitle());
        game.setDescription(request.getDescription());
        game.setThumbnailUrl(request.getThumbnailUrl());
        game.setDifficulty(request.getDifficulty());
        game.setIsPremium(request.getIsPremium());
        game.setMultiplier(request.getMultiplier());
        game.setUpdatedAt(LocalDateTime.now());

        // Note : On ne change pas le slug car cela briserait le lien avec le dossier physique
        // Si besoin de changer le slug, il faudrait renommer le dossier sur le disque.

        Game saved = gameRepository.save(game);
        replaceCategories(saved.getId(), request.getCategoryIds());

        return toDTO(saved);
    }

    @Override
    public void deleteGame(String id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jeu introuvable."));
        game.softDelete();
        gameRepository.save(game);
    }

    @Override
    public List<GameDTO> getAllGames() {

        return gameRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public GameDTO getGameById(String id) {
        Game game = gameRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jeu introuvable."));
        return toDTO(game);
    }

    @Override
    public GameDTO getGameBySlug(String slug) {
        Game game = gameRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Jeu introuvable."));
        return toDTO(game);
    }

    private void replaceCategories(String gameId, List<String> categoryIds) {
        gameCategoryRepository.deleteByGameId(gameId);
        if (categoryIds == null || categoryIds.isEmpty()) return;

        List<String> distinctIds = new ArrayList<>(new LinkedHashSet<>(categoryIds));
        List<Category> categories = categoryRepository.findAllById(distinctIds);

        List<GameCategory> links = categories.stream()
                .map(cat -> GameCategory.builder()
                        .gameId(gameId)
                        .categoryId(cat.getId())
                        .assignedAt(LocalDateTime.now())
                        .build())
                .toList();

        gameCategoryRepository.saveAll(links);
    }

    private GameDTO toDTO(Game game) {
        List<String> categoryIds = gameCategoryRepository.findByGameId(game.getId())
                .stream()
                .map(GameCategory::getCategoryId)
                .collect(Collectors.toList());

        return GameDTO.builder()
                .id(game.getId())
                .slug(game.getSlug())
                .title(game.getTitle())
                .description(game.getDescription())
                .gameUrl(game.getGameUrl())
                .thumbnailUrl(game.getThumbnailUrl())
                .difficulty(game.getDifficulty())
                .isPremium(game.getIsPremium())
                .isActive(game.getIsActive())
                .multiplier(game.getMultiplier())
                .plays30d(game.getPlays30d())
                .totalPlays(game.getTotalPlays())
                .averageScore(game.getAverageScore())
                .createdBy(game.getCreatedBy())
                .createdAt(game.getCreatedAt())
                .updatedAt(game.getUpdatedAt())
                .categoryIds(categoryIds)
                .build();
    }
}