package ma.emsi.game_platform_backend.shared.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Structure de réponse uniforme
    public record ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            List<String> details
    ) {}

    // 1. Ressource non trouvée (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    // 2. Erreurs métier (400)
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBusiness(Exception ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    // 3. Validation @Valid (400) - UNIQUE MÉTHODE
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation échouée", request, details);
    }

    // 4. Validation de contraintes JPA/Hibernate (400)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Contrainte violée", request, details);
    }

    // 5. Sécurité : Mauvaises credentials (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiants incorrects", request, List.of());
    }

    // 6. Sécurité : Compte verrouillé (423)
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLocked(LockedException ex, WebRequest request) {
        return build(HttpStatus.LOCKED, "Compte verrouillé", request, List.of());
    }

    // 7. Sécurité : Compte désactivé (403)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Compte désactivé", request, List.of());
    }

    // 8. Erreur générique (500) - UNIQUE MÉTHODE
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne : " + ex.getMessage(), request, List.of());
    }

    // Helper pour construire la réponse
    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request, List<String> details) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", ""),
                details
        );
        return ResponseEntity.status(status).body(error);
    }
}