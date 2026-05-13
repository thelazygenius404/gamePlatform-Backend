package ma.emsi.game_platform_backend.billing.repository;

import ma.emsi.game_platform_backend.billing.model.PaymentTransaction;
import ma.emsi.game_platform_backend.shared.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

    /** Idempotence : vérifie si une transaction existe déjà pour cette session. */
    Optional<PaymentTransaction> findByStripeSessionId(String stripeSessionId);

    boolean existsByStripeSessionId(String stripeSessionId);

    /** Historique des paiements d'un utilisateur, du plus récent. */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Toutes les transactions liées à un abonnement. */
    List<PaymentTransaction> findBySubscriptionId(String subscriptionId);

    /** Transactions par statut (utile pour le dashboard admin). */
    List<PaymentTransaction> findByStatus(PaymentStatus status);
}