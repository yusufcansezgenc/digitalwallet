package com.inghubs.digitalwallet.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.entities.*;
import com.inghubs.digitalwallet.repositories.CustomerRepository;
import com.inghubs.digitalwallet.repositories.WalletRepository;
import com.inghubs.digitalwallet.utilities.constants.WalletConstants;
import com.inghubs.digitalwallet.utilities.enums.*;
import com.inghubs.digitalwallet.utilities.mappers.CreateWalletMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
    private final CreateWalletMapper createWalletMapper;
    private final TransactionService transactionService;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public WalletService(WalletRepository walletRepository,
            CreateWalletMapper createWalletMapper,
            CustomerRepository customerRepository,
            TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.createWalletMapper = createWalletMapper;
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    public ListWalletResponse ListWallets(UUID customerId) {
        logger.info("Listing wallets for customerId: {}", customerId);

        if (!customerRepository.existsById(customerId)) {
            logger.warn("Customer with ID {} not found.", customerId);
            return null;
        }

        List<Wallet> wallets = walletRepository.findByCustomerId(customerId);
        return ListWalletResponse.builder()
                .wallets(wallets)
                .build();
    }

    public CreateWalletResponse CreateWallet(CreateWalletRequest request) {
        logger.info("Creating wallet for customerId: {}", request.getCustomerId());

        if (!customerRepository.existsById(request.getCustomerId())) {
            logger.warn("Customer with ID {} not found.", request.getCustomerId());
            return null;
        }

        Wallet response = walletRepository.save(createWalletMapper.toEntity(request));

        return CreateWalletResponse.builder()
                .wallet(response)
                .build();
    }

    public DepositWalletResponse DepositWallet(DepositWalletRequest request) {
        logger.info("Depositing to walletId: {}", request.getWalletId());
        
        Wallet wallet = walletRepository.findById(request.getWalletId()).orElse(null);
        if (wallet == null) {
            logger.warn("Wallet with ID {} not found.", request.getWalletId());
            return null;
        }

        // TODO: approval stage
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

        Transaction createdTransaction = transactionService.CreateTransaction(transaction);
        Wallet affectedWallet = walletRepository.findById(request.getWalletId()).orElse(null);

        return DepositWalletResponse.builder()
                .transaction(createdTransaction)
                .wallet(affectedWallet)
                .isPendingTransaction(transactionStatus == TransactionStatus.PENDING)
                .build();
    }

    public WithdrawWalletResponse WithdrawWallet(WithdrawWalletRequest request) {
        logger.info("Withdrawing from walletId: {}", request.getWalletId());

        Wallet wallet = walletRepository.findById(request.getWalletId()).orElse(null);
        if (wallet == null) {
            logger.warn("Wallet with ID {} not found.", request.getWalletId());
            return null;
        }

        // TODO: approval stage
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

        Transaction createdTransaction = transactionService.CreateTransaction(transaction);
        Wallet affectedWallet = walletRepository.findById(request.getWalletId()).orElse(null);

        return WithdrawWalletResponse.builder()
                .transaction(createdTransaction)
                .wallet(affectedWallet)
                .isPendingTransaction(transactionStatus == TransactionStatus.PENDING)
                .build();
    }
}
