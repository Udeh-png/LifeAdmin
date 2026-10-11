package com.life_admin.backend.security;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.UserAccount;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NullMarked
public class MyUserDetails implements UserDetails {
	
	private final String username;
	private final String password;
	private final List<GrantedAuthority> authorities;
	
	@Getter
	private final String email;
	@Getter
	private final String id;
	
	public MyUserDetails(UserAccount user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}
	
	public MyUserDetails(BusinessAccount business) {
		this.id = business.getId();
		this.username = business.getName();
		this.email = business.getEmail();
		this.password = business.getPassword();
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_BUSINESS"));
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public @Nullable String getPassword() {
		return password;
	}
	
	@Override
	public String getUsername() {
		return username;
	}
}
