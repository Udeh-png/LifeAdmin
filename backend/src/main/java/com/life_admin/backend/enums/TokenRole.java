package com.life_admin.backend.enums;

import lombok.Getter;

@Getter
public enum TokenRole {
	ACCESS_TOKEN("ACCESS", 24 * 60 * 60 * 7), // TODO: this is temporary, change it back to 1800 later
	REFRESH_TOKEN("REFRESH", 24 * 60 * 60 * 7);
	
	private final String name;
	private final long maxAgeSeconds;
	
	TokenRole(String name, long maxAgeSeconds) {
		this.name = name;
		this.maxAgeSeconds = maxAgeSeconds;
	}
}
