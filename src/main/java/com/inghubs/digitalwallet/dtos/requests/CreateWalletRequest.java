package com.inghubs.digitalwallet.dtos.requests;


import java.util.UUID;

import com.inghubs.digitalwallet.utilities.enums.Currency;
import com.inghubs.digitalwallet.utilities.validators.EnumValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    @NotBlank(message = "Wallet name cannot be blank.")
    private String walletName;

    @EnumValue(enumClass = Currency.class, message = "Invalid currency.")
    private Currency currency;

    @NotNull(message = "Shopping status cannot be null.")
    private Boolean isActiveShopping;
    
    @NotNull(message = "Withdraw status cannot be null.")
    private Boolean isActiveWithdraw;
    
    private UUID customerId;
}
