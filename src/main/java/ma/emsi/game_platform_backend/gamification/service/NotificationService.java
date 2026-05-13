

// ═══════════════════════════════════════════════════════
// NotificationService.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.service;

import ma.emsi.game_platform_backend.gamification.model.Notification;
import ma.emsi.game_platform_backend.shared.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    /** Crée et sauvegarde une notification. */
    void send(String userId, NotificationType type,
              String title, String message, String relatedId);

    /** Toutes les notifications non lues d'un utilisateur. */
    List<Notification> getUnread(String userId);

    /** Marque toutes les notifications d'un user comme lues. */
    void markAllAsRead(String userId);

    /** Compte les notifications non lues. */
    long countUnread(String userId);
}