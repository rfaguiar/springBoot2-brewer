package com.brewer.config;
import com.brewer.security.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = AppUserDetailsService.class)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
private static final String LOGIN = "/login";
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http.authorizeHttpRequests(auth -> auth
.requestMatchers("/layout/**", "/images/**").permitAll()
.requestMatchers("/cidades/novo").hasAnyRole("CADASTRAR_CIDADE")
.requestMatchers("/usuarios/**").hasAnyRole("CADASTRAR_USUARIO")
.anyRequest().authenticated())
.formLogin(form -> form.loginPage(LOGIN).permitAll())
.logout(logout -> logout.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/logout")))
.sessionManagement(session -> session
.invalidSessionUrl(LOGIN)
.maximumSessions(1)
.expiredUrl(LOGIN));
return http.build();
}
@Bean
public DaoAuthenticationProvider authenticationProvider(AppUserDetailsService userDetailsService) {
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
provider.setPasswordEncoder(passwordEncoder());
return provider;
}
@Bean
public PasswordEncoder passwordEncoder() {
return new BCryptPasswordEncoder();
}
}