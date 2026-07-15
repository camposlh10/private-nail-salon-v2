package com.nailsalon.backend.configuration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebConfig {

	/**
	 * ETags for the public JSON catalog so browsers/CDNs can revalidate cheaply
	 * (304 Not Modified). Media responses set their own strong ETag + immutable
	 * cache headers, so they're excluded here.
	 */
	@Bean
	FilterRegistrationBean<ShallowEtagHeaderFilter> publicCatalogEtagFilter() {
		FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
				new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
		registration.addUrlPatterns(
				"/api/v1/public/business",
				"/api/v1/public/categories",
				"/api/v1/public/services",
				"/api/v1/public/services/*");
		return registration;
	}
}
