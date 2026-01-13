package com.inghubs.digitalwallet.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inghubs.digitalwallet.entities.Transaction;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, UUID> {
    
    List<Transaction> findByWalletId(UUID walletId);
}
