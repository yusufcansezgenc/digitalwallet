package com.inghubs.digitalwallet.dtos.responses;

import com.inghubs.digitalwallet.entities.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DepositWalletResponse {
    
    private Transaction transaction;
    private Wallet wallet;
    private Boolean isPendingTransaction;
}
