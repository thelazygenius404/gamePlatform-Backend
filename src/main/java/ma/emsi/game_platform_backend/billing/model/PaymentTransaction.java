package ma.emsi.game_platform_backend.billing.model;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.PaymentStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Trace chaque tentative de paiement Stripe.
 *
 * Idempotence : @Indexed(unique=true) sur stripeSessionId
 * → une seule transaction par session Stripe, pas de double débit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payment_transactions")
public class PaymentTransaction {

    @Id
    private String id;

    /** FK → users._id */
    @Indexed
    private String userId;

    /** FK → subscriptions._id */
    @Indexed
    private String subscriptionId;

    private double amount;

    @Builder.Default
    private String currency = "EUR";

    private PaymentStatus status;

    /**
     * Unique : 1 transaction par session Stripe.
     * Garantit l'idempotence côté base.
     */
    @Indexed(unique = true)
    private String stripeSessionId;

    private String failureReason;

    @CreatedDate
    private LocalDateTime createdAt;

    // ── Méthodes métier ────────────────────────────────────────────

    public void confirmPayment() {
        this.status = PaymentStatus.PAID;
    }

    public void markFailed(String reason) {
        this.status        = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void refund() {
        if (!PaymentStatus.PAID.equals(this.status)) {
            throw new IllegalStateException(
                    "Seule une transaction PAID peut être remboursée.");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public boolean isPaid()   { return PaymentStatus.PAID.equals(this.status); }
    public boolean isFailed() { return PaymentStatus.FAILED.equals(this.status); }
}