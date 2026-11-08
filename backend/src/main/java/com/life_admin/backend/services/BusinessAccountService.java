package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.AddServicesRequest;
import com.life_admin.backend.exceptions.*;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class BusinessAccountService {
	
	@Autowired
	BusinessAccountRepo businessRepo;
	@Autowired
	MongoTemplate mongoTemplate;
	
	public void addService(AddServicesRequest serviceReq, MyUserDetails userDetails) {
		BusinessAccount business = businessRepo.findById(userDetails.getId()).orElseThrow(BusinessNotFoundException::new);
		
		boolean existsByName = business.getServices().stream().anyMatch(s -> s.getName().equals(serviceReq.getServiceName()));
		if (existsByName)
			throw new ServiceNameAlreadyExists("Can't have services with the same name");
		
		BusinessAccount.Service service = new BusinessAccount.Service();
		service.setId(UUID.randomUUID().toString());
		service.setName(serviceReq.getServiceName());
		
		if (serviceReq.getServiceDuration() != null && (serviceReq.getMinService() != null || serviceReq.getMaxService() != null)) {
			throw new InvalidServiceException("Set only a fixed duration, or both a min and max");
		}
		
		if ((serviceReq.getMinService() == null || serviceReq.getMaxService() == null) && serviceReq.getServiceDuration() == null)
			throw new InvalidServiceException("Set either a fixed duration, or both a min and max");
		
		if (serviceReq.getServiceDuration() != null) {
			if (serviceReq.getServiceDuration() <= 0) {
				throw new InvalidServiceException("Duration must be a positive whole integer greater than zero");
			}
			service.setSessionDuration(Duration.ofMinutes(serviceReq.getServiceDuration()));
		} else if (serviceReq.getMinService() <= serviceReq.getMaxService()) {
			service.setSessionMin(Duration.ofMinutes(serviceReq.getMinService()));
			service.setSessionMax(Duration.ofMinutes(serviceReq.getMaxService()));
		} else throw new InvalidTimeRangeException("The min value can't be more than the max value, dumbass");
		
		service.setWorkTime(serviceReq.getWorkTime());
		business.getServices().add(service);
		
		if (!business.isPublished())
			business.setPublished(true);
		
		businessRepo.save(business);
	}
	
	public void profileSetting(String name, String category, String location, String email, MyUserDetails userDetails) {
		Update update = new Update();
		
		if (name != null)
			update.set("name", name.trim());
		
		if (category != null)
			update.set("category", category.trim());
		
		if (location != null)
			update.set("location", location.trim());
		
		if (email != null) {
			update.set("email", email.trim());
			update.set("emailIsVerified", false);
		}
		
		if (update.getUpdateObject().isEmpty())
			return;
		
		try {
			mongoTemplate.updateFirst(
					Query.query(Criteria.where("_id").is(userDetails.getId())),
					update,
					BusinessAccount.class
			);
		} catch (DuplicateKeyException dke) {
			if (dke.getMessage().contains("email"))
				throw new EmailAlreadyExistsException();
			
			throw dke;
		}
	}
}
