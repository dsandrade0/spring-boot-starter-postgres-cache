package info.dsandrade.postgrescache.service;

import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class PostgresCacheService {

    private final JdbcTemplate jdbcTemplate;
    private final PostgresCacheProperties props;

    public PostgresCacheService(JdbcTemplate jdbcTemplate, PostgresCacheProperties props) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
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

    public String get(String key) {
        try {
            String sql = String.format("SELECT value FROM %s.%s WHERE key = ? AND (ttl IS NULL OR ttl > now())", props.getSchema(), props.getTableName());
            return jdbcTemplate.queryForObject(sql, String.class, key);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void delete(String key) {
        String sql = String.format("DELETE FROM %s.%s WHERE key = ?", props.getSchema(), props.getTableName());
        jdbcTemplate.update(sql, key);
    }
}
