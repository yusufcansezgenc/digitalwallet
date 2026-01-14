package com.inghubs.digitalwallet.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.entities.*;
import com.inghubs.digitalwallet.repositories.*;
import com.inghubs.digitalwallet.utilities.enums.*;
import com.inghubs.digitalwallet.utilities.exceptions.NotFoundException;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private WalletRepository walletRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Override
    public Transaction CreateTransaction(Transaction transaction) {
        logger.info("Creating transaction for walletId: {}", transaction.getWallet().getId());

        if (!walletRepository.existsById(transaction.getWallet().getId())) {
            logger.warn("Wallet with ID {} not found.", transaction.getWallet().getId());
            throw new NotFoundException("Wallet not found.");
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction created with ID: {}", savedTransaction.getId());

        if (savedTransaction.getStatus() == TransactionStatus.APPROVED) {
            this.CompleteTransaction(savedTransaction);
        } else if (savedTransaction.getStatus() == TransactionStatus.PENDING) {
            this.CompletePendingTransaction(savedTransaction);
        }
        return savedTransaction;
    }

    @Override
    public ApproveTransactionResponse ApproveTransaction(ApproveTransactionRequest request,
            CustomUserDetails userDetails) {
        logger.info("Approving transaction with transactionId: {}", request.getTransactionId());

        if (userDetails.getRole() != Role.EMPLOYEE) {
            logger.warn("User with ID {} is not authorized to approve/reject transactions.", userDetails.getId());
            throw new SecurityException("Not authorized to approve/reject transactions. Must be an employee.");
        }

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElse(null);

        if (transaction == null) {
            logger.warn("Transaction with ID {} not found.", request.getTransactionId());
            throw new NotFoundException("Transaction not found.");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            logger.warn("Transaction with ID {} is not in PENDING status.", request.getTransactionId());
            throw new IllegalStateException("Only PENDING transactions can be approved or denied.");
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

        Wallet updatedWallet = walletRepository.findById(transaction.getWallet().getId())
                .orElse(null);

        return ApproveTransactionResponse.builder()
                .transaction(transaction)
                .wallet(updatedWallet)
                .build();
    }

    @Override
    public ListTransactionsResponse ListTransactions(UUID walletId, CustomUserDetails userDetails) {
        logger.info("Listing transactions for walletId: {}", walletId);

        if (!walletRepository.existsById(walletId)) {
            logger.warn("Wallet with ID {} not found.", walletId);
            throw new NotFoundException("Wallet not found.");
        }

        List<Wallet> userWallets = walletRepository.findByCustomerId(userDetails.getCustomerId());
        boolean ownsWallet = userWallets.stream()
                .anyMatch(wallet -> wallet.getId().equals(walletId));

        if (!ownsWallet && userDetails.getRole() != Role.EMPLOYEE) {
            logger.warn("User with ID {} is not authorized to access transactions of wallet ID {}.",
                    userDetails.getId(), walletId);
            throw new SecurityException("Not authorized to access transactions for this wallet.");
        }

        return ListTransactionsResponse.builder()
                .transactions(transactionRepository.findByWalletId(walletId))
                .build();
    }

    @Override
    public ListTransactionsResponse ListTransactions(CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.EMPLOYEE) {
            logger.warn("User with ID {} is not authorized to list all transactions.", userDetails.getId());
            throw new SecurityException("Not authorized to list all transactions. Must be an employee.");
        }

        logger.info("Listing all transactions");

        return ListTransactionsResponse.builder()
                .transactions(transactionRepository.findAll())
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
                affectedWallet.setUsableBalance(affectedWallet.getUsableBalance() + amount);
                affectedWallet.setBalance(this.calculateFinalBalance(affectedWallet, transaction));
            }
            case COMPLETE_APPROVED_WITHDRAW -> {
                affectedWallet.setUsableBalance(affectedWallet.getUsableBalance() - amount);
                affectedWallet.setBalance(this.calculateFinalBalance(affectedWallet, transaction));
            }
        }

        return walletRepository.save(affectedWallet);
    }

    private Double calculateFinalBalance(Wallet wallet, Transaction completedTransaction) {
        logger.info("Syncing balance for wallet ID {}", wallet.getId());
        Double finalBalance = 0.0;
        List<Transaction> pendingTransactions = transactionRepository.findByWalletIdAndStatus(wallet.getId(),
                TransactionStatus.PENDING);

        for (Transaction transaction : pendingTransactions) {
            if (!transaction.equals(completedTransaction)) {
                if (transaction.getType() == TransactionType.DEPOSIT) {
                    finalBalance += transaction.getAmount();
                } else if (transaction.getType() == TransactionType.WITHDRAW) {
                    finalBalance -= transaction.getAmount();
                }
            }
        }
        return finalBalance + wallet.getUsableBalance();
    }

}
