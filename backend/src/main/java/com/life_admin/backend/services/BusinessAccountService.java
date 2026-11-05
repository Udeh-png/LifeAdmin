package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.AddServicesRequest;
import com.life_admin.backend.exceptions.BusinessNotFoundException;
import com.life_admin.backend.exceptions.InvalidServiceException;
import com.life_admin.backend.exceptions.InvalidTimeRangeException;
import com.life_admin.backend.exceptions.ServiceNameAlreadyExists;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessAccountService {
	
	@Autowired
	BusinessAccountRepo businessRepo;
	
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
		
		businessRepo.save(business);
	}
}
