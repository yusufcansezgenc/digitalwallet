package com.inghubs.digitalwallet.services;

import java.util.UUID;

import com.inghubs.digitalwallet.dtos.requests.ApproveTransactionRequest;
import com.inghubs.digitalwallet.dtos.responses.ApproveTransactionResponse;
import com.inghubs.digitalwallet.dtos.responses.ListTransactionsResponse;
import com.inghubs.digitalwallet.entities.Transaction;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

public interface TransactionService {
    Transaction CreateTransaction(Transaction transaction);
    ApproveTransactionResponse ApproveTransaction(ApproveTransactionRequest request, CustomUserDetails userDetails);
    ListTransactionsResponse ListTransactions(UUID walletId, CustomUserDetails userDetails);
    ListTransactionsResponse ListTransactions(CustomUserDetails userDetails);
}
