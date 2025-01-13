package com.java.StockMarketApi.Repository;

import com.java.StockMarketApi.Models.User;
import com.java.StockMarketApi.Models.UserRole;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Initialize database table
    public void initializeTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                role VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
        """);
    }

    public User save(User user) {
        // First insert the user
        jdbcTemplate.update(
                "INSERT INTO users (username, password, email, role, created_at) VALUES (?, ?, ?, ?, ?)",
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRole().toString(),
                Timestamp.valueOf(user.getCreatedAt())
        );

        // Then retrieve the generated id using last_insert_rowid()
        Integer generatedId = jdbcTemplate.queryForObject(
                "SELECT last_insert_rowid()",
                Integer.class
        );

        user.setId(generatedId);
        return user;
    }

    public Optional<User> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE id = ?",
                    this::mapRowToUser,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE username = ?",
                    this::mapRowToUser,
                    username
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?",
                Integer.class,
                username,
                email
        );
        return count != null && count > 0;
    }

    public Optional<User> findByUsernameAndEmailAndIdNot(String username, String email, Long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE (username = ? OR email = ?) AND id != ?",
                    this::mapRowToUser,
                    username,
                    email,
                    userId
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void update(User user) {
        jdbcTemplate.update(
                "UPDATE users SET username = ?, password = ?, email = ?, role = ? WHERE id = ?",
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRole().toString(),
                user.getId()
        );
    }

    public void deleteById(Integer id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    private User mapRowToUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }

    public boolean existsById(Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }
}