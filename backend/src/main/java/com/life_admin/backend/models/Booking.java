package com.life_admin.backend.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document("businesses")
public class Booking {
	@Id
	private String id;
	
	@NonNull
	private String userId;
	
	@NonNull
	private String businessId;
	
	@NonNull
	private Instant slot;
	
	@NonNull
	private String status;
}
// TODO: Create compound index on userUd and status