package com.life_admin.backend.security;

import com.life_admin.backend.models.BusinessAccount;
import com.life_admin.backend.models.UserAccount;
import com.life_admin.backend.repositories.BusinessAccountRepo;
import com.life_admin.backend.repositories.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class MyUserDetailsService implements UserDetailsService {
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private BusinessAccountRepo businessAccountRepo;
	
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
	
	@Override
	@NullMarked
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		UserAccount userAccount;
		BusinessAccount businessAccount;
		
		String trimmedIdentifier = identifier.trim();
		
		if (trimmedIdentifier.isEmpty())
			throw new UsernameNotFoundException("Identifier cannot be empty");
		
		if (isEmail(trimmedIdentifier)) {
			userAccount = userRepo.findByEmail(trimmedIdentifier).orElse(null);
			if (userAccount != null)
				return User.withUsername(userAccount.getUsername()).password(userAccount.getPassword()).build();
			
			businessAccount = businessAccountRepo.findByEmail(identifier).orElse(null);
			
		} else {
			userAccount = userRepo.findByUsername(trimmedIdentifier).orElse(null);
			if (userAccount != null)
				return User.withUsername(userAccount.getUsername()).password(userAccount.getPassword()).build();
			
			businessAccount = businessAccountRepo.findByName(identifier).orElse(null);
			
		}
		
		if (businessAccount != null)
			return User.withUsername(businessAccount.getName()).password(businessAccount.getPassword()).build();
		
		throw new UsernameNotFoundException("Bad Credentials");
		/* after this spring only checks passwords authorities and other methods default userdetails has */
	}
	
	private Boolean isEmail(String identifier) {
		return EMAIL_PATTERN.matcher(identifier).matches();
	}
}
