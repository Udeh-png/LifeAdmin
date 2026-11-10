package com.life_admin.backend.services;

import com.life_admin.backend.DTOs.request.AddServicesRequest;
import com.life_admin.backend.DTOs.request.ProfileSettingRequest;
import com.life_admin.backend.DTOs.request.UpdateServiceRequest;
import com.life_admin.backend.exceptions.*;
import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.security.MyUserDetails;
import com.mongodb.client.result.UpdateResult;
import com.networknt.schema.format.TimeFormat;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
			throw new ServiceNameAlreadyExistsException("Can't have services with the same name");
		
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
		
		Update update = new Update();
		update.set("isPublished", true);
		update.addToSet("services", service);
		
		mongoTemplate.updateFirst(
				Query.query(Criteria.where("_id").is(business.getId())),
				update,
				BusinessAccount.class
		);
	}
	
	public void profileSetting(ProfileSettingRequest req, MyUserDetails userDetails) {
		Update update = new Update();
		
		if (req.getName() != null)
			update.set("name", req.getName().trim());
		
		if (req.getCategory() != null)
			update.set("category", req.getCategory().trim());
		
		if (req.getLocation() != null)
			update.set("location", req.getLocation().trim());
		
		if (req.getEmail() != null) {
			update.set("email", req.getEmail().trim());
			update.set("emailIsVerified", false);
		}
		
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
	
	public void updateService(UpdateServiceRequest req, MyUserDetails userDetails) {
		Criteria criteria = new Criteria().andOperator(
				Criteria.where("_id").is(userDetails.getId()),
				Criteria.where("services").elemMatch(
						Criteria.where("id").is(req.getServiceId())
				)
		);
		
		Update update = getUpdate(req);
		
		mongoTemplate.updateFirst(
				Query.query(criteria),
				update,
				BusinessAccount.class
		);
	}
	
	private static Update getUpdate(UpdateServiceRequest req) {
		Update update = new Update();
		
		if (req.getName() != null)
			update.set("service.$.name", req.getName().trim());
		
		if (req.getSessionDuration() != null && req.getSessionDuration() > 0)
			update.set("service.$.sessionDuration", req.getSessionDuration());
		
		if (req.getSessionMax() != null && req.getSessionMax() > 0)
			update.set("service.$.sessionMax", req.getSessionDuration());
		
		if (req.getSessionMin() != null && req.getSessionMin() > 0)
			update.set("service.$.sessionMin", req.getSessionDuration());
		
		return update;
	}
	
	public void updateHours(BusinessAccount.WorkTime workTime, ZoneId zoneId, MyUserDetails userDetails) {
		BusinessAccount businessAccount = businessRepo.findById(userDetails.getId()).orElseThrow(BusinessNotFoundException::new);
		
		if (workTime.getDaysHoursMap().isEmpty())
			throw new InvalidServiceException("Service hours must have at least one day, or be left empty");
		for (Map.Entry<DayOfWeek, BusinessAccount.DayHours> e : workTime.getDaysHoursMap().entrySet()) {
			BusinessAccount.DayHours d = e.getValue();
			if (!d.openTime().isBefore(d.closeTime()))
				throw new InvalidServiceException(e.getKey() + ": open must be before close");
			
			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
			LocalTime openTime = LocalTime.parse(d.getOpen());
			LocalTime closeTime = LocalTime.parse(d.getClose());
			
			openTime.format(timeFormatter);
			closeTime.format(timeFormatter);
		}
		
	}
}

/*
	businessRepo.findById(req.getBusinessId())
				.orElseThrow(BusinessNotFoundException::new)
				.getServices()
				.stream()
				.filter(s -> s.getId().equals(req.getServiceId()))
				.findFirst()
				.orElseThrow(ServiceNotFoundException::new);
*/