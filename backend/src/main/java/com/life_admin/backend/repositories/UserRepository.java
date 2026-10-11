package com.life_admin.backend.repositories;

import com.life_admin.backend.models.UserAccount;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@NullMarked
@Repository
public interface UserRepository extends MongoRepository<UserAccount, String> {
	Optional<UserAccount> findByUsername(String username);
	
	Optional<UserAccount> findByEmail(String email);
}
