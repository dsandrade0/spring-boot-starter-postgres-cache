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

    /**
     * Inserts or updates a key-value pair in the database with a specified time-to-live (TTL).
     * If the key already exists, its value and TTL are updated. The TTL defines the expiration time for the key.
     *
     * @param key       The key associated with the value to be stored or updated.
     * @param value     The value to be stored or updated, represented as a JSON string.
     * @param duration  The duration for which the key-value pair remains valid. Determines the TTL.
     */
    public void set(String key, String value, Duration duration) {
        String sql = String.format(""" 
                INSERT INTO %s.%s (key, value, ttl) VALUES (?, ?::jsonb, ?)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, ttl = EXCLUDED.ttl
                """,
                props.getSchema(), props.getTableName());

        LocalDateTime ttl = null;
        if (duration != null) {
            ttl = LocalDateTime.now().plus(duration);
        }

        jdbcTemplate.update(sql, key, value, ttl);
    }

    /**
     * Inserts or updates a key-value pair in the database with a specified time-to-live (TTL). TTL can be null
     * @param key
     * @param value
     * @param duration
     * @param <T>
     */
    public <T> void put(String key, T value, Duration duration) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json, duration);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value for key '" + key + "'", e);
        }
    }

    /**
     * Retrieves a value from the database associated with the specified key and deserializes it into the given type.
     * If the key either does not exist or is expired, an empty {@code Optional} is returned.
     *
     * @param key   The key associated with the value to be retrieved.
     * @param clazz The class type to which the value should be deserialized.
     * @param <T>   The type of the value to be retrieved.
     * @return An {@code Optional} containing the deserialized value if found and valid, or an empty {@code Optional} if the key does not exist or is expired.
     * @throws IllegalStateException If the value cannot be deserialized into the specified type.
     */
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

    /**
     * Deletes a key-value pair from the database.
     * @param key
     */
    public void delete(String key) {
        String sql = String.format("DELETE FROM %s.%s WHERE key = ?", props.getSchema(), props.getTableName());
        jdbcTemplate.update(sql, key);
    }
}
