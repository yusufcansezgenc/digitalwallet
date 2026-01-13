package com.inghubs.digitalwallet.dtos.requests;

import java.util.UUID;

import com.inghubs.digitalwallet.utilities.enums.OppositePartyType;
import com.inghubs.digitalwallet.utilities.validators.EnumValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class WithdrawWalletRequest {

    @NotNull(message = "Transaction ID cannot be null.")
    private UUID walletId;

    @NotNull(message = "Amount cannot be null.")
    private Double amount;

    @EnumValue(enumClass = OppositePartyType.class, message = "Invalid opposite party type.")
    private OppositePartyType oppositePartyType;

    @NotBlank(message = "Destination cannot be blank.")
    private String destination;
}
