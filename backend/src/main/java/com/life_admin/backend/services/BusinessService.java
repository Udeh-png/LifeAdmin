package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.response.BusinessResponse;

import com.life_admin.backend.exceptions.BusinessNotFoundException;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessService {
	@Autowired
	BusinessAccountRepo businessRepo;
	@Autowired
	MongoTemplate mongoTemplate;
	
	public List<BusinessResponse> search(String name, String category, String location, List<String> services, String operator) {
		List<Criteria> criteriaList = new ArrayList<>();
		if (name != null)
			criteriaList.add(Criteria.where("name").regex(name, "i"));
		
		if (category != null)
			criteriaList.add(Criteria.where("category").is(category));
		
		if (location != null)
			criteriaList.add(Criteria.where("location").regex(location, "i"));
		
		if (services != null && !services.isEmpty())
			if ("AND".equalsIgnoreCase(operator))
				services.forEach((s) -> criteriaList.add(Criteria.where("services.name").regex(s, "i")));
			else if ("OR".equalsIgnoreCase(operator)) {
				List<Criteria> serviceList = new ArrayList<>();
				services.forEach((s) -> serviceList.add(Criteria.where("service.name").regex(s, "i")));
				
				criteriaList.add(new Criteria().orOperator(serviceList));
			}
		
		
		Criteria criteria = new Criteria().andOperator(criteriaList);
		return mongoTemplate.find(
						Query.query(criteria),
						BusinessAccount.class,
						"businesses"
				)
				.stream()
				.map(BusinessResponse::new)
				.toList();
		
	}
	
	public List<BusinessResponse> getAllBusinesses() {
		return businessRepo.findAll()
				.stream().map(BusinessResponse::new).toList();
	}
	
	public BusinessResponse findWithId(String id) {
		BusinessAccount account = businessRepo.findById(id).orElseThrow(BusinessNotFoundException::new);
		return new BusinessResponse(account);
	}
}
