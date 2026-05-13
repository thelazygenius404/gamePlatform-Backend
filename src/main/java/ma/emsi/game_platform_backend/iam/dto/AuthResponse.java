package ma.emsi.game_platform_backend.iam.dto;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.Role;

/**
 * Réponse retournée après login/register réussi.
 * Contient le JWT et les informations de l'utilisateur.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String userId;
    private String email;
    private String pseudo;
    private Role role;
    private long expiresIn;    // Durée de validité en ms
}