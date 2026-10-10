package com.life_admin.backend.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity security) {
		
		return security.csrf(CsrfConfigurer::disable)
				.authorizeHttpRequests(
						a -> a.anyRequest().permitAll()
				)
				.sessionManagement(
						s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(
						e -> e.authenticationEntryPoint((q, r, x) -> r.sendError(401))
								.accessDeniedHandler((q, r, x) -> r.sendError(403))
				)
				.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder () {
		return new BCryptPasswordEncoder(12);
	}
}
