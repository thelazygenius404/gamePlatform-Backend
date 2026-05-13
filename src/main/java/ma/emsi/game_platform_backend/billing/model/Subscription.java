package ma.emsi.game_platform_backend.billing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionPlan;
import ma.emsi.game_platform_backend.shared.enums.SubscriptionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "subscriptions")
public class Subscription {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private SubscriptionPlan   plan;
    private SubscriptionStatus status;
    private LocalDateTime      startDate;
    private LocalDateTime      endDate;

    @Indexed(sparse = true)
    private String stripeSessionId;

    @Builder.Default
    @Field("autoRenew")
    @JsonProperty("autoRenew")
    private boolean autoRenew = true;

    private LocalDateTime cancelledAt;
    private String        cancelReason;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        boolean statusOk = SubscriptionStatus.ACTIVE.equals(this.status)
                || SubscriptionStatus.CANCELLED.equals(this.status);
        boolean dateOk   = this.endDate != null && this.endDate.isAfter(LocalDateTime.now());
        return statusOk && dateOk;
    }

    public long getDaysRemaining() {
        if (this.endDate == null) return 0L;
        return Math.max(ChronoUnit.DAYS.between(LocalDateTime.now(), this.endDate), 0L);
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.autoRenew = false;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.autoRenew = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return SubscriptionStatus.PENDING.equals(this.status);
    }
}