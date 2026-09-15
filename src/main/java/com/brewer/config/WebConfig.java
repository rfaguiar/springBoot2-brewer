package com.brewer.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.cache.Caching;
import java.time.Duration;



@Configuration
@EnableCaching
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

	/**
	 * Configuração programática do cache (equivalente ao antigo env/ehcache.xml),
	 * evitando a necessidade de JAXB (javax.xml.bind) em tempo de execução.
	 */
	@Bean
	public CacheManager cacheManager() {
		javax.cache.CacheManager jCacheManager = Caching.getCachingProvider().getCacheManager();

		org.ehcache.config.CacheConfiguration<Object, Object> cidadesConfig = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(3))
				.withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(10)))
				.build();

		jCacheManager.createCache("cidades", Eh107Configuration.fromEhcacheCacheConfiguration(cidadesConfig));

		return new JCacheCacheManager(jCacheManager);
	}


}
