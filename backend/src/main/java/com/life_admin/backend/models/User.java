package com.life_admin.backend.models;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class User {
	@Id
	private String id;
	
	@Indexed(unique = true)
	@Email
	private String email;
	
	@Indexed(unique = true)
	private String username;
	
	private String password;
}
