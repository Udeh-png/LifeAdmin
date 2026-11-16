package com.life_admin.backend.mcp_tools;

import com.life_admin.backend.DTOs.response.BusinessResponse;
import com.life_admin.backend.services.BusinessService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessTools {
	
	@Autowired
	BusinessService service;
	
	@McpTool(name="search_businesses", description = "Look up specific businesses with criteria")
	private List<BusinessResponse> searchBusiness (
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
}
