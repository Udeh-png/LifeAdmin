package com.life_admin.backend.DTOs.request;

import lombok.Data;

@Data
public class UpdateServiceRequest {
	private String serviceId;
	private String name;
	
	private Long sessionDuration;
	private Long sessionMax;
	private Long sessionMin;
}
