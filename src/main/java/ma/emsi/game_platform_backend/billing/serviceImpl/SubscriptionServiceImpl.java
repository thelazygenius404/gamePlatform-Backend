package ma.emsi.game_platform_backend.billing.serviceImpl;

import ma.emsi.game_platform_backend.billing.dto.CreateSubscriptionRequest;
import ma.emsi.game_platform_backend.billing.dto.SubscriptionDTO;
import ma.emsi.game_platform_backend.billing.model.PaymentTransaction;
import ma.emsi.game_platform_backend.billing.model.Subscription;
import ma.emsi.game_platform_backend.billing.repository.PaymentTransactionRepository;
import ma.emsi.game_platform_backend.billing.repository.SubscriptionRepository;
import ma.emsi.game_platform_backend.billing.service.SubscriptionService;
import ma.emsi.game_platform_backend.shared.enums.PaymentStatus;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionStatus;
import ma.emsi.game_platform_backend.shared.exception.BusinessException;
import ma.emsi.game_platform_backend.shared.exception.ResourceNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository       subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   PaymentTransactionRepository paymentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository      = paymentRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // Création — statut PENDING (avant confirmation Stripe)
    // ─────────────────────────────────────────────────────────────

    @Override
    public SubscriptionDTO createSubscription(CreateSubscriptionRequest request) {

        // 1 seul abonnement actif par user — index unique MongoDB
        if (subscriptionRepository.existsByUserIdAndStatus(
                request.getUserId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessException(
                    "User already has an active subscription: " + request.getUserId());
        }

        LocalDateTime now     = LocalDateTime.now();

        // Calcul endDate via les données de l'enum — ex: MONTHLY = +30j, YEARLY = +365j
        LocalDateTime endDate = now.plusDays(request.getPlan().getDurationDays());

        Subscription sub = Subscription.builder()
                .userId(request.getUserId())
                .plan(request.getPlan())
                .status(SubscriptionStatus.PENDING)
                .startDate(now)
                .endDate(endDate)
                .stripeSessionId(request.getStripeSessionId())
                .autoRenew(true)
                .updatedAt(now)
                .build();

        Subscription saved = subscriptionRepository.save(sub);

        // Crée la transaction de paiement en PENDING
        createPendingTransaction(saved);

        return toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Activation après confirmation Stripe
    // ─────────────────────────────────────────────────────────────

    @Override
    public SubscriptionDTO activateSubscription(String stripeSessionId) {

        Subscription sub = subscriptionRepository
                .findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for stripeSessionId: " + stripeSessionId));

        if (!sub.isPending()) {
            throw new BusinessException(
                    "Subscription is not in PENDING status: " + sub.getStatus());
        }

        sub.activate();
        subscriptionRepository.save(sub);

        // Confirme la transaction Stripe associée
        paymentRepository.findByStripeSessionId(stripeSessionId)
                .ifPresent(tx -> {
                    tx.confirmPayment();
                    paymentRepository.save(tx);
                });

        return toDTO(sub);
    }

    // ─────────────────────────────────────────────────────────────
    // Annulation — RG-12 : accès maintenu jusqu'à endDate
    // ─────────────────────────────────────────────────────────────

    @Override
    public SubscriptionDTO cancelSubscription(String userId, String reason) {

        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for userId: " + userId));

        if (!sub.isActive()) {
            throw new BusinessException(
                    "Subscription is not active for userId: " + userId);
        }

        sub.cancel(reason);
        subscriptionRepository.save(sub);

        return toDTO(sub);
    }

    // ─────────────────────────────────────────────────────────────
    // Vérification accès premium — RG-10
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean isUserPremium(String userId) {
        return subscriptionRepository
                .findByUserId(userId)
                .map(Subscription::isActive)
                .orElse(false);
    }

    @Override
    public SubscriptionDTO getByUserId(String userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for userId: " + userId));
        return toDTO(sub);
    }

    // ─────────────────────────────────────────────────────────────
    // @Scheduled : expire les abonnements chaque nuit à 2h
    // ─────────────────────────────────────────────────────────────

    @Override
    @Scheduled(cron = "0 0 2 * * *")
    public void processExpirations() {

        List<Subscription> toExpire = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());

        toExpire.forEach(sub -> {
            sub.expire();
            subscriptionRepository.save(sub);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────

    private void createPendingTransaction(Subscription sub) {

        // Le montant vient directement de l'enum — pas de magic number
        double amount = sub.getPlan().getDefaultAmount();

        PaymentTransaction tx = PaymentTransaction.builder()
                .userId(sub.getUserId())
                .subscriptionId(sub.getId())
                .amount(amount)
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .stripeSessionId(sub.getStripeSessionId())
                .build();

        paymentRepository.save(tx);
    }

    private SubscriptionDTO toDTO(Subscription sub) {
        return SubscriptionDTO.builder()
                .id(sub.getId())
                .userId(sub.getUserId())
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .daysRemaining(sub.getDaysRemaining())
                .autoRenew(sub.isAutoRenew())
                .active(sub.isActive())
                .cancelledAt(sub.getCancelledAt())
                .cancelReason(sub.getCancelReason())
                .build();
    }
}