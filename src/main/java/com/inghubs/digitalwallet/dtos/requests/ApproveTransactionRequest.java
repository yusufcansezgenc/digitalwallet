package com.inghubs.digitalwallet.dtos.requests;

import java.util.UUID;

import com.inghubs.digitalwallet.utilities.enums.TransactionStatus;
import com.inghubs.digitalwallet.utilities.validators.EnumValue;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ApproveTransactionRequest {

    @NotNull(message = "Transaction ID cannot be null.")
    private UUID transactionId;

    @EnumValue(enumClass = TransactionStatus.class, message = "Invalid transaction status.")
    private TransactionStatus status;
}
