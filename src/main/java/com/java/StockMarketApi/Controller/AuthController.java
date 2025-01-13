package com.java.StockMarketApi.Controller;

import com.java.StockMarketApi.Models.UserRole;
import com.java.StockMarketApi.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    public Map<String, Object> register(Map<String, String> request) {
        try {
            UserRole role = UserRole.valueOf(request.get("role").toUpperCase());
            UserService.AuthResponse response = userService.register(
                    request.get("username"),
                    request.get("password"),
                    request.get("email"),
                    role
            );

            if (!response.success()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", Map.of(
                        "message", response.errorMessage(),
                        "code", 400
                ));
                return errorResponse;
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", response.user().getId());
            userData.put("username", response.user().getUsername());
            userData.put("email", response.user().getEmail());
            userData.put("role", response.user().getRole().ordinal());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Authentication successful");
            result.put("data", Map.of(
                    "user", userData,
                    "token", response.token()
            ));

            return result;
        } catch (IllegalArgumentException e) {
            return Map.of(
                    "success", false,
                    "error", Map.of(
                            "message", "Invalid role specified",
                            "code", 400
                    )
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", Map.of(
                            "message", "Registration failed: " + e.getMessage(),
                            "code", 500
                    )
            );
        }
    }

    public Map<String, Object> login(Map<String, String> request) {
        try {
            UserService.AuthResponse response = userService.login(
                    request.get("username"),
                    request.get("password")
            );

            if (!response.success()) {
                return Map.of(
                        "success", false,
                        "error", Map.of(
                                "message", "Invalid credentials",
                                "code", 401
                        )
                );
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", response.user().getId());
            userData.put("username", response.user().getUsername());
            userData.put("email", response.user().getEmail());
            userData.put("role", response.user().getRole().ordinal());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Authentication successful");
            result.put("data", Map.of(
                    "user", userData,
                    "token", response.token()
            ));

            return result;
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", Map.of(
                            "message", "Login failed: " + e.getMessage(),
                            "code", 500
                    )
            );
        }
    }

    public Map<String, Object> getUserProfile(Integer id) {
        try {
            return userService.getUserProfile(id.longValue())
                    .map(user -> {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("id", user.getId());
                        userData.put("username", user.getUsername());
                        userData.put("email", user.getEmail());
                        userData.put("role", user.getRole().ordinal());

                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", userData);

                        return result;
                    })
                    .orElse(Map.of(
                            "success", false,
                            "error", Map.of(
                                    "message", "User not found",
                                    "code", 404
                            )
                    ));
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", Map.of(
                            "message", e.getMessage(),
                            "code", 500
                    )
            );
        }
    }

    public Map<String, Object> deleteUser(Integer id) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (!deleted) {
                return Map.of(
                        "success", false,
                        "error", Map.of(
                                "message", "User not found",
                                "code", 404
                        )
                );
            }

            return Map.of(
                    "success", true,
                    "message", "User deleted successfully"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", Map.of(
                            "message", e.getMessage(),
                            "code", 500
                    )
            );
        }
    }
}