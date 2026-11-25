package com.life_admin.backend.services.redis_services;

import com.life_admin.backend.DTOs.request.CreateBookingRequest;
import com.life_admin.backend.exceptions.PendingBookingNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class PendingBookingRedisService {
	
	private static final Duration TTL = Duration.ofMinutes(5);
	
	@Autowired
	private RedisTemplate<String, Object> redis;
	
	public String save(CreateBookingRequest request, String userId) {
		String pendingId = UUID.randomUUID().toString();
		redis.opsForValue().set(key(userId, pendingId), request, TTL);
		return pendingId;
	}
	
	public CreateBookingRequest claim(String userId, String pendingId) {
		Object stored = redis.opsForValue().getAndDelete(key(userId, pendingId));
		if (stored == null)
			throw new PendingBookingNotFoundException();
		return (CreateBookingRequest) stored;
	}
	
	private String key(String userId, String pendingId) {
		return "pending:" + userId + ":" + pendingId;
	}
}