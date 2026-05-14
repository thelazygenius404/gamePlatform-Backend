package ma.emsi.game_platform_backend.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProfileRequest {
    @NotBlank(message = "Le pseudo est obligatoire.")
    private String pseudo;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    private String email;
}