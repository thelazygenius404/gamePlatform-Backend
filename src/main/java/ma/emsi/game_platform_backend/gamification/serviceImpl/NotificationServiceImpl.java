package ma.emsi.game_platform_backend.gamification.serviceImpl;

import lombok.RequiredArgsConstructor;
import ma.emsi.game_platform_backend.gamification.model.Notification;
import ma.emsi.game_platform_backend.gamification.repository.NotificationRepository;
import ma.emsi.game_platform_backend.gamification.service.NotificationService;
import ma.emsi.game_platform_backend.shared.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void send(String userId, NotificationType type,
                     String title, String message, String relatedId) {
        Notification notif = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(notif);
    }

    @Override
    public List<Notification> getUnread(String userId) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }

    @Override
    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}