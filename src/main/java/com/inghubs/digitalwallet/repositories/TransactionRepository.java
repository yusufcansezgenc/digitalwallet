package com.inghubs.digitalwallet.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inghubs.digitalwallet.entities.Transaction;
import com.inghubs.digitalwallet.utilities.enums.TransactionStatus;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, UUID> {
    
    List<Transaction> findByWalletId(UUID walletId);
    List<Transaction> findByWalletIdAndStatus(UUID walletId, TransactionStatus status);
    List<Transaction> findAll();
}
