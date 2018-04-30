package com.brewer.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.cache.Caching;
import java.net.URISyntaxException;



@Configuration
@EnableCaching
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

	@Bean
	public CacheManager cacheManager() throws URISyntaxException {
		return new JCacheCacheManager(Caching.getCachingProvider().getCacheManager(
				getClass().getResource("/env/ehcache.xml").toURI(),
				getClass().getClassLoader()));
	}


}
