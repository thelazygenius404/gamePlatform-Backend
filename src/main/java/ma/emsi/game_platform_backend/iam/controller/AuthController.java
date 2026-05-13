package ma.emsi.game_platform_backend.iam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.iam.dto.*;
import ma.emsi.game_platform_backend.iam.model.User;
import ma.emsi.game_platform_backend.iam.repository.UserRepository;
import ma.emsi.game_platform_backend.iam.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================
 *  Contrôleur REST : Authentification
 * ============================================================
 *
 * @RestController = @Controller + @ResponseBody
 * Tous les retours sont automatiquement sérialisés en JSON via Jackson.
 *
 * SANS Spring MVC :
 *   On étendrait HttpServlet et implémenterait doPost() manuellement :
 *     protected void doPost(HttpServletRequest req, HttpServletResponse res) {
 *       res.setContentType("application/json");
 *       String body = req.getReader().lines().collect(Collectors.joining());
 *       // Parser JSON manuellement (org.json ou Gson)
 *       // Appeler la logique métier
 *       // Construire la réponse JSON manuellement
 *       res.getWriter().write("{\"token\": \"...\"}");
 *     }
 *
 * AVEC Spring MVC (@RestController) :
 *   - @RequestBody désérialise automatiquement le JSON entrant vers le DTO
 *   - @Valid déclenche la validation Jakarta Bean Validation
 *   - ResponseEntity<T> contrôle le statut HTTP et le body de réponse
 *   - Jackson sérialise automatiquement l'objet Java retourné en JSON
 *   - Gestion des erreurs via @ControllerAdvice (GlobalExceptionHandler)
 * ============================================================
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * POST /api/auth/register
     * Body : { "email": "...", "password": "...", "pseudo": "..." }
     *
     * @Valid déclenche automatiquement la validation des contraintes
     * définies dans RegisterRequest (@NotBlank, @Email, @Pattern...).
     * Si invalide → MethodArgumentNotValidException → 400 Bad Request
     * (géré par GlobalExceptionHandler).
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Body : { "email": "...", "password": "..." }
     *
     * HttpServletRequest injecté par Spring pour extraire IP et User-Agent.
     * SANS Spring : request déjà disponible dans le Servlet, mais pas d'injection.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Header : Authorization: Bearer <token>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie."));
    }

    /**
     * POST /api/auth/forgot-password
     * Body : { "email": "..." }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        // Message générique pour ne pas révéler si l'email existe
        return ResponseEntity.ok(Map.of(
                "message", "Si cet email est associé à un compte, un lien de réinitialisation a été envoyé."
        ));
    }

    /**
     * POST /api/auth/reset-password
     * Body : { "token": "...", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }
    /**
     * GET /api/auth/me
     * Route protégée — retourne les infos de l'utilisateur connecté.
     * Spring Security injecte automatiquement le Principal depuis le SecurityContext.
     * SANS Spring : on ferait request.getAttribute("currentUser") après vérification manuelle du token.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(Map.of(
                "userId",  user.getId(),
                "email",   user.getEmail(),
                "pseudo",  user.getPseudo(),
                "role",    user.getRole(),
                "level",   user.getLevel(),
                "points",  user.getPoints(),
                "status",  user.getStatus()
        ));
    }
}