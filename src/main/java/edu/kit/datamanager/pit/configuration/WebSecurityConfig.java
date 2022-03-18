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

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private Logger logger;

  @Autowired
  private KeycloakJwtProperties properties;

  @Autowired
  private ApplicationProperties config;

  @Value("${metastore.security.enable-csrf:true}")
  private boolean enableCsrf;
  @Value("${metastore.security.allowedOriginPattern:http[*]://localhost:[*]}")
  private String allowedOriginPattern;

  public WebSecurityConfig() {
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        // everyone, even unauthenticated users may do HTTP OPTIONS on urls.
        .authorizeRequests()
        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .antMatchers("/api/v1").authenticated()
        .and()
        // do not store sessions (use stateless "sessions")
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        // insert the AuthenticationManager which was configured in the method above as
        // a filter, right after HTTP Basic auth.
        .addFilterAfter(keycloaktokenFilterBean(), BasicAuthenticationFilter.class)
        // TODO why?
        .headers().cacheControl().disable();

    if (!enableCsrf) {
      // TODO disables csrf. https://developer.mozilla.org/en-US/docs/Glossary/CSRF
      http.csrf().disable();
    }

    if (!config.isAuthEnabled()) {
      logger.info("Authentication is DISABLED. Adding 'NoAuthenticationFilter' to authentication chain.");
      http.addFilterAfter(
          new NoAuthenticationFilter(config.getJwtSecret(), authenticationManager()),
          KeycloakTokenFilter.class);
    } else {
      logger.info("Authentication is ENABLED.");
    }
  }

  @Bean
  public KeycloakTokenFilter keycloaktokenFilterBean() throws Exception {
    return new KeycloakTokenFilter(KeycloakTokenValidator.builder()
        .readTimeout(properties.getReadTimeoutms())
        .connectTimeout(properties.getConnectTimeoutms())
        .sizeLimit(properties.getSizeLimit())
        .jwtLocalSecret(config.getJwtSecret())
        .build(properties.getJwkUrl(), properties.getResource(), properties.getJwtClaim()));
  }

  @Bean
  // Reads properties.application. Result will be autowired to this class.
  public KeycloakJwtProperties properties() {
    return new KeycloakJwtProperties();
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    // might be necessary for certain identifier types.
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
  }

  @Bean
  public FilterRegistrationBean<Filter> corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(false);
    config.addAllowedOriginPattern(allowedOriginPattern);
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.addExposedHeader("Content-Range");
    config.addExposedHeader("ETag");

    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(0);
    return bean;
  }
}
