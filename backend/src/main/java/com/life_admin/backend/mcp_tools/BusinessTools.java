package com.life_admin.backend.mcp_tools;

import com.life_admin.backend.DTOs.response.BusinessResponse;
import com.life_admin.backend.security.MyUserDetails;
import com.life_admin.backend.services.BusinessService;
import com.life_admin.backend.utils.Interval;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BusinessTools {
	
	@Autowired
	BusinessService service;
	
	@McpTool(name = "search_businesses", description = "Look up specific businesses with criteria")
	private List<BusinessResponse> searchBusiness(
			@McpToolParam(description = "Part of the business name, e.g. 'Lagos Medical'. Partial match, case-insensitive. Leave empty to ignore.",
					required = false) String name,
			
			@McpToolParam(description = "Business type. One of: clinic, gym. Leave empty to search all types.",
					required = false) String category,
			
			@McpToolParam(description = "Area name, e.g. 'Lekki' or 'Mainland'. Partial match, case-insensitive. Leave empty to ignore.",
					required = false) String location,
			
			@McpToolParam(description = "Service names the business must offer, e.g. ['vaccination', 'lab test']. Each name is matched partially. Leave empty to ignore.",
					required = false) List<String> services,
			
			@McpToolParam(description = "How to combine multiple services: 'AND' means the business must offer all of them, 'OR' means at least one. Defaults to AND. Only used when services is set.",
					required = false) String operator) {
		return service.search(name, category, location, services, operator);
	}
	
	@McpTool(name = "get_free_times",
			description = "Find the free times on one day for a service at a business. Returns windows in Nigerian " +
					"time, each long enough to book this service. Use business and service ids from " +
					"search_businesses. Call this before suggesting a time, so you only offer times that are free.")
	public List<Interval> getFreeTimes(
			@McpToolParam(description = "Business id from search_businesses. Do not guess or make one up.")
			String businessId,
			
			@McpToolParam(description = "Service id from search_businesses. Sets the shortest window that can be offered. " +
					"Do not guess or make one up.")
			String serviceId,
			
			@McpToolParam(description = "Day to check in Nigerian time, ISO format, e.g. 2026-11-20. " +
					"For 'today', use today's date.")
			LocalDate date) {
		
		MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return service.getAvailableSlots(businessId, serviceId, date, userDetails);
	}
}
