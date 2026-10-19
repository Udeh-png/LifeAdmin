package com.life_admin.backend.repositories;

import com.life_admin.backend.models.BusinessAccount;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@NullMarked
@Repository
public interface BusinessAccountRepo extends MongoRepository<BusinessAccount, String> {
	
	List<BusinessAccount> findByCategoryAndLocationContainingIgnoreCase (String category, String location);
	
	Optional<BusinessAccount> findByName(String name);
	
	Optional<BusinessAccount> findByEmail(String email);
}
