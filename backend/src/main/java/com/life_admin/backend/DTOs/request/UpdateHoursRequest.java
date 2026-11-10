package com.life_admin.backend.DTOs.request;

import com.life_admin.backend.models.BusinessAccount;
import lombok.Data;

import java.time.ZoneId;

@Data
public class UpdateHoursRequest {
	private BusinessAccount.WorkTime workTime;
	private ZoneId zoneId;
}
