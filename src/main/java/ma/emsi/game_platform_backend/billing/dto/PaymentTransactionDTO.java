
// ════════════════════════════════════════════════════════════════
// PaymentTransactionDTO.java
// ════════════════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.billing.dto;

import lombok.*;
        import ma.emsi.game_platform_backend.shared.enums.PaymentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionDTO {
    private String        id;
    private String        userId;
    private String        subscriptionId;
    private double        amount;
    private String        currency;
    private PaymentStatus status;
    private String        stripeSessionId;
    private String        failureReason;
    private LocalDateTime createdAt;
}