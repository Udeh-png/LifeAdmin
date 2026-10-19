package com.life_admin.backend.enums;

import lombok.Getter;

@Getter
public enum TokenRole {
	ACCESS_TOKEN("ACCESS", 1800),
	REFRESH_TOKEN("REFRESH", 24 * 60 * 60 * 7);
	
	private final String name;
	private final long maxAgeSeconds;
	
	TokenRole(String name, long maxAgeSeconds) {
		this.name = name;
		this.maxAgeSeconds = maxAgeSeconds;
	}
}
