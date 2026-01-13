package com.inghubs.digitalwallet.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inghubs.digitalwallet.dtos.requests.*;
import com.inghubs.digitalwallet.dtos.responses.*;
import com.inghubs.digitalwallet.services.WalletService;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet API", description = "Operations related to wallets")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Create a new wallet for a customer", description = "Creates a new wallet for a specific customer.")
    @ApiResponse(responseCode = "200", description = "Wallet created successfully.")
    @ApiResponse(responseCode = "404", description = "Customer not found.")
    @PostMapping("/create")
    public ResponseEntity<CreateWalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        CreateWalletResponse response = walletService.CreateWallet(request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get wallets of a customer by customer ID", description = "Returns a list of wallets for a specific customer.")
    @ApiResponse(responseCode = "200", description = "Wallets retrieved successfully.")
    @ApiResponse(responseCode = "404", description = "Customer not found.")
    @GetMapping("/{customerId}")
    public ResponseEntity<ListWalletResponse> listWallets(@PathVariable UUID customerId) {
        ListWalletResponse response = walletService.ListWallets(customerId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deposit money into a customer's wallet", description = "Deposits money into a customer's wallet.")
    @ApiResponse(responseCode = "200", description = "Money deposited successfully")
    @ApiResponse(responseCode = "202", description = "Money deposit request has been accepted for processing.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    @ApiResponse(responseCode = "403", description = "This customer is not authorized to perform this action.")
    @ApiResponse(responseCode = "404", description = "No wallet found for the customer.")
    @PostMapping("/deposit")
    public ResponseEntity<DepositWalletResponse> depositWallet(@Valid @RequestBody DepositWalletRequest request) {
        DepositWalletResponse response = walletService.DepositWallet(request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        if (response.getIsPendingTransaction()) {
            return ResponseEntity.accepted().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Withdraw money from a customer's wallet", description = "Withdraws money from a customer's wallet.")
    @ApiResponse(responseCode = "200", description = "Money withdrawn successfully")
    @ApiResponse(responseCode = "202", description = "Money withdraw request has been accepted for processing.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    @ApiResponse(responseCode = "403", description = "This customer is not authorized to perform this action.")
    @ApiResponse(responseCode = "404", description = "No wallet found for the customer.")
    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawWalletResponse> withdrawWallet(@Valid @RequestBody WithdrawWalletRequest request) {
        WithdrawWalletResponse response = walletService.WithdrawWallet(request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        if (response.getIsPendingTransaction()) {
            return ResponseEntity.accepted().body(response);
        }
        return ResponseEntity.ok(response);
    }
}