package com.inghubs.digitalwallet.repositories;

import org.springframework.stereotype.Repository;
import com.inghubs.digitalwallet.entities.Customer;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

@Repository
public interface CustomerRepository extends CrudRepository<Customer, UUID> {
    
}
