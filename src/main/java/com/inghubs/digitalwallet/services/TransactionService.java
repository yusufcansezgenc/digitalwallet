package com.inghubs.digitalwallet.services;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.inghubs.digitalwallet.dtos.requests.ApproveTransactionRequest;
import com.inghubs.digitalwallet.dtos.responses.ApproveTransactionResponse;
import com.inghubs.digitalwallet.dtos.responses.ListTransactionsResponse;
import com.inghubs.digitalwallet.entities.Transaction;
import com.inghubs.digitalwallet.entities.Wallet;
import com.inghubs.digitalwallet.repositories.TransactionRepository;
import com.inghubs.digitalwallet.repositories.WalletRepository;
import com.inghubs.digitalwallet.utilities.enums.BalanceOperation;
import com.inghubs.digitalwallet.utilities.enums.TransactionStatus;
import com.inghubs.digitalwallet.utilities.enums.TransactionType;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository,
            WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public Transaction CreateTransaction(Transaction transaction) {
        logger.info("Creating transaction for walletId: {}", transaction.getWallet().getId());

        if (!walletRepository.existsById(transaction.getWallet().getId())) {
            logger.warn("Wallet with ID {} not found.", transaction.getWallet().getId());
            return null;
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction created with ID: {}", savedTransaction.getId());

        this.CompletePendingTransaction(savedTransaction);
        return savedTransaction;
    }

    public ApproveTransactionResponse ApproveTransaction(ApproveTransactionRequest request) {
        logger.info("Approving transaction with transactionId: {}", request.getTransactionId());

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElse(null);

        if (transaction == null) {
            logger.warn("Transaction with ID {} not found.", request.getTransactionId());
            return null;
        }

        if (request.getStatus() == TransactionStatus.APPROVED) {
            logger.info("Completing transaction with transactionId: {}", request.getTransactionId());
            this.CompleteTransaction(transaction);
        } else if (request.getStatus() == TransactionStatus.DENIED) {
            this.RevertPendingTransaction(transaction);
        }

        logger.info("Updating status for transactionId: {}", request.getTransactionId());
        transaction.setStatus(request.getStatus());
        transaction = transactionRepository.save(transaction);

        return ApproveTransactionResponse.builder()
                .transaction(transaction)
                .build();
    }

    public ListTransactionsResponse ListTransactions(UUID walletId) {
        logger.info("Listing transactions for walletId: {}", walletId);

        if (!walletRepository.existsById(walletId)) {
            logger.warn("Wallet with ID {} not found.", walletId);
            return null;
        }

        return ListTransactionsResponse.builder()
                .transactions(transactionRepository.findByWalletId(walletId))
                .build();
    }

    private Wallet CompletePendingTransaction(Transaction transaction) {
        return this.updateWalletBalance(transaction,
                getBalanceOperation("COMPLETE_PENDING", transaction.getType()));
    }

    private Wallet RevertPendingTransaction(Transaction transaction) {
        return this.updateWalletBalance(transaction,
                getBalanceOperation("REVERT_PENDING", transaction.getType()));
    }

    private Wallet CompleteTransaction(Transaction transaction) {
        return this.updateWalletBalance(transaction,
                getBalanceOperation("COMPLETE_APPROVED", transaction.getType()));
    }

    private BalanceOperation getBalanceOperation(String operationCategory, TransactionType type) {
        return switch (operationCategory) {
            case "COMPLETE_PENDING" -> type == TransactionType.DEPOSIT ? BalanceOperation.COMPLETE_PENDING_DEPOSIT
                    : BalanceOperation.COMPLETE_PENDING_WITHDRAW;
            case "REVERT_PENDING" -> type == TransactionType.DEPOSIT ? BalanceOperation.REVERT_PENDING_DEPOSIT
                    : BalanceOperation.REVERT_PENDING_WITHDRAW;
            case "COMPLETE_APPROVED" -> type == TransactionType.DEPOSIT ? BalanceOperation.COMPLETE_APPROVED_DEPOSIT
                    : BalanceOperation.COMPLETE_APPROVED_WITHDRAW;
            default -> throw new IllegalArgumentException("Unknown operation category: " + operationCategory);
        };
    }

    private Wallet updateWalletBalance(Transaction transaction, BalanceOperation operation) {
        Wallet affectedWallet = walletRepository.findById(transaction.getWallet().getId())
                .orElse(null);

        if (affectedWallet == null) {
            logger.warn("Wallet with ID {} not found.", transaction.getWallet().getId());
            return null;
        }

        logger.info(operation.getLogMessage(), transaction.getWallet().getId());

        Double amount = transaction.getAmount();

        switch (operation) {
            case COMPLETE_PENDING_DEPOSIT -> affectedWallet.setBalance(affectedWallet.getBalance() + amount);
            case COMPLETE_PENDING_WITHDRAW -> affectedWallet.setBalance(affectedWallet.getBalance() - amount);
            case REVERT_PENDING_DEPOSIT -> affectedWallet.setBalance(affectedWallet.getBalance() - amount);
            case REVERT_PENDING_WITHDRAW -> affectedWallet.setBalance(affectedWallet.getBalance() + amount);
            case COMPLETE_APPROVED_DEPOSIT -> {
                Double newBalance = affectedWallet.getUsableBalance() + amount;
                affectedWallet.setBalance(newBalance);
                affectedWallet.setUsableBalance(newBalance);
            }
            case COMPLETE_APPROVED_WITHDRAW -> {
                Double newBalance = affectedWallet.getUsableBalance() - amount;
                affectedWallet.setBalance(newBalance);
                affectedWallet.setUsableBalance(newBalance);
            }
        }

        return walletRepository.save(affectedWallet);
    }

}
