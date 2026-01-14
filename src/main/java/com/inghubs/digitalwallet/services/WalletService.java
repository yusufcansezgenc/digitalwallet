package com.inghubs.digitalwallet.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.entities.*;
import com.inghubs.digitalwallet.repositories.*;
import com.inghubs.digitalwallet.utilities.constants.WalletConstants;
import com.inghubs.digitalwallet.utilities.enums.*;
import com.inghubs.digitalwallet.utilities.exceptions.*;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private TransactionService transactionService;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public ListWalletResponse ListWallets(UUID customerId) {
        logger.info("Listing wallets for customerId: {}", customerId);

        if (!customerRepository.existsById(customerId)) {
            logger.warn("Customer with ID {} not found.", customerId);
            throw new NotFoundException("Customer not found.");
        }

        List<Wallet> wallets = walletRepository.findByCustomerId(customerId);
        return ListWalletResponse.builder()
                .wallets(wallets)
                .build();
    }

    public CreateWalletResponse CreateWallet(CreateWalletRequest request, CustomUserDetails userDetails) {
        logger.info("Creating wallet for customerId: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        if (customer == null) {
            logger.warn("Customer with ID {} not found.", request.getCustomerId());
            throw new NotFoundException("Customer not found.");
        }

        if (request.getCustomerId() != null
                && !request.getCustomerId().equals(userDetails.getCustomerId())
                && !(userDetails.getRole() == Role.EMPLOYEE)) {
            logger.warn("User with ID {} is not authorized to create wallet for customer ID {}.", userDetails.getId(),
                    request.getCustomerId());
            throw new SecurityException("Not authorized to create wallet for this customer.");
        }

        Wallet wallet = Wallet.builder()
                .customer(customer)
                .walletName(request.getWalletName())
                .currency(request.getCurrency())
                .isActiveWithdraw(request.getIsActiveWithdraw())
                .isActiveShopping(request.getIsActiveShopping())
                .build();

        Wallet response = walletRepository.save(wallet);

        return CreateWalletResponse.builder()
                .wallet(response)
                .build();
    }

    public DepositWalletResponse DepositWallet(DepositWalletRequest request) {
        logger.info("Depositing to walletId: {}", request.getWalletId());

        Wallet wallet = walletRepository.findById(request.getWalletId()).orElse(null);
        if (wallet == null) {
            logger.warn("Wallet with ID {} not found.", request.getWalletId());
            throw new NotFoundException("Wallet not found.");
        }

        TransactionStatus transactionStatus = TransactionStatus.APPROVED;
        if (request.getAmount() > WalletConstants.AMOUNT_LIMIT) {
            transactionStatus = TransactionStatus.PENDING;
        }

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(TransactionType.DEPOSIT)
                .status(transactionStatus)
                .wallet(wallet)
                .oppositeParty(request.getSource())
                .oppositePartyType(request.getOppositePartyType())
                .build();

        this.ValidateWalletProcessing(wallet, transaction);

        Transaction createdTransaction = transactionService.CreateTransaction(transaction);
        Wallet affectedWallet = walletRepository.findById(request.getWalletId()).orElse(null);

        return DepositWalletResponse.builder()
                .transaction(createdTransaction)
                .wallet(affectedWallet)
                .isPendingTransaction(transactionStatus == TransactionStatus.PENDING)
                .build();
    }

    public WithdrawWalletResponse WithdrawWallet(WithdrawWalletRequest request, CustomUserDetails userDetails) {
        logger.info("Withdrawing from walletId: {}", request.getWalletId());

        Wallet wallet = walletRepository.findById(request.getWalletId()).orElse(null);
        if (wallet == null) {
            logger.warn("Wallet with ID {} not found.", request.getWalletId());
            throw new NotFoundException("No wallet found for the customer.");
        }

        if (!wallet.getCustomer().getId().equals(userDetails.getCustomerId())
                && !(userDetails.getRole() == Role.EMPLOYEE)) {
            logger.warn("User with User ID {} is not authorized to withdraw from wallet ID {}.", userDetails.getId(),
                    wallet.getId());
            throw new SecurityException("Not authorized to withdraw from this wallet.");
        }

        TransactionStatus transactionStatus = TransactionStatus.APPROVED;
        if (request.getAmount() > WalletConstants.AMOUNT_LIMIT) {
            transactionStatus = TransactionStatus.PENDING;
        }

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(TransactionType.WITHDRAW)
                .status(transactionStatus)
                .wallet(wallet)
                .oppositeParty(request.getDestination())
                .oppositePartyType(request.getOppositePartyType())
                .build();

        this.ValidateWalletProcessing(wallet, transaction);

        Transaction createdTransaction = transactionService.CreateTransaction(transaction);
        Wallet affectedWallet = walletRepository.findById(request.getWalletId()).orElse(null);

        return WithdrawWalletResponse.builder()
                .transaction(createdTransaction)
                .wallet(affectedWallet)
                .isPendingTransaction(transactionStatus == TransactionStatus.PENDING)
                .build();
    }

    private void ValidateWalletProcessing(Wallet wallet, Transaction transaction) {

        logger.info("Validating wallet ID {} for transaction ID {}", wallet.getId(), transaction.getId());

        if (transaction.getType() == TransactionType.WITHDRAW && !wallet.getIsActiveWithdraw()) {
            logger.info("Wallet ID {} with Transaction ID {} is not authorized for withdrawals.", wallet.getId(),
                    transaction.getId());
            throw new WithdrawalDeniedException("This wallet is not authorized for withdrawals.");
        }

        if ((transaction.getOppositePartyType() == OppositePartyType.PAYMENT
                && transaction.getType() == TransactionType.WITHDRAW)
                && !wallet.getIsActiveShopping()) {
            logger.info("Wallet ID {} with Transaction ID {} is not authorized for shopping payments.", wallet.getId(),
                    transaction.getId());
            throw new WithdrawalDeniedException("This wallet is not authorized for shopping payments.");
        }
    }
}
