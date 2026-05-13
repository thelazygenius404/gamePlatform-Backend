package ma.emsi.game_platform_backend.shared.enums;

public enum SubscriptionPlan {
    MONTHLY(30, 9.99),
    YEARLY(365, 99.99);

    private final int durationDays;
    private final double defaultAmount;

    SubscriptionPlan(int durationDays, double defaultAmount) {
        this.durationDays = durationDays;
        this.defaultAmount = defaultAmount;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public double getDefaultAmount() {
        return defaultAmount;
    }
}