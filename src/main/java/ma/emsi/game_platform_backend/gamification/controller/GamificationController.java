package ma.emsi.game_platform_backend.gamification.controller;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.gamification.dto.GamificationStatsDTO;
import ma.emsi.game_platform_backend.gamification.dto.NotificationDTO;
import ma.emsi.game_platform_backend.gamification.model.Badge;
import ma.emsi.game_platform_backend.gamification.model.Notification;
import ma.emsi.game_platform_backend.gamification.repository.BadgeRepository;
import ma.emsi.game_platform_backend.gamification.service.GamificationService;
import ma.emsi.game_platform_backend.gamification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoints Gamification :
 *
 * GET  /api/gamification/stats/{userId}          → points, niveau, badges, progression
 * GET  /api/gamification/badges                  → tous les badges disponibles
 * GET  /api/gamification/notifications/{userId}  → notifications non lues
 * PUT  /api/gamification/notifications/{userId}/read → marque tout comme lu
 */
@RestController
@RequestMapping("/api/gamification")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final BadgeRepository     badgeRepository;

    // ── GET /api/gamification/stats/{userId} ──────────────────────
    @GetMapping("/stats/{userId}")
    public ResponseEntity<GamificationStatsDTO> getStats(@PathVariable String userId) {
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    // ── GET /api/gamification/badges ──────────────────────────────
    @GetMapping("/badges")
    public ResponseEntity<List<Badge>> getAllBadges() {
        return ResponseEntity.ok(badgeRepository.findByIsActiveTrue());
    }

    // ── GET /api/gamification/notifications/{userId} ──────────────
    @GetMapping("/notifications/{userId}")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @PathVariable String userId) {
        List<NotificationDTO> dtos = notificationService.getUnread(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ── PUT /api/gamification/notifications/{userId}/read ─────────
    @PutMapping("/notifications/{userId}/read")
    public ResponseEntity<Void> markAllRead(@PathVariable String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedId(n.getRelatedId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}