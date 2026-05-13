package ma.emsi.game_platform_backend.iam.service;

import ma.emsi.game_platform_backend.iam.dto.AuthResponse;
import ma.emsi.game_platform_backend.iam.dto.LoginRequest;
import ma.emsi.game_platform_backend.iam.dto.RegisterRequest;

/**
 * Interface du service d'authentification.
 *
 * PATTERN : Interface + Implémentation (principe DIP — Dependency Inversion).
 * SANS Spring : on accèderait directement à la classe concrète.
 * AVEC Spring : injection par interface → facilite les tests (Mock), le remplacement
 * d'implémentation, et respecte les principes SOLID.
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    void logout(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}