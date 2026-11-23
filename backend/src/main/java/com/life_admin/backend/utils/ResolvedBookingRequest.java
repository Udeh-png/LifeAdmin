package com.life_admin.backend.utils;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.BusinessAccount.Service;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolvedBookingRequest {
	BusinessAccount business;
	Service service;
	LocalDateTime requestedEndTime;
}
