package com.inghubs.digitalwallet.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.services.TransactionService;
import com.inghubs.digitalwallet.utilities.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "Transaction API", description = "Operations related to transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Operation(summary = "List transactions for a wallet", description = "Lists all transactions for a specific wallet.")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully.")
    @ApiResponse(responseCode = "403", description = "This customer is not authorized to perform this action.")
    @ApiResponse(responseCode = "404", description = "Wallet not found.")
    @GetMapping("/{walletId}")
    public ResponseEntity<ListTransactionsResponse> listTransactions(@PathVariable UUID walletId, Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        ListTransactionsResponse response = transactionService.ListTransactions(walletId, userDetails);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all transactions", description = "Lists all transactions.")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully.")
    @ApiResponse(responseCode = "403", description = "This customer is not authorized to perform this action.")
    @GetMapping
    public ResponseEntity<ListTransactionsResponse> listAllTransactions(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        ListTransactionsResponse response = transactionService.ListTransactions(userDetails);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Approve a transaction", description = "Approves a specific transaction.")
    @ApiResponse(responseCode = "200", description = "Transaction approved successfully.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    @ApiResponse(responseCode = "403", description = "This customer is not authorized to approve/reject this transaction.")
    @ApiResponse(responseCode = "404", description = "Transaction not found.")
    @PostMapping("/approve")
    public ResponseEntity<ApproveTransactionResponse> approveTransaction(@Valid @RequestBody ApproveTransactionRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        ApproveTransactionResponse response = transactionService.ApproveTransaction(request, userDetails);
        return ResponseEntity.ok(response);
    }
}
