package com.life_admin.backend.DTOs.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileSettingRequest {
	@NotBlank
	private String name;
	@NotBlank
	private String category;
	@NotBlank
	private String location;
	@NotBlank
	@Email
	private String email;
}
