package com.life_admin.backend.DTOs.response;

import com.life_admin.backend.models.BusinessAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessResponse {
	private String id;
	private String name;
	private String email;
	private String category;
	private List<BusinessAccount.Service> services;
	
	public BusinessResponse (BusinessAccount b) {
		this.email = b.getEmail();
		this.id = b.getId();
		this.name = b.getName();
		this.category = b.getCategory();
		this.services = b.getServices();
	}
}
