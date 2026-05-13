
// ═══════════════════════════════════════════════════════
// NotificationRepository.java
// ═══════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.gamification.repository;

import ma.emsi.game_platform_backend.gamification.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsReadFalse(String userId);
}