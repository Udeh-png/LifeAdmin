package com.life_admin.backend.DTOs.request;

import lombok.Data;

@Data
public class LoginRequest {
	String identifier; // could be Email of Username
	String password;
}
