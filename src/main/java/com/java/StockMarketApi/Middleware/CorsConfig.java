//package com.java.StockMarketApi.Middleware;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//
//        // Allow all origins
//        config.addAllowedOrigin("*");
//
//        // Allow all HTTP methods
//        config.addAllowedMethod("GET");
//        config.addAllowedMethod("POST");
//        config.addAllowedMethod("PUT");
//        config.addAllowedMethod("DELETE");
//        config.addAllowedMethod("OPTIONS");
//
//        // Allow specific headers
//        config.addAllowedHeader("Content-Type");
//        config.addAllowedHeader("Authorization");
//
//        // Allow credentials
//        config.setAllowCredentials(true);
//
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }
//}
