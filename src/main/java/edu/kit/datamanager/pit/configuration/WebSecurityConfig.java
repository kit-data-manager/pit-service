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

import edu.kit.datamanager.security.filter.JwtAuthenticationFilter;
import edu.kit.datamanager.security.filter.JwtAuthenticationProvider;
import edu.kit.datamanager.security.filter.NoAuthenticationFilter;
import edu.kit.datamanager.security.filter.NoopAuthenticationEventPublisher;

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

  @Autowired
  private Logger logger;

  @Autowired
  private ApplicationProperties config;

  public WebSecurityConfig(){
  }

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception{
    auth
      // we do not act on success or failure in any special way.
      .authenticationEventPublisher(new NoopAuthenticationEventPublisher())
      // we use JWT to authenticate users.
      .authenticationProvider(
        new JwtAuthenticationProvider(config.getJwtSecret(), logger)
      );
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception{
    HttpSecurity httpSecurity = http
      // everyone, even unauthenticated users may do HTTP OPTIONS on urls.
      .authorizeRequests()
      .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
      .antMatchers("/api/v1").authenticated()
      .and()
      // do not store sessions (use stateless "sessions")
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and()
      // TODO disables csrf. Should be evaluated before releasing this service. https://developer.mozilla.org/en-US/docs/Glossary/CSRF
      .csrf().disable()
      // insert the AuthenticationManager which was configured in the method above as a filter, right after HTTP Basic auth.
      .addFilterAfter(new JwtAuthenticationFilter(authenticationManager()), BasicAuthenticationFilter.class);

    if (!config.isAuthEnabled()) {
      logger.info("Authentication is DISABLED. Adding 'NoAuthenticationFilter' to authentication chain.");
      httpSecurity = httpSecurity.addFilterAfter(new NoAuthenticationFilter(config.getJwtSecret(), authenticationManager()), JwtAuthenticationFilter.class);
    } else {
      logger.info("Authentication is ENABLED.");
    }

    // TODO why?
    http.headers().cacheControl().disable();
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall(){
    DefaultHttpFirewall firewall = new DefaultHttpFirewall();
    // might be necessary for certain identifier types.
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  @Override
  public void configure(WebSecurity web) throws Exception{
    web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
  }

  @Bean
  public FilterRegistrationBean<Filter> corsFilter(){
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(false);
    config.addAllowedOrigin("*"); // @Value: http://localhost:8080
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
