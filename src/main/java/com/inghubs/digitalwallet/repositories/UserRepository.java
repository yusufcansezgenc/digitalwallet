package com.inghubs.digitalwallet.repositories;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.inghubs.digitalwallet.entities.User;

public interface UserRepository extends CrudRepository<User, UUID> {

    User findByUsername(String username);
}
