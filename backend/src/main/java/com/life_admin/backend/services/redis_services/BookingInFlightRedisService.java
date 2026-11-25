package com.life_admin.backend.services.redis_services;

import com.life_admin.backend.exceptions.BookingInProgressException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class BookingInFlightRedisService {
	
	private static final Duration TTL = Duration.ofSeconds(30);
	
	@Autowired
	private RedisTemplate<String, Object> redis;
	
	public void acquire(String userId) {
		Boolean acquired = redis.opsForValue().setIfAbsent(key(userId), "1", TTL);
		if (!Boolean.TRUE.equals(acquired))
			throw new BookingInProgressException();
	}
	
	public void release(String userId) {
		redis.delete(key(userId));
	}
	
	private String key(String userId) {
		return "booking:in:flight:" + userId;
	}
}