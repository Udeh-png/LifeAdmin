package com.life_admin.backend.models;

import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document("users")
public class UserAccount {
	@Id
	private String id;
	
	@Email
	@Indexed(unique = true)
	@NonNull
	private String email;
	
	@NonNull
	@Indexed(unique = true)
	private String username;
	
	@NonNull
	private String password;
	
	private String phoneNumber;
}
