package ma.emsi.game_platform_backend.billing.controller;

import jakarta.validation.Valid;
import ma.emsi.game_platform_backend.billing.dto.CreateSubscriptionRequest;
import ma.emsi.game_platform_backend.billing.dto.PaymentTransactionDTO;
import ma.emsi.game_platform_backend.billing.dto.SubscriptionDTO;
import ma.emsi.game_platform_backend.billing.service.PaymentTransactionService;
import ma.emsi.game_platform_backend.billing.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints Billing :
 *
 * POST   /api/subscriptions                         → créer un abonnement
 * POST   /api/subscriptions/activate/{sessionId}    → activer (confirmation Stripe)
 * POST   /api/subscriptions/cancel/{userId}         → annuler
 * GET    /api/subscriptions/{userId}                → abonnement d'un user
 * GET    /api/subscriptions/{userId}/premium        → vérifier accès premium
 * GET    /api/subscriptions/{userId}/payments       → historique paiements
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService       subscriptionService;
    private final PaymentTransactionService paymentService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  PaymentTransactionService paymentService) {
        this.subscriptionService = subscriptionService;
        this.paymentService      = paymentService;
    }

    // ── POST /api/subscriptions ───────────────────────────────────
    @PostMapping
    public ResponseEntity<SubscriptionDTO> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionDTO sub = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sub);
    }

    // ── POST /api/subscriptions/activate/{stripeSessionId} ────────
    @PostMapping("/activate/{stripeSessionId}")
    public ResponseEntity<SubscriptionDTO> activate(
            @PathVariable String stripeSessionId) {
        return ResponseEntity.ok(subscriptionService.activateSubscription(stripeSessionId));
    }

    // ── POST /api/subscriptions/cancel/{userId} ───────────────────
    @PostMapping("/cancel/{userId}")
    public ResponseEntity<SubscriptionDTO> cancel(
            @PathVariable String userId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId, reason));
    }

    // ── GET /api/subscriptions/{userId} ──────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<SubscriptionDTO> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.getByUserId(userId));
    }

    // ── GET /api/subscriptions/{userId}/premium ───────────────────
    @GetMapping("/{userId}/premium")
    public ResponseEntity<Boolean> isPremium(@PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.isUserPremium(userId));
    }

    // ── GET /api/subscriptions/{userId}/payments ──────────────────
    @GetMapping("/{userId}/payments")
    public ResponseEntity<List<PaymentTransactionDTO>> getPaymentHistory(
            @PathVariable String userId) {
        return ResponseEntity.ok(paymentService.getHistoryByUserId(userId));
    }
}