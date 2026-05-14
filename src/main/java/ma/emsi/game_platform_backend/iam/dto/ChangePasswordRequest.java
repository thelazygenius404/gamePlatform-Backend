package ma.emsi.game_platform_backend.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangePasswordRequest {
    @NotBlank(message = "L'ancien mot de passe est obligatoire.")
    private String oldPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères.")
    private String newPassword;
}