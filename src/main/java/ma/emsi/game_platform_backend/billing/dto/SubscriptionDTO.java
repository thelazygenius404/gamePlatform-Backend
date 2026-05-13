// ════════════════════════════════════════════════════════════════
// SubscriptionDTO.java
// ════════════════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.billing.dto;

import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionPlan;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDTO {
    private String             id;
    private String             userId;
    private SubscriptionPlan   plan;
    private SubscriptionStatus status;
    private LocalDateTime      startDate;
    private LocalDateTime      endDate;
    private long               daysRemaining;
    private boolean            autoRenew;
    private boolean            active;
    private LocalDateTime      cancelledAt;
    private String             cancelReason;
}