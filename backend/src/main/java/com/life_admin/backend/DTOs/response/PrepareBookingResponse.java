package com.life_admin.backend.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrepareBookingResponse{
		String pendingId;
		String businessName;
		String serviceName;
		String yourStartTime;
		String yourEndTime;
		String yourZone;
		String clinicStartTime;
		String clinicEndTime;
		String clinicZone;
}
