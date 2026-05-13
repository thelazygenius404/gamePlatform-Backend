
// ════════════════════════════════════════════════════════════════
// PaymentTransactionService.java
// ════════════════════════════════════════════════════════════════
package ma.emsi.game_platform_backend.billing.service;

import ma.emsi.game_platform_backend.billing.dto.PaymentTransactionDTO;

import java.util.List;

public interface PaymentTransactionService {

    /** Marque une transaction comme PAID. */
    PaymentTransactionDTO confirmPayment(String stripeSessionId);

    /** Marque une transaction comme FAILED. */
    PaymentTransactionDTO handleFailure(String stripeSessionId, String reason);

    /** Rembourse une transaction PAID → REFUNDED. */
    PaymentTransactionDTO refund(String stripeSessionId);

    /** Historique des paiements d'un utilisateur. */
    List<PaymentTransactionDTO> getHistoryByUserId(String userId);
}