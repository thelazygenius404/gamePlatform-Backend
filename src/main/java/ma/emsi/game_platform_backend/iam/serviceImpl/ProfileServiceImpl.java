package ma.emsi.game_platform_backend.iam.serviceImpl;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.dto.ChangePasswordRequest;
import ma.emsi.game_platform_backend.iam.dto.ProfileDTO;
import ma.emsi.game_platform_backend.iam.dto.UpdateProfileRequest;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.iam.service.ProfileService;
import ma.emsi.game_platform_backend.shared.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileDTO getProfile(String email) {

        User user = findUserByEmail(email);

        return mapToDTO(user);
    }

    @Override
    public ProfileDTO updateProfile(String email, UpdateProfileRequest request) {

        User user = findUserByEmail(email);

        // CHECK PSEUDO
        if (!user.getPseudo().equals(request.getPseudo())
                && userRepository.existsByPseudo(request.getPseudo())) {

            throw new BusinessException("Ce pseudo est déjà utilisé.");
        }

        // CHECK EMAIL
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new BusinessException("Cet email est déjà utilisé.");
        }

        user.setPseudo(request.getPseudo());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return mapToDTO(user);
    }

    @Override
    public Map<String, String> uploadAvatar(String email, MultipartFile file) {

        User user = findUserByEmail(email);

        if (file.isEmpty()) {
            throw new BusinessException("Le fichier est vide.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename
                    .substring(originalFilename.lastIndexOf("."))
                    .toLowerCase();
        }

        boolean isValid =
                (contentType != null && contentType.startsWith("image/"))
                        || extension.matches("\\.(png|jpg|jpeg|gif|bmp|webp)$");

        if (!isValid) {
            throw new BusinessException("Format d'image invalide.");
        }

        String newFilename =
                "avatar_"
                        + user.getId()
                        + "_"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + extension;

        Path uploadPath = Paths.get("uploads/avatars");

        try {

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException("Erreur upload avatar.", e);
        }

        String avatarUrl = "/uploads/avatars/" + newFilename;

        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return Map.of(
                "message", "Avatar mis à jour.",
                "avatarUrl", avatarUrl
        );
    }

    @Override
    public Map<String, String> deleteAvatar(String email) {

        User user = findUserByEmail(email);

        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return Map.of(
                "message", "Avatar supprimé.",
                "avatarUrl", ""
        );
    }

    @Override
    public Map<String, String> changePassword(String email, ChangePasswordRequest request) {

        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {

            throw new BusinessException("Ancien mot de passe incorrect.");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {

            throw new BusinessException(
                    "Le nouveau mot de passe doit être différent."
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return Map.of(
                "message",
                "Mot de passe modifié avec succès."
        );
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur non trouvé"));
    }

    private ProfileDTO mapToDTO(User user) {

        return ProfileDTO.builder()
                .id(user.getId())
                .pseudo(user.getPseudo())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .level(user.getLevel())
                .points(user.getPoints())
                .build();
    }
}