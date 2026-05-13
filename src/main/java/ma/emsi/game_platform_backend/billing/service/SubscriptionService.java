// ════════════════════════════════════════════════════════════════
// SubscriptionService.java
// ════════════════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.billing.service;

import ma.emsi.game_platform_backend.billing.dto.CreateSubscriptionRequest;
import ma.emsi.game_platform_backend.billing.dto.SubscriptionDTO;

public interface SubscriptionService {

    /** Crée un abonnement en statut PENDING + transaction de paiement associée. */
    SubscriptionDTO createSubscription(CreateSubscriptionRequest request);

    /** Active l'abonnement après confirmation du paiement Stripe. */
    SubscriptionDTO activateSubscription(String stripeSessionId);

    /**
     * RG-12 : annule l'abonnement.
     * L'accès premium est maintenu jusqu'à endDate.
     */
    SubscriptionDTO cancelSubscription(String userId, String reason);

    /** RG-10 : vérifie si l'utilisateur a un accès premium valide. */
    boolean isUserPremium(String userId);

    /** Récupère l'abonnement d'un utilisateur. */
    SubscriptionDTO getByUserId(String userId);

    /** @Scheduled : expire les abonnements dont endDate est dépassée. */
    void processExpirations();
}

