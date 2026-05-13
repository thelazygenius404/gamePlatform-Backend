


// ════════════════════════════════════════════════════════════════
// CreateSubscriptionRequest.java
// ════════════════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionPlan;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "plan is required")
    private SubscriptionPlan plan;

    /** ID de session fourni par Stripe simulé (peut être null au départ). */
    private String stripeSessionId;
}

