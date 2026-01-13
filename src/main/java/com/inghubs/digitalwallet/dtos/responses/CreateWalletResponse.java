package com.inghubs.digitalwallet.dtos.responses;

import com.inghubs.digitalwallet.entities.Wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CreateWalletResponse {
    
    private Wallet wallet;
}
