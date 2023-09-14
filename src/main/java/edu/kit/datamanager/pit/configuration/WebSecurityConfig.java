/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.pit.configuration;

import edu.kit.datamanager.security.filter.KeycloakJwtProperties;
import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import edu.kit.datamanager.security.filter.KeycloakTokenValidator;
import edu.kit.datamanager.security.filter.NoAuthenticationFilter;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 *
 * @author jejkal
 */
@Configuration
@AutoConfigureAfter(value = KeycloakJwtProperties.class)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

  @Autowired
  private KeycloakJwtProperties properties;

  @Autowired
  private ApplicationProperties config;

  @Value("${pit.security.enable-csrf:true}")
  private boolean enableCsrf;
  @Value("${pit.security.allowedOriginPattern:http*://localhost:[*]}")
  private String allowedOriginPattern;

  @Bean
  protected SecurityFilterChain filterChain(HttpSecurity http, Logger logger) throws Exception {
    http
        .cors()
        .and()
        // everyone, even unauthenticated users may do HTTP OPTIONS on urls.
        .authorizeHttpRequests()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/v1/**").authenticated()
        .and()
        // do not store sessions (use stateless "sessions")
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .addFilterAfter(keycloaktokenFilterBean(), BasicAuthenticationFilter.class)
        // TODO why?
        .headers().cacheControl().disable();

    if (!enableCsrf) {
        // TODO disables csrf. https://developer.mozilla.org/en-US/docs/Glossary/CSRF
        http.csrf(csrf -> csrf.disable());
    }

    if (!config.isAuthEnabled()) {
      logger.info("Authentication is DISABLED. Adding 'NoAuthenticationFilter' to authentication chain.");
      AuthenticationManager defaultAuthenticationManager = http.getSharedObject(AuthenticationManager.class);
      http.addFilterAfter(
          new NoAuthenticationFilter(config.getJwtSecret(), defaultAuthenticationManager),
          KeycloakTokenFilter.class);
    } else {
      logger.info("Authentication is ENABLED.");
    }
    return http.build();
  }

  public KeycloakTokenFilter keycloaktokenFilterBean() {
    return new KeycloakTokenFilter(KeycloakTokenValidator.builder()
        .readTimeout(properties.getReadTimeoutms())
        .connectTimeout(properties.getConnectTimeoutms())
        .sizeLimit(properties.getSizeLimit())
        .jwtLocalSecret(config.getJwtSecret())
        .build(properties.getJwkUrl(), properties.getResource(), properties.getJwtClaim()));
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    // might be necessary for certain identifier types.
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  @Bean
  public WebSecurityCustomizer webSecurity() {
    return web -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
  }

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowCredentials(false);
    corsConfig.addAllowedOriginPattern(allowedOriginPattern);
    corsConfig.addAllowedHeader("*");
    corsConfig.addAllowedMethod("*");
    corsConfig.addExposedHeader("Content-Range");
    corsConfig.addExposedHeader("ETag");

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return new CorsFilter(source);
  }
}
