package ma.emsi.game_platform_backend.iam.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO (Data Transfer Object) pour l'inscription.
 *
 * SANS Spring Validation :
 *   Validation manuelle dans le Servlet :
 *     if (email == null || !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
 *       throw new IllegalArgumentException("Email invalide");
 *     }
 *   Répété dans chaque méthode de service → duplication massive.
 *
 * AVEC Spring Boot Validation (@Valid + annotations JSR-380) :
 *   Les contraintes déclarées ici sont vérifiées automatiquement quand
 *   @Valid est utilisé dans @RequestBody du contrôleur.
 *   Un @ControllerAdvice centralisé gère toutes les MethodArgumentNotValidException.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /** RG-01 : email valide et unique. */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    /**
     * RG-02 : mot de passe ≥ 8 caractères, 1 majuscule, 1 chiffre, 1 caractère spécial.
     * Regex expliquée :
     *   (?=.*[A-Z])    → au moins une majuscule
     *   (?=.*[0-9])    → au moins un chiffre
     *   (?=.*[@#$%^&+=!]) → au moins un caractère spécial
     *   .{8,}          → minimum 8 caractères
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial"
    )
    private String password;

    @NotBlank(message = "Le pseudo est obligatoire")
    @Size(min = 3, max = 30, message = "Le pseudo doit avoir entre 3 et 30 caractères")
    private String pseudo;
}