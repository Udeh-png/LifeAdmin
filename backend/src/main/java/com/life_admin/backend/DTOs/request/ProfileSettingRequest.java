package com.life_admin.backend.DTOs.request;

import com.life_admin.backend.utils.OptionalString;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ProfileSettingRequest {
	@OptionalString
	private String name;
	@OptionalString
	private String category;
	@OptionalString
	private String location;
	
	@Email()
	private String email;
	
	@AssertTrue(message = "At least one field must be provided")
	private boolean isAtLeastOneFieldPresent() {
		return name != null || category != null || location != null || email != null;
	}
}
