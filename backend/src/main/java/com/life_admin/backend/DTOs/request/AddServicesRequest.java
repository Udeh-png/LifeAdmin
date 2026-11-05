package com.life_admin.backend.DTOs.request;

import com.life_admin.backend.models.BusinessAccount;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddServicesRequest {
	private String serviceName;
	private Long serviceDuration;
	private Long minService;
	private Long maxService;
	private BusinessAccount.WorkTime workTime;
}
