package com.life_admin.backend.configurations;

import com.life_admin.backend.security.JwtFilter;
import com.life_admin.backend.security.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Autowired
	private MyUserDetailsService userDetailsService;
	@Autowired
	private JwtFilter jwtFilter;
	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity security) {
		
		return security.csrf(CsrfConfigurer::disable)
				.authorizeHttpRequests(
						a -> a
								.requestMatchers("/api/v1/auth/**", "/token/**", "/businesses/**").permitAll()
								.anyRequest().authenticated()
				)
				.sessionManagement(
						s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(
						e -> e.authenticationEntryPoint((q, r, x) -> r.sendError(401))
								.accessDeniedHandler((q, r, x) -> r.sendError(403))
				).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}
	
	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		
		return authProvider;
	}
	
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
