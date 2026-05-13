package ma.emsi.game_platform_backend.billing.serviceImpl;

import ma.emsi.game_platform_backend.billing.dto.PaymentTransactionDTO;
import ma.emsi.game_platform_backend.billing.model.PaymentTransaction;
import ma.emsi.game_platform_backend.billing.repository.PaymentTransactionRepository;
import ma.emsi.game_platform_backend.billing.service.PaymentTransactionService;
import ma.emsi.game_platform_backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionRepository paymentRepository;

    public PaymentTransactionServiceImpl(PaymentTransactionRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // Confirmation du paiement
    // ─────────────────────────────────────────────────────────────

    @Override
    public PaymentTransactionDTO confirmPayment(String stripeSessionId) {
        PaymentTransaction tx = findOrThrow(stripeSessionId);
        tx.confirmPayment();
        return toDTO(paymentRepository.save(tx));
    }

    // ─────────────────────────────────────────────────────────────
    // Échec de paiement
    // ─────────────────────────────────────────────────────────────

    @Override
    public PaymentTransactionDTO handleFailure(String stripeSessionId, String reason) {
        PaymentTransaction tx = findOrThrow(stripeSessionId);
        tx.markFailed(reason);
        return toDTO(paymentRepository.save(tx));
    }

    // ─────────────────────────────────────────────────────────────
    // Remboursement
    // ─────────────────────────────────────────────────────────────

    @Override
    public PaymentTransactionDTO refund(String stripeSessionId) {
        PaymentTransaction tx = findOrThrow(stripeSessionId);
        tx.refund(); // lève IllegalStateException si pas PAID
        return toDTO(paymentRepository.save(tx));
    }

    // ─────────────────────────────────────────────────────────────
    // Historique
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<PaymentTransactionDTO> getHistoryByUserId(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────

    private PaymentTransaction findOrThrow(String stripeSessionId) {
        return paymentRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No transaction found for stripeSessionId: " + stripeSessionId));
    }

    private PaymentTransactionDTO toDTO(PaymentTransaction tx) {
        return PaymentTransactionDTO.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .subscriptionId(tx.getSubscriptionId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .stripeSessionId(tx.getStripeSessionId())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}