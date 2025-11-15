package info.dsandrade.postgrescache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class PostgresCacheService {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresCacheProperties props;
    private final ObjectMapper objectMapper;

    public PostgresCacheService(JdbcTemplate jdbcTemplate,
                                PostgresCacheProperties props,
                                ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void set(String key, String value, Duration duration) {
        String sql = String.format(""" 
                INSERT INTO %s.%s (key, value, ttl) VALUES (?, ?::jsonb, ?)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, ttl = EXCLUDED.ttl
                """,
                props.getSchema(), props.getTableName());
        LocalDateTime ttl = LocalDateTime.now().plus(duration);

        jdbcTemplate.update(sql, key, value, ttl);
    }

    public <T> void put(String key, T value, Duration duration) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json, duration);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value for key '" + key + "'", e);
        }
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String sql = String.format(
                    "SELECT value FROM %s.%s WHERE key = ? AND (ttl IS NULL OR ttl > now())",
                    props.getSchema(), props.getTableName()
            );

            String json = jdbcTemplate.queryForObject(sql, String.class, key);
            if (json == null) {
                return Optional.empty();
            }

            T value = objectMapper.readValue(json, clazz);
            return Optional.of(value);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize value for key '" + key + "'", e);
        }
    }

    public void delete(String key) {
        String sql = String.format("DELETE FROM %s.%s WHERE key = ?", props.getSchema(), props.getTableName());
        jdbcTemplate.update(sql, key);
    }
}
