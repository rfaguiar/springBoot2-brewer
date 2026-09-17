package com.brewer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;



@Configuration
@EnableCaching
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

	/**
	 * Configuração programática do cache "cidades" (equivalente ao antigo env/ehcache.xml),
	 * usando Caffeine em vez de EhCache/JSR-107: mesmo comportamento (tamanho máximo 3,
	 * expiração após 10s de ociosidade), mas sem a dependência de JAXB em tempo de execução
	 * e sem a árvore de estatísticas do EhCache, que não é compatível com GraalVM
	 * native-image sem uma cadeia extensa (e praticamente aberta) de hints de reflection
	 * (ver docs/spring-native-migration-plan.md, seção 8).
	 */
	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager("cidades");
		cacheManager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(3)
				.expireAfterAccess(Duration.ofSeconds(10)));
		return cacheManager;
	}


}
