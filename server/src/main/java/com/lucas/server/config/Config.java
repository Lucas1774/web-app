package com.lucas.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class Config {
    @Value("${spring.IPAddress}")
    List<String> ipAddress;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ipAddress);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @DependsOn("corsConfigurationSource")
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    @DependsOn("corsFilter")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .authorizeHttpRequests()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
                .httpBasic()
                .and()
                .formLogin();
        return http.build();
    }

    @Bean
    @DependsOn("filterChain")
    public SecurityFilterChain rateLimitFilterChain(HttpSecurity http, RateLimitFilter rateLimitingFilter)
            throws Exception {
        http
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
