package com.life_admin.backend.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogInResponse  {
	String accessToken;
	String tokenType;
	Long expiresIn;
}
