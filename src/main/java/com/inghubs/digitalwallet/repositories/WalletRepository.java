package com.inghubs.digitalwallet.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inghubs.digitalwallet.entities.Wallet;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, UUID> {
    
    List<Wallet> findByCustomerId(UUID customerId);
}
