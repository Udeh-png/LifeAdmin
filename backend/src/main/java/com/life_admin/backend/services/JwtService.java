package com.life_admin.backend.services;

import com.life_admin.backend.enums.TokenRole;
import com.life_admin.backend.security.MyUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
	@Value("${jwt.secret.key}")
	private String rawSecretKey;
	
	private SecretKey getSecretKey() {
		return Keys.hmacShaKeyFor(rawSecretKey.getBytes());
	}
	
	public String generateToken(MyUserDetails user, TokenRole tokenRole) {
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("role", tokenRole.getName())
				.signWith(getSecretKey())
				.expiration(Date.from(Instant.now().plusSeconds(tokenRole.getMaxAgeSeconds())))
				.issuedAt(new Date())
				.compact();
	}
	
	public <T> T extractClaim(String token, Function<Claims, T> function) {
		Claims claims = extractAllClaims(token);
		return function.apply(claims);
	}
	
	public Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
	}
	
	public String getTokenType(String token) {
		return extractClaim(token, (claims) -> claims.get("role", String.class));
	}
}
