package com.life_admin.backend.DTOs.request;

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
	private Duration duration;
	
	@ArraySchema(maxItems = 2)
	@Size(max = 2, message = "Cant be more than 2 values mate")
	@Nullable
	private List<Instant> workingHours;
}
