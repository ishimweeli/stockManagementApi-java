package com.java.StockMarketApi.Service;

import com.java.StockMarketApi.Models.User;
import com.java.StockMarketApi.Models.UserRole;
import com.java.StockMarketApi.Repository.UserRepository;
import com.java.StockMarketApi.Utils.JWTManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

@Service
@Transactional

public class UserService {

    private final UserRepository userRepository;

    private final JWTManager jwtManager;

    public UserService(UserRepository userRepository, JWTManager jwtManager) {
        this.userRepository = userRepository;
        this.jwtManager = jwtManager;
    }

    public record AuthResponse(
            boolean success,
            String token,
            User user,
            String errorMessage
    ) {}
    public record JwtClaims(
            Integer userId,
            String username,
            UserRole role
    ) {}


    public AuthResponse register(String username, String password, String email, UserRole role) {
        if (!validateInput(username, password, email)) {
            return new AuthResponse(false, null, null, "Invalid input data");
        }

        if (!EnumSet.of(UserRole.ADMIN, UserRole.STOCK_OFFICER).contains(role)) {
            return new AuthResponse(false, null, null, "Invalid role specified");
        }

        if (userRepository.existsByUsernameOrEmail(username, email)) {
            return new AuthResponse(false, null, null, "Username or email already exists");
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(jwtManager.encryptPassword(password));
            user.setEmail(email);
            user.setRole(role);
            user.setCreatedAt(LocalDateTime.now());

            user = userRepository.save(user);

            String token = jwtManager.generateToken(
                        user.getUsername(),
                        user.getRole(),
                        user.getEmail(),
                        user.getId()
                );

            return new AuthResponse(true, token, user, null);
        } catch (Exception e) {
            return new AuthResponse(false, null, null, "Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse login(String username, String password) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty() || !jwtManager.checkPassword(password, userOpt.get().getPassword())) {
                return new AuthResponse(false, null, null, "Invalid credentials");
            }

            User user = userOpt.get();
            String token = jwtManager.generateToken(
                    user.getUsername(),
                    user.getRole(),
                    user.getEmail(),
                    user.getId()
            );
            return new AuthResponse(true, token, user, null);
        } catch (Exception e) {
            return new AuthResponse(false, null, null, "Login failed: " + e.getMessage());
        }
    }

    public Optional<User> getUserProfile(Long userId) {
        return userRepository.findById(userId);
    }

    public boolean updateUserProfile(Long userId, String email, String username) {
        if (userId == null || (email == null && username == null)) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> {
                    if (!userRepository.findByUsernameAndEmailAndIdNot(username, email, userId).isPresent()) {
                        if (email != null) user.setEmail(email);
                        if (username != null) user.setUsername(username);
                        userRepository.save(user);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        if (userId == null || currentPassword == null || newPassword == null || newPassword.length() < 6) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> {
                    if (jwtManager.checkPassword(currentPassword, user.getPassword())) {
                        user.setPassword(jwtManager.encryptPassword(newPassword));
                        userRepository.save(user);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean deleteUser(Integer userId) {
        if (userId == null) {
            return false;
        }

        try {
            if (userRepository.existsById(userId)) {
                userRepository.deleteById(userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }


    private boolean validateInput(String username, String password, String email) {
        return username != null && username.length() >= 3 &&
                password != null && password.length() >= 6 &&
                email != null && email.contains("@") &&
                email.indexOf('.') > email.indexOf('@') + 1;
    }
}