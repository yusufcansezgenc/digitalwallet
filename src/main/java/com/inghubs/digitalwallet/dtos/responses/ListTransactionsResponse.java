package com.inghubs.digitalwallet.dtos.responses;

import java.util.List;

import com.inghubs.digitalwallet.entities.Transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ListTransactionsResponse {
    
    private List<Transaction> transactions;
}
