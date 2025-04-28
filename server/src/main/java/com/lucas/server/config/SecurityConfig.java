package com.lucas.server.config;

import com.lucas.server.config.filter.NoLimitFilter;
import com.lucas.server.config.filter.RateLimitFilter;
import com.lucas.server.config.filter.RequestPerSecondLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final List<String> ipAddress;

    public SecurityConfig(@Value("${security.ip-address}") List<String> ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ipAddress);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsFilter(source);
    }

    @Bean
    @Profile("prod")
    public RateLimitFilter realRateLimitFilter() {
        return new RequestPerSecondLimitFilter();
    }

    @Bean
    @Profile("!prod")
    public RateLimitFilter noopRateLimitFilter() {
        return new NoLimitFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsFilter corsFilter,
                                                   RateLimitFilter rateLimitingFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> {
                })
                .formLogin(form -> {
                });

        return http.build();
    }
}
