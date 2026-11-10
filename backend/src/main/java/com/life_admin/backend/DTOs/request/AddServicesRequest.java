package com.life_admin.backend.DTOs.request;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.utils.OptionalString;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddServicesRequest {
	@OptionalString
	private String serviceName;
	
	private Long serviceDuration;
	private Long minService;
	private Long maxService;
	private BusinessAccount.WorkTime workTime;
	
	@AssertTrue(message = "At least one field must be provided")
	private boolean isAtLeastOneFieldPresent() {
		return serviceName != null || serviceDuration != null || minService != null || maxService != null;
	}
}
