package com.java.StockMarketApi;

import com.java.StockMarketApi.Controller.AuthController;
import com.java.StockMarketApi.Controller.ProductController;
import com.java.StockMarketApi.Models.Item;
import com.java.StockMarketApi.Utils.JWTManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class StockManagementApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(StockManagementApiApplication.class);
    private static final String SECRET_KEY = "your-secret-key-here-make-it-at-least-32-characters";


    @Autowired
    public JWTManager jwtManager;

    @Autowired
    private AuthController authController;

    @Autowired
    private ProductController productController;

    public static void main(String[] args) {
        SpringApplication.run(StockManagementApiApplication.class, args);
    }

    @Bean
    @Order(1)
    public OncePerRequestFilter roleCheckFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) {
                try {
                    String path = request.getRequestURI();
                    String method = request.getMethod();

                    logger.info("Processing request: {} {} ");

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    // Add CORS headers for all responses
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                    response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

                    // Handle preflight OPTIONS requests
                    if ("OPTIONS".equalsIgnoreCase(method)) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return;
                    }

                    // Skip authentication for auth endpoints
                    if (path.startsWith("/auth/")) {
                        logger.debug("Skipping authentication for auth endpoint: {}");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (path.startsWith("/notifications")) {
                        logger.debug("Skipping authentication for auth endpoint: {}");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String token = request.getHeader("Authorization");
                    System.out.println(request.getHeader("Authorization"));
                    logger.debug("Token received:"+token);

                    // Token validation
                    if (token == null || token.isEmpty()) {
                        logger.warn("No authorization token provided for path: {}");
                        sendDetailedErrorResponse(response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Authentication required",
                                "No token provided",
                                "LOGIN_REQUIRED");
                        return;
                    }
                    try {
                        String role = jwtManager.getRoleFromToken(token);
                        System.out.println("role"+role);
                        logger.info("User role: {} accessing: {} {}");
                        // Role-based authorization checks

//                        if (path.contains("/sell")) {
//                            if (!role.equals("STOCK_OFFICER") && !role.equals("ADMIN")) {
//                                sendDetailedErrorResponse(response,
//                                        HttpServletResponse.SC_FORBIDDEN,
//                                        "Insufficient permissions",
//                                        "This operation requires STOCK_OFFICER or ADMIN role",
//                                        "INSUFFICIENT_ROLE");
//                                return;
//                            }
//                        }
                        if (path.startsWith("/products")) {
                            if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
                                // Check if user has EITHER Admin OR Stock Officer role
                                if (!role.equals("ADMIN") && !role.equals("STOCK_OFFICER")) {
                                    sendDetailedErrorResponse(response,
                                            HttpServletResponse.SC_FORBIDDEN,
                                            "Insufficient permissions",
                                            "This operation requires ADMIN or STOCK_OFFICER role",
                                            "INSUFFICIENT_ROLE");
                                    return;
                                }
                            }
                        }

                        filterChain.doFilter(request, response);

                    } catch (ExpiredJwtException e) {
                        logger.error("Token expired: ", e);
                        sendDetailedErrorResponse(response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Token expired",
                                "Your session has expired. Please log in again",
                                "TOKEN_EXPIRED");
                    }

                  catch (Exception e) {
                        logger.error("Error processing request: ", e);
                        sendDetailedErrorResponse(response,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Server error",
                                "An unexpected error occurred: " + e.getMessage(),
                                "INTERNAL_ERROR");
                    }
                } catch (Exception e) {
                    logger.error("Critical error in filter: ", e);
                    try {
                        sendDetailedErrorResponse(response,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Critical server error",
                                "A critical error occurred. Please try again later",
                                "CRITICAL_ERROR");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            private void sendDetailedErrorResponse(HttpServletResponse response,
                                                   int status,
                                                   String message,
                                                   String details,
                                                   String errorCode) throws Exception {
                response.setStatus(status);

                JSONObject json = new JSONObject();
                json.put("status", status);
                json.put("error", message);
                json.put("details", details);
                json.put("errorCode", errorCode);
                json.put("timestamp", System.currentTimeMillis());

                // Add suggested actions based on error type
                if (errorCode.equals("LOGIN_REQUIRED")) {
                    json.put("suggestedAction", "Please login at /auth/login to obtain a valid token");
                } else if (errorCode.equals("TOKEN_EXPIRED")) {
                    json.put("suggestedAction", "Please login again to obtain a new token");
                } else if (errorCode.equals("INSUFFICIENT_ROLE") ||
                        errorCode.equals("INSUFFICIENT_ROLE_FOR_NOTIFICATIONS") ||
                        errorCode.equals("ADMIN_ROLE_REQUIRED")) {
                    json.put("suggestedAction", "Contact your administrator for role elevation if needed");
                }

                String jsonString = json.toString();
                response.getWriter().write(jsonString);
                response.getWriter().flush();
            }
        };
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        logger.info("Initializing router functions");
        return route()
                // Auth routes
                .POST("/auth/register", request -> {
                    logger.debug("Processing registration request");
                    return ServerResponse.ok().body(authController.register(request.body(Map.class)));
                })
                .POST("/auth/login", request -> {
                    logger.debug("Processing login request");
                    return ServerResponse.ok().body(authController.login(request.body(Map.class)));
                })

                // User routes
                .GET("/users/{id}", request -> {
                    String id = request.pathVariable("id");
                    logger.debug("Getting user profile for id: {}", id);
                    return ServerResponse.ok().body(authController.getUserProfile(Integer.parseInt(id)));
                })

                // Product routes
                .GET("/products", request -> {
                    logger.debug("Getting all products");
                    return ServerResponse.ok().body(productController.getProducts());
                })
                .GET("/products/{id}", request -> {
                    String id = request.pathVariable("id");
                    logger.debug("Getting product with id: {}", id);
                    return ServerResponse.ok().body(productController.getProduct(Integer.parseInt(id)));
                })
                .POST("/products", request -> {
                    logger.debug("Creating new product");
                    return ServerResponse.ok().body(productController.createProduct(request.body(Item.class)));
                })
                .PUT("/products/{id}", request -> {
                    String id = request.pathVariable("id");
                    logger.debug("Updating product with id: {}", id);
                    return ServerResponse.ok().body(productController.updateProduct(
                            Integer.parseInt(id),
                            request.body(Item.class)));
                })
                .DELETE("/products/{id}", request -> {
                    String id = request.pathVariable("id");
                    logger.debug("Deleting product with id: {}", id);
                    return ServerResponse.ok().body(productController.deleteProduct(Integer.parseInt(id)));
                })
                .POST("/products/{id}/sell", request -> {
                    String id = request.pathVariable("id");
                    logger.debug("Processing sale for product id: {}", id);
                    return ServerResponse.ok().body(productController.sellProduct(
                            Integer.parseInt(id),
                            request.body(Map.class)));
                })

                // Notifications
                .GET("/notifications", request -> {
                    logger.debug("Getting notifications");
                    return ServerResponse.ok().body(productController.getNotifications());
                })
                .build();
    }
}