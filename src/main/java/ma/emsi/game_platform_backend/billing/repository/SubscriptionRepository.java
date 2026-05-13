package ma.emsi.game_platform_backend.billing.repository;

import ma.emsi.game_platform_backend.billing.model.Subscription;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    /** Abonnement d'un utilisateur (unique par index). */
    Optional<Subscription> findByUserId(String userId);

    /** Vérifie si un abonnement actif existe pour un user. */
    boolean existsByUserIdAndStatus(String userId, SubscriptionStatus status);

    /** Retrouve un abonnement par session Stripe. */
    Optional<Subscription> findByStripeSessionId(String stripeSessionId);

    /**
     * @Scheduled job : abonnements ACTIVE dont endDate est dépassée.
     * Permet d'expirer automatiquement les abonnements chaque nuit.
     */
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime date);

}