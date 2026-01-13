package com.inghubs.digitalwallet.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.services.TransactionService;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "Transaction API", description = "Operations related to transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "List transactions for a wallet", description = "Lists all transactions for a specific wallet.")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "Wallet not found.")
    @GetMapping("/{walletId}")
    public ResponseEntity<ListTransactionsResponse> listTransactions(@PathVariable UUID walletId) {
        ListTransactionsResponse response = transactionService.ListTransactions(walletId);
        if(response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve a transaction", description = "Approves a specific transaction.")
    @ApiResponse(responseCode = "200", description = "Transaction approved successfully.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    @ApiResponse(responseCode = "403", description = "This user is not authorized to approve/reject this transaction.")
    @ApiResponse(responseCode = "404", description = "Transaction not found.")
    @PostMapping("/approve")
    public ResponseEntity<ApproveTransactionResponse> approveTransaction(@Valid @RequestBody ApproveTransactionRequest request) {
        ApproveTransactionResponse response = transactionService.ApproveTransaction(request);
        if(response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }
}
